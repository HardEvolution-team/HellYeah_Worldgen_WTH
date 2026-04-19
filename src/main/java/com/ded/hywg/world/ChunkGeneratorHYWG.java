package com.ded.hywg.world;

import com.ded.hywg.WorldHeightConfig;
import com.ded.hywg.noise.OpenSimplex2;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.IChunkGenerator;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

public class ChunkGeneratorHYWG implements IChunkGenerator {
    private final World world;
    private final long seed;
    private final Random rand;

    // Уровень моря — базовая линия мира
    private static final int SEA_LEVEL = 2048;

    // Семена для разных слоёв шума (XOR с базовым seed)
    private static final long SEED_CONTINENT    = 0x1A2B3C4DL;
    private static final long SEED_EROSION      = 0xDEADBEEFL;
    private static final long SEED_RIDGES       = 0xCAFEBABEL;
    private static final long SEED_DETAIL1      = 0xFEEDFACEL;
    private static final long SEED_DETAIL2      = 0xBAADF00DL;
    private static final long SEED_DETAIL3      = 0xC0FFEE42L;
    private static final long SEED_WARP_X       = 0x12345678L;
    private static final long SEED_WARP_Z       = 0x87654321L;
    private static final long SEED_MOUNTAIN_MASK = 0xABCDEF01L;
    private static final long SEED_PLATEAU      = 0x0FEDCBA9L;
    private static final long SEED_RIVER        = 0x13579BDFL;
    private static final long SEED_MICRO        = 0x2468ACE0L;

    public ChunkGeneratorHYWG(World world) {
        this.world = world;
        this.seed = world.getSeed();
        this.rand = new Random(seed);
    }

    @Override
    public Chunk generateChunk(int chunkX, int chunkZ) {
        ChunkPrimer primer = new ChunkPrimer();

        for (int localX = 0; localX < 16; ++localX) {
            for (int localZ = 0; localZ < 16; ++localZ) {
                double worldX = (double) ((chunkX << 4) | localX);
                double worldZ = (double) ((chunkZ << 4) | localZ);

                int terrainHeight = computeTerrainHeight(worldX, worldZ);

                // Ограничиваем высоту в пределах мира
                terrainHeight = clamp(terrainHeight, 1, WorldHeightConfig.WORLD_HEIGHT - 2);

                fillColumn(primer, localX, localZ, terrainHeight);
            }
        }

        Chunk chunk = new Chunk(this.world, primer, chunkX, chunkZ);

        // Назначаем биомы
        assignBiomes(chunk, chunkX, chunkZ);

        chunk.generateSkylightMap();
        return chunk;
    }

    /**
     * Главная функция расчёта высоты рельефа.
     *
     * Архитектура шума:
     *
     * 1) Continentalness — определяет сушу vs океан (очень крупный масштаб)
     * 2) Erosion — определяет плоскость vs изрезанность
     * 3) Ridges (горные хребты) — Swiss-cheese ridged noise для горных цепей
     * 4) Domain warping — искажение координат для органичных форм
     * 5) Многооктавный detail noise — мелкие детали рельефа
     * 6) Plateau terracing — террасирование на больших высотах
     * 7) River carving — вырезание речных долин
     */
    private int computeTerrainHeight(double wx, double wz) {

        // ============================================================
        // DOMAIN WARPING — искажаем координаты для более органичных форм
        // ============================================================
        double warpScale = 600.0;
        double warpStrength = 150.0;
        double warpX = OpenSimplex2.noise2(seed ^ SEED_WARP_X, wx / warpScale, wz / warpScale) * warpStrength;
        double warpZ = OpenSimplex2.noise2(seed ^ SEED_WARP_Z, wx / warpScale, wz / warpScale) * warpStrength;

        double sx = wx + warpX; // warped coordinates
        double sz = wz + warpZ;

        // ============================================================
        // СЛОЙ 1: КОНТИНЕНТАЛЬНОСТЬ (суша / океан)
        // Масштаб ~3000-5000 блоков — огромные массивы суши и океанов
        // ============================================================
        double continentRaw = 0.0;
        continentRaw += OpenSimplex2.noise2(seed ^ SEED_CONTINENT, sx / 4000.0, sz / 4000.0) * 1.0;
        continentRaw += OpenSimplex2.noise2(seed ^ SEED_CONTINENT + 1, sx / 2000.0, sz / 2000.0) * 0.5;
        continentRaw += OpenSimplex2.noise2(seed ^ SEED_CONTINENT + 2, sx / 1000.0, sz / 1000.0) * 0.2;
        // Нормализуем примерно в [-1, 1]
        continentRaw /= 1.7;

        // Формируем кривую континентальности:
        // < -0.2 => глубокий океан
        // -0.2 .. 0.0 => шельф / мелководье
        // 0.0 .. 0.3 => равнины (низменности)
        // 0.3 .. 0.6 => холмы / предгорья
        // > 0.6 => горные зоны
        double continentalness = continentRaw;

        // ============================================================
        // СЛОЙ 2: ЭРОЗИЯ — насколько рельеф "сглажен"
        // Высокая эрозия = плоско, низкая = изрезано
        // ============================================================
        double erosionRaw = 0.0;
        erosionRaw += OpenSimplex2.noise2(seed ^ SEED_EROSION, sx / 3000.0, sz / 3000.0) * 1.0;
        erosionRaw += OpenSimplex2.noise2(seed ^ SEED_EROSION + 1, sx / 1500.0, sz / 1500.0) * 0.4;
        erosionRaw /= 1.4;
        // erosion в диапазоне примерно [-1, 1]
        // Переводим в [0, 1] где 0 = максимальная эрозия (плоско), 1 = минимальная (горы)
        double erosion = (erosionRaw + 1.0) * 0.5;
        erosion = clampD(erosion, 0.0, 1.0);

        // ============================================================
        // СЛОЙ 3: ГОРНЫЕ ХРЕБТЫ (Ridged noise)
        // Используем |noise| инвертированный для создания острых хребтов
        // ============================================================
        double ridgeRaw = 0.0;
        // Основной хребет
        ridgeRaw += ridgedNoise(seed ^ SEED_RIDGES, sx / 1200.0, sz / 1200.0) * 1.0;
        // Вторичные хребты
        ridgeRaw += ridgedNoise(seed ^ SEED_RIDGES + 1, sx / 600.0, sz / 600.0) * 0.5;
        // Мелкие гребни
        ridgeRaw += ridgedNoise(seed ^ SEED_RIDGES + 2, sx / 300.0, sz / 300.0) * 0.25;
        ridgeRaw /= 1.75;
        // ridgeRaw в [0, 1]

        // Маска гор — определяет ГДЕ появляются горы
        double mountainMask = 0.0;
        mountainMask += OpenSimplex2.noise2(seed ^ SEED_MOUNTAIN_MASK, sx / 3500.0, sz / 3500.0) * 1.0;
        mountainMask += OpenSimplex2.noise2(seed ^ SEED_MOUNTAIN_MASK + 1, sx / 1800.0, sz / 1800.0) * 0.4;
        mountainMask /= 1.4;
        // Превращаем в маску [0, 1] с резким переходом
        mountainMask = smoothStep(mountainMask, 0.1, 0.5);

        // Комбинируем: горы появляются только где маска высокая И эрозия низкая
        double mountainFactor = ridgeRaw * mountainMask * (0.3 + 0.7 * (1.0 - erosion));

        // ============================================================
        // СЛОЙ 4: DETAIL NOISE — многооктавный шум деталей
        // ============================================================
        double detail = 0.0;
        detail += OpenSimplex2.noise2(seed ^ SEED_DETAIL1, sx / 400.0, sz / 400.0) * 1.0;
        detail += OpenSimplex2.noise2(seed ^ SEED_DETAIL2, sx / 200.0, sz / 200.0) * 0.5;
        detail += OpenSimplex2.noise2(seed ^ SEED_DETAIL3, sx / 100.0, sz / 100.0) * 0.25;
        detail += OpenSimplex2.noise2(seed ^ SEED_MICRO, sx / 50.0, sz / 50.0) * 0.125;
        detail /= 1.875;
        // detail примерно в [-1, 1]

        // ============================================================
        // СЛОЙ 5: ПЛАТО / ТЕРРАСЫ — на больших высотах
        // ============================================================
        double plateauNoise = OpenSimplex2.noise2(seed ^ SEED_PLATEAU, sx / 800.0, sz / 800.0);

        // ============================================================
        // СБОРКА ФИНАЛЬНОЙ ВЫСОТЫ
        // ============================================================

        double height;

        if (continentalness < -0.2) {
            // === ОКЕАН ===
            // Глубина океана: от SEA_LEVEL до SEA_LEVEL - 600
            double oceanDepth = remap(continentalness, -1.0, -0.2, 1.0, 0.0);
            oceanDepth = clampD(oceanDepth, 0.0, 1.0);
            // oceanDepth: 1 = глубокий океан, 0 = у берега

            double baseOceanFloor = SEA_LEVEL - 80 - oceanDepth * 500.0;

            // Детали дна океана (слабые)
            double oceanDetail = detail * 30.0 * (1.0 - oceanDepth * 0.5);

            height = baseOceanFloor + oceanDetail;

        } else if (continentalness < 0.05) {
            // === ПОБЕРЕЖЬЕ / ШЕЛЬФ ===
            // Плавный переход от океана к суше
            double coastT = remap(continentalness, -0.2, 0.05, 0.0, 1.0);
            coastT = clampD(coastT, 0.0, 1.0);
            coastT = smootherStep(coastT); // Плавный S-curve

            double oceanSide = SEA_LEVEL - 80 + detail * 20.0;
            double landSide = SEA_LEVEL + 5 + detail * 15.0;

            height = lerp(oceanSide, landSide, coastT);

        } else if (continentalness < 0.35) {
            // === РАВНИНЫ / НИЗМЕННОСТИ ===
            double plainT = remap(continentalness, 0.05, 0.35, 0.0, 1.0);
            plainT = clampD(plainT, 0.0, 1.0);

            // Базовая высота равнин: SEA_LEVEL + 5 .. SEA_LEVEL + 60
            double baseHeight = SEA_LEVEL + 5 + plainT * 55.0;

            // Эрозия сглаживает равнины
            double detailStrength = 20.0 + 40.0 * (1.0 - erosion);
            double plainDetail = detail * detailStrength;

            // Небольшие холмы на равнинах
            double hilliness = mountainFactor * 80.0;

            height = baseHeight + plainDetail + hilliness;

        } else if (continentalness < 0.55) {
            // === ХОЛМЫ / ПРЕДГОРЬЯ ===
            double hillT = remap(continentalness, 0.35, 0.55, 0.0, 1.0);
            hillT = clampD(hillT, 0.0, 1.0);

            double baseHeight = SEA_LEVEL + 60 + hillT * 200.0;

            // Больше деталей и горного влияния
            double detailStrength = 40.0 + 80.0 * (1.0 - erosion);
            double hillDetail = detail * detailStrength;

            double mountainContrib = mountainFactor * 300.0;

            height = baseHeight + hillDetail + mountainContrib;

        } else {
            // === ГОРЫ ===
            double mountT = remap(continentalness, 0.55, 1.0, 0.0, 1.0);
            mountT = clampD(mountT, 0.0, 1.0);

            // Базовая высота гор: SEA_LEVEL + 260 .. SEA_LEVEL + 600
            double baseHeight = SEA_LEVEL + 260 + mountT * 340.0;

            // Горные хребты — основной вклад в высоту
            // mountainFactor [0..1] * до 1400 блоков = пики до ~2000+ над уровнем моря
            double peakHeight = mountainFactor * (800.0 + mountT * 600.0);

            // Возведение в степень для более острых пиков
            double sharpPeak = Math.pow(mountainFactor, 1.5) * 400.0 * mountT;

            // Детали на горах
            double detailStrength = 30.0 + 60.0 * (1.0 - erosion * 0.5);
            double mountDetail = detail * detailStrength;

            height = baseHeight + peakHeight + sharpPeak + mountDetail;

            // === ТЕРРАСИРОВАНИЕ на больших высотах ===
            if (height > SEA_LEVEL + 400 && plateauNoise > 0.1) {
                double terraceStrength = clampD(remap(plateauNoise, 0.1, 0.6, 0.0, 1.0), 0.0, 1.0);
                double terraceScale = 80.0; // высота одной террасы
                double terraced = Math.floor(height / terraceScale) * terraceScale;
                height = lerp(height, terraced, terraceStrength * 0.4);
            }
        }

        // ============================================================
        // РЕЧНЫЕ ДОЛИНЫ — вырезаем каньоны/долины
        // ============================================================
        double riverNoise = OpenSimplex2.noise2(seed ^ SEED_RIVER, sx / 800.0, sz / 800.0);
        double riverNoise2 = OpenSimplex2.noise2(seed ^ SEED_RIVER + 1, sx / 400.0, sz / 400.0) * 0.3;
        double riverVal = Math.abs(riverNoise + riverNoise2);

        // Ширина реки
        double riverWidth = 0.04;
        if (riverVal < riverWidth && height > SEA_LEVEL - 10) {
            double riverDepthFactor = 1.0 - (riverVal / riverWidth);
            riverDepthFactor = riverDepthFactor * riverDepthFactor; // квадратичный профиль
            double maxRiverCarve = 40.0;

            // Реки не вырезают ниже уровня моря
            double carve = riverDepthFactor * maxRiverCarve;
            height -= carve;
            if (height < SEA_LEVEL - 5) {
                height = SEA_LEVEL - 5;
            }
        }

        // ============================================================
        // ФИНАЛЬНОЕ ОГРАНИЧЕНИЕ
        // ============================================================
        return (int) Math.round(height);
    }

    /**
     * Заполняет колонку блоков в ChunkPrimer
     */
    private void fillColumn(ChunkPrimer primer, int localX, int localZ, int terrainHeight) {
        int maxY = WorldHeightConfig.WORLD_HEIGHT;

        for (int y = 0; y < maxY; ++y) {
            IBlockState state = null;

            if (y == 0) {
                // Бедрок на дне
                state = Blocks.BEDROCK.getDefaultState();
            } else if (y <= 4) {
                // Слой бедрока с рандомными дырками (как в ваниле)
                // Простая псевдо-рандомизация
                if (((y * 3 + localX * 7 + localZ * 13) & 3) != 0) {
                    state = Blocks.BEDROCK.getDefaultState();
                } else {
                    state = Blocks.STONE.getDefaultState();
                }
            } else if (y < terrainHeight) {
                // Под поверхностью
                state = getSubsurfaceBlock(y, terrainHeight);
            } else if (y == terrainHeight) {
                // Поверхность
                state = getSurfaceBlock(terrainHeight);
            } else if (y <= SEA_LEVEL) {
                // Вода
                state = Blocks.WATER.getDefaultState();
            }
            // Выше — воздух (null, ChunkPrimer по умолчанию air)

            if (state != null) {
                primer.setBlockState(localX, y, localZ, state);
            }
        }
    }

    /**
     * Определяет блок поверхности в зависимости от высоты
     */
    private IBlockState getSurfaceBlock(int terrainHeight) {
        if (terrainHeight <= SEA_LEVEL + 1) {
            // Пляж / дно у воды
            return Blocks.SAND.getDefaultState();
        } else if (terrainHeight < SEA_LEVEL + 5) {
            // Низкий берег — песок/трава
            return Blocks.GRASS.getDefaultState();
        } else if (terrainHeight < SEA_LEVEL + 800) {
            // Обычная трава
            return Blocks.GRASS.getDefaultState();
        } else if (terrainHeight < SEA_LEVEL + 1200) {
            // Высокогорье — камень с гравием
            return Blocks.STONE.getDefaultState();
        } else {
            // Заснеженные вершины
            return Blocks.SNOW.getDefaultState();
        }
    }

    /**
     * Определяет блок под поверхностью
     */
    private IBlockState getSubsurfaceBlock(int y, int terrainHeight) {
        int depth = terrainHeight - y;

        if (terrainHeight <= SEA_LEVEL + 1) {
            // Подводное дно
            if (depth < 4) {
                return Blocks.SAND.getDefaultState();
            } else if (depth < 8) {
                return Blocks.SANDSTONE.getDefaultState();
            } else {
                return Blocks.STONE.getDefaultState();
            }
        } else if (terrainHeight < SEA_LEVEL + 800) {
            // Обычная суша
            if (depth < 4) {
                return Blocks.DIRT.getDefaultState();
            } else {
                return Blocks.STONE.getDefaultState();
            }
        } else if (terrainHeight < SEA_LEVEL + 1200) {
            // Высокогорье
            if (depth < 2) {
                return Blocks.GRAVEL.getDefaultState();
            } else {
                return Blocks.STONE.getDefaultState();
            }
        } else {
            // Снежные пики
            if (depth < 3) {
                return Blocks.PACKED_ICE.getDefaultState();
            } else {
                return Blocks.STONE.getDefaultState();
            }
        }
    }

    /**
     * Назначает биомы чанку на основе высоты и позиции
     */
    private void assignBiomes(Chunk chunk, int chunkX, int chunkZ) {
        byte[] biomes = chunk.getBiomeArray();

        for (int localZ = 0; localZ < 16; localZ++) {
            for (int localX = 0; localX < 16; localX++) {
                double wx = (double) ((chunkX << 4) | localX);
                double wz = (double) ((chunkZ << 4) | localZ);

                int h = computeTerrainHeight(wx, wz);
                int biomeId = getBiomeForHeight(h);

                int index = (localZ & 0xF) << 4 | (localX & 0xF);
                biomes[index] = (byte) biomeId;
            }
        }
    }

    /**
     * Выбирает биом по высоте рельефа
     */
    private int getBiomeForHeight(int height) {
        if (height < SEA_LEVEL - 200) {
            return Biome.getIdForBiome(Biome.getBiome(0));  // Ocean
        } else if (height < SEA_LEVEL - 30) {
            return Biome.getIdForBiome(Biome.getBiome(0));  // Ocean
        } else if (height <= SEA_LEVEL + 2) {
            return Biome.getIdForBiome(Biome.getBiome(16)); // Beach
        } else if (height < SEA_LEVEL + 80) {
            return Biome.getIdForBiome(Biome.getBiome(1));  // Plains
        } else if (height < SEA_LEVEL + 200) {
            return Biome.getIdForBiome(Biome.getBiome(4));  // Forest
        } else if (height < SEA_LEVEL + 500) {
            return Biome.getIdForBiome(Biome.getBiome(34)); // Extreme Hills+ (Windswept Forest)
        } else if (height < SEA_LEVEL + 800) {
            return Biome.getIdForBiome(Biome.getBiome(3));  // Extreme Hills
        } else if (height < SEA_LEVEL + 1200) {
            return Biome.getIdForBiome(Biome.getBiome(13)); // Ice Mountains
        } else {
            return Biome.getIdForBiome(Biome.getBiome(12)); // Ice Plains (снежные пики)
        }
    }

    // ================================================================
    // УТИЛИТЫ
    // ================================================================

    /**
     * Ridged noise — инвертированный абсолютный шум.
     * Создаёт острые хребты. Результат в [0, 1].
     */
    private double ridgedNoise(long s, double x, double z) {
        double n = OpenSimplex2.noise2(s, x, z);
        return 1.0 - Math.abs(n);
    }

    /**
     * Линейная интерполяция
     */
    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /**
     * Переотображение значения из одного диапазона в другой
     */
    private double remap(double value, double fromLow, double fromHigh, double toLow, double toHigh) {
        double t = (value - fromLow) / (fromHigh - fromLow);
        return toLow + t * (toHigh - toLow);
    }

    /**
     * Smooth step — плавный переход [0,1] с нулевыми производными на краях
     * Используется для маски: значения ниже edge0 -> 0, выше edge1 -> 1
     */
    private double smoothStep(double value, double edge0, double edge1) {
        double t = clampD((value - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /**
     * Smoother step — ещё более плавный S-curve (Кен Перлин)
     */
    private double smootherStep(double t) {
        t = clampD(t, 0.0, 1.0);
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    /**
     * Ограничение int
     */
    private int clamp(int val, int min, int max) {
        return val < min ? min : (val > max ? max : val);
    }

    /**
     * Ограничение double
     */
    private double clampD(double val, double min, double max) {
        return val < min ? min : (val > max ? max : val);
    }

    // ================================================================
    // СТАНДАРТНЫЕ МЕТОДЫ IChunkGenerator
    // ================================================================

    @Override
    public void populate(int x, int z) {
        // Будущая популяция: деревья, руды, структуры
    }

    @Override
    public boolean generateStructures(Chunk chunkIn, int x, int z) {
        return false;
    }

    @Override
    public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
        Biome biome = this.world.getBiome(pos);
        return biome.getSpawnableList(creatureType);
    }

    @Nullable
    @Override
    public BlockPos getNearestStructurePos(World worldIn, String structureName, BlockPos position,
                                           boolean findUnexplored) {
        return null;
    }

    @Override
    public void recreateStructures(Chunk chunkIn, int x, int z) {
    }

    @Override
    public boolean isInsideStructure(World worldIn, String structureName, BlockPos pos) {
        return false;
    }
}
