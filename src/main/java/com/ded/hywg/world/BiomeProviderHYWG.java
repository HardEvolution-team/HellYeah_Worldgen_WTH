package com.ded.hywg.world;

import com.ded.hywg.noise.OpenSimplex2;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

public class BiomeProviderHYWG extends BiomeProvider {

    private final long seed;

    // =====================================================================
    //  Decorrelated noise seeds
    // =====================================================================
    private final long SEED_WARP_X1, SEED_WARP_Z1;
    private final long SEED_WARP_X2, SEED_WARP_Z2;
    private final long SEED_WARP_X3, SEED_WARP_Z3;
    private final long SEED_TEMP, SEED_MOIST, SEED_CONT;
    private final long SEED_RIVER_A, SEED_RIVER_B, SEED_RIVER_C;
    private final long SEED_EROSION, SEED_PEAKS, SEED_DETAIL;
    private final long SEED_COAST_WARP_X, SEED_COAST_WARP_Z;
    private final long SEED_RARE;
    private final long SEED_VOLCANIC;
    private final long SEED_LATITUDE_WARP;
    private final long SEED_SWAMP;
    private final long SEED_MUSHROOM;

    // =====================================================================
    //  Scale constants — EPIC scale for 4096-block world height
    //  All values dramatically reduced for continent-sized biomes
    // =====================================================================

    // Temperature & moisture: ~25,000-35,000 block biome regions
    private static final double BIOME_SCALE_TEMP   = 0.000030;
    private static final double BIOME_SCALE_MOIST  = 0.000035;

    // Continentalness: ~80,000+ block continents
    private static final double CONTINENT_SCALE    = 0.000012;

    // Domain warping — three levels for maximally organic shapes
    private static final double WARP_SCALE_1       = 0.000040;
    private static final double WARP_STRENGTH_1    = 6000.0;
    private static final double WARP_SCALE_2       = 0.000120;
    private static final double WARP_STRENGTH_2    = 2200.0;
    private static final double WARP_SCALE_3       = 0.000350;
    private static final double WARP_STRENGTH_3    = 700.0;

    // Rivers: epic scale, wide rivers
    private static final double RIVER_SCALE        = 0.000090;
    private static final double RIVER_THRESHOLD    = 0.032;

    // Erosion & peaks
    private static final double EROSION_SCALE      = 0.000055;
    private static final double PEAKS_SCALE        = 0.000180;

    // Rare biome features
    private static final double RARE_SCALE         = 0.000250;
    private static final double MUSHROOM_SCALE     = 0.000080;

    // Latitude influence on temperature (gives polar→equatorial gradient)
    private static final double LATITUDE_TEMP_INFLUENCE = 0.000008;

    public BiomeProviderHYWG(World world) {
        super(world.getWorldInfo());
        this.seed = world.getSeed();

        Random r = new Random(seed);
        SEED_WARP_X1       = r.nextLong();
        SEED_WARP_Z1       = r.nextLong();
        SEED_WARP_X2       = r.nextLong();
        SEED_WARP_Z2       = r.nextLong();
        SEED_WARP_X3       = r.nextLong();
        SEED_WARP_Z3       = r.nextLong();
        SEED_TEMP          = r.nextLong();
        SEED_MOIST         = r.nextLong();
        SEED_CONT          = r.nextLong();
        SEED_RIVER_A       = r.nextLong();
        SEED_RIVER_B       = r.nextLong();
        SEED_RIVER_C       = r.nextLong();
        SEED_EROSION       = r.nextLong();
        SEED_PEAKS         = r.nextLong();
        SEED_DETAIL        = r.nextLong();
        SEED_COAST_WARP_X  = r.nextLong();
        SEED_COAST_WARP_Z  = r.nextLong();
        SEED_RARE          = r.nextLong();
        SEED_VOLCANIC      = r.nextLong();
        SEED_LATITUDE_WARP = r.nextLong();
        SEED_SWAMP         = r.nextLong();
        SEED_MUSHROOM      = r.nextLong();
    }

    // =====================================================================
    //  Fractal Brownian Motion
    // =====================================================================
    private double fbm(long seedVal, double x, double z,
                       double scale, int octaves, double lacunarity, double gain) {
        double value = 0.0;
        double amplitude = 1.0;
        double frequency = scale;
        double maxValue = 0.0;

        for (int i = 0; i < octaves; i++) {
            value    += OpenSimplex2.noise2(seedVal + i * 31337L,
                    x * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            frequency *= lacunarity;
            amplitude *= gain;
        }
        return value / maxValue;
    }

    // =====================================================================
    //  Ridge noise — absolute value creates ridge-like features
    // =====================================================================
    private double ridgeNoise(long seedVal, double x, double z,
                              double scale, int octaves, double lacunarity, double gain) {
        double value = 0.0;
        double amplitude = 1.0;
        double frequency = scale;
        double maxValue = 0.0;

        for (int i = 0; i < octaves; i++) {
            double n = OpenSimplex2.noise2(seedVal + i * 31337L,
                    x * frequency, z * frequency);
            // Ridge: invert absolute value so ridges are peaks
            value    += (1.0 - Math.abs(n)) * amplitude;
            maxValue += amplitude;
            frequency *= lacunarity;
            amplitude *= gain;
        }
        return value / maxValue; // [0, 1]
    }

    // =====================================================================
    //  Continentalness with coastal warping and multi-octave detail
    // =====================================================================
    private double getContinentalness(double x, double z) {
        // Large-scale coastal warping for interesting continent shapes
        double cw1 = fbm(SEED_COAST_WARP_X, x, z, 0.000030, 4, 2.0, 0.5) * 3500.0;
        double cw2 = fbm(SEED_COAST_WARP_Z, x, z, 0.000035, 4, 2.0, 0.5) * 3500.0;

        // Medium detail warping for fjords, peninsulas, bays
        double cw3 = fbm(SEED_COAST_WARP_X + 9999L, x, z, 0.000120, 3, 2.0, 0.5) * 1200.0;
        double cw4 = fbm(SEED_COAST_WARP_Z + 9999L, x, z, 0.000120, 3, 2.0, 0.5) * 1200.0;

        double wx = x + cw1 + cw3;
        double wz = z + cw2 + cw4;

        // Multi-octave continentalness for varied coastline at all scales
        double c = fbm(SEED_CONT, wx, wz, CONTINENT_SCALE, 6, 2.0, 0.5);

        // Add subtle large-scale variation to prevent uniform continent sizes
        double variation = fbm(SEED_CONT + 77777L, x, z, CONTINENT_SCALE * 0.3, 3, 2.0, 0.5) * 0.15;
        c += variation;

        return c;
    }

    // =====================================================================
    //  River network — three layers for realistic drainage patterns
    //  Main rivers, tributaries, and small streams
    // =====================================================================
    private double getRiverValue(double x, double z) {
        // Primary rivers — large scale, wide
        double r1 = fbm(SEED_RIVER_A, x, z, RIVER_SCALE, 4, 2.0, 0.5);
        // Secondary tributaries
        double r2 = fbm(SEED_RIVER_B, x, z, RIVER_SCALE * 2.1, 3, 2.0, 0.5);
        // Small streams / creeks
        double r3 = fbm(SEED_RIVER_C, x, z, RIVER_SCALE * 4.5, 2, 2.0, 0.5);

        double ridge1 = Math.abs(r1);
        double ridge2 = Math.abs(r2) * 1.3;  // Slightly wider threshold = narrower rivers
        double ridge3 = Math.abs(r3) * 1.8;  // Even narrower streams

        return Math.min(ridge1, Math.min(ridge2, ridge3));
    }

    // =====================================================================
    //  Erosion — multi-scale for varied terrain roughness
    // =====================================================================
    private double getErosion(double x, double z) {
        double e1 = fbm(SEED_EROSION, x, z, EROSION_SCALE, 5, 2.0, 0.5);
        // Add large-scale erosion trend for continent-scale mountain ranges
        double e2 = fbm(SEED_EROSION + 55555L, x, z, EROSION_SCALE * 0.35, 3, 2.0, 0.5) * 0.4;
        return e1 + e2;
    }

    // =====================================================================
    //  Peaks — sharp mountain peaks with ridge noise
    // =====================================================================
    private double getPeaks(double x, double z) {
        // Ridge noise creates sharp mountain ridges
        double ridges = ridgeNoise(SEED_PEAKS, x, z, PEAKS_SCALE, 4, 2.2, 0.45);
        // Standard fbm for broader mountain masses
        double broad = fbm(SEED_PEAKS + 12345L, x, z, PEAKS_SCALE * 0.5, 3, 2.0, 0.5);

        // Combine: ridges dominate in mountain areas, broad sets the base
        return ridges * 0.6 + broad * 0.4;
    }

    // =====================================================================
    //  Three-level domain warping for temperature & moisture
    //  Creates continent-scale flowing biome shapes
    // =====================================================================
    private double[] sampleWarped(double x, double z) {
        // --- Level 1: Continental-scale warping (huge flowing shapes) ---
        double wx1 = fbm(SEED_WARP_X1, x, z, WARP_SCALE_1, 4, 2.0, 0.5) * WARP_STRENGTH_1;
        double wz1 = fbm(SEED_WARP_Z1, x, z, WARP_SCALE_1, 4, 2.0, 0.5) * WARP_STRENGTH_1;

        double px = x + wx1;
        double pz = z + wz1;

        // --- Level 2: Regional warping (biome-scale flowing) ---
        double wx2 = fbm(SEED_WARP_X2, px, pz, WARP_SCALE_2, 3, 2.0, 0.5) * WARP_STRENGTH_2;
        double wz2 = fbm(SEED_WARP_Z2, px, pz, WARP_SCALE_2, 3, 2.0, 0.5) * WARP_STRENGTH_2;

        double qx = px + wx2;
        double qz = pz + wz2;

        // --- Level 3: Local detail warping (organic borders) ---
        double wx3 = fbm(SEED_WARP_X3, qx, qz, WARP_SCALE_3, 2, 2.0, 0.5) * WARP_STRENGTH_3;
        double wz3 = fbm(SEED_WARP_Z3, qx, qz, WARP_SCALE_3, 2, 2.0, 0.5) * WARP_STRENGTH_3;

        double fx = qx + wx3;
        double fz = qz + wz3;

        // --- Temperature: base noise + latitude influence ---
        double tempNoise = fbm(SEED_TEMP, fx, fz, BIOME_SCALE_TEMP, 5, 2.0, 0.5);

        // Latitude-based temperature: warped Z creates wavy climate bands
        double latWarp = fbm(SEED_LATITUDE_WARP, x, z, 0.000015, 3, 2.0, 0.5) * 8000.0;
        double latitudeInfluence = Math.sin((z + latWarp) * LATITUDE_TEMP_INFLUENCE) * 0.35;

        double temp = tempNoise * 0.65 + latitudeInfluence;

        // --- Moisture: independent noise ---
        double moist = fbm(SEED_MOIST, fx, fz, BIOME_SCALE_MOIST, 5, 2.0, 0.5);

        // Moisture slightly influenced by continentalness (inland = drier)
        double cont = getContinentalness(x, z);
        double inlandDrying = smoothstep(0.0, 0.6, cont) * -0.15;
        moist += inlandDrying;

        return new double[]{
                clamp(temp, -1.0, 1.0),
                clamp(moist, -1.0, 1.0)
        };
    }

    // =====================================================================
    //  Utility functions
    // =====================================================================
    private static double smoothstep(double edge0, double edge1, double x) {
        double t = clamp01((x - edge0) / (edge1 - edge0));
        return t * t * (3.0 - 2.0 * t);
    }

    private static double clamp01(double v) {
        return v < 0.0 ? 0.0 : (v > 1.0 ? 1.0 : v);
    }

    private static double clamp(double v, double min, double max) {
        return v < min ? min : (v > max ? max : v);
    }

    // =====================================================================
    //  Rare biome detection — mushroom islands, flower forests, etc.
    // =====================================================================
    private double getRareValue(double x, double z) {
        return fbm(SEED_RARE, x, z, RARE_SCALE, 2, 2.0, 0.5);
    }

    private double getMushroomValue(double x, double z) {
        // Mushroom islands: very specific noise pattern in ocean
        double m = fbm(SEED_MUSHROOM, x, z, MUSHROOM_SCALE, 3, 2.0, 0.5);
        return m;
    }

    private double getSwampValue(double x, double z) {
        return fbm(SEED_SWAMP, x, z, 0.000150, 3, 2.0, 0.5);
    }

    // =====================================================================
    //  MAIN BIOME SELECTION
    // =====================================================================
    private Biome getBiomeAt(int x, int z) {
        double dx = (double) x;
        double dz = (double) z;

        // =================================================================
        //  Step 1: Continentalness — Deep Ocean / Ocean / Coast / Land
        // =================================================================
        double cont = getContinentalness(dx, dz);

        // Deep ocean
        if (cont < -0.45) {
            // Mushroom island — rare volcanic islands in deep ocean
            double mushroom = getMushroomValue(dx, dz);
            if (mushroom > 0.72) {
                return Biomes.MUSHROOM_ISLAND;
            }
            return Biomes.DEEP_OCEAN;
        }

        // Ocean
        if (cont < -0.22) {
            // Mushroom island shores
            double mushroom = getMushroomValue(dx, dz);
            if (mushroom > 0.72) {
                return Biomes.MUSHROOM_ISLAND_SHORE;
            }
            return Biomes.OCEAN;
        }

        // Get climate data (needed for beach type and everything else)
        double[] tm = sampleWarped(dx, dz);
        double temp  = tm[0];
        double moist = tm[1];
        double erosion = getErosion(dx, dz);

        // Beach / coastal zone
        if (cont < -0.10) {
            // Narrow beach strip
            if (cont < -0.16) {
                // Outer beach — can be stone cliffs
                if (erosion < -0.45) return Biomes.STONE_BEACH;
                if (temp < -0.40)    return Biomes.COLD_BEACH;
                return Biomes.BEACH;
            }
            // Inner beach — transitional
            if (erosion < -0.50) return Biomes.STONE_BEACH;
            if (temp < -0.40)    return Biomes.COLD_BEACH;
            return Biomes.BEACH;
        }

        // =================================================================
        //  Step 2: Erosion & Peaks for terrain classification
        // =================================================================
        double peaks = getPeaks(dx, dz);

        // Terrain categories based on erosion
        // erosion < -0.40 → extreme mountains (volcanic, glacial)
        // erosion < -0.20 → mountains
        // erosion < 0.05  → hills
        // erosion < 0.30  → rolling/plains
        // erosion >= 0.30 → flat (river valleys, deltas)
        boolean isExtremeMountain = erosion < -0.40;
        boolean isMountain        = erosion < -0.20 && !isExtremeMountain;
        boolean isHilly           = erosion >= -0.20 && erosion < 0.05;
        boolean isFlat            = erosion >= 0.30;

        boolean isAnyMountain     = erosion < -0.20;
        boolean isExtremePeak     = isAnyMountain && peaks > 0.55;

        // =================================================================
        //  Step 3: River detection
        //  Rivers avoid extreme mountains, get wider in flat terrain
        // =================================================================
        double riverVal = getRiverValue(dx, dz);

        double inlandFactor = smoothstep(-0.10, 0.35, cont);
        // Rivers wider in flat terrain, narrower in hills
        double terrainRiverMod = isFlat ? 1.4 : (isHilly ? 0.7 : 0.5);
        double effectiveRiverThreshold = RIVER_THRESHOLD * (0.3 + 0.7 * inlandFactor) * terrainRiverMod;

        if (riverVal < effectiveRiverThreshold && !isAnyMountain) {
            if (temp < -0.35) return Biomes.FROZEN_RIVER;
            return Biomes.RIVER;
        }

        // =================================================================
        //  Step 4: Mountain biomes
        // =================================================================
        if (isExtremeMountain) {
            return getExtremeMountainBiome(temp, moist, peaks);
        }
        if (isMountain) {
            return getMountainBiome(temp, moist, peaks, isExtremePeak);
        }

        // =================================================================
        //  Step 5: Whittaker biome table with rare biome overlays
        // =================================================================
        double rareVal  = getRareValue(dx, dz);
        double swampVal = getSwampValue(dx, dz);

        Biome baseBiome = getWhittakerBiome(temp, moist, rareVal, swampVal, cont);

        // =================================================================
        //  Step 6: Hills variants
        // =================================================================
        if (isHilly) {
            baseBiome = getHillyVariant(baseBiome);
        }

        return baseBiome;
    }

    // =====================================================================
    //  Extreme mountain biomes — volcanic peaks, glacial ridges
    //  These are the tallest, most dramatic terrain features
    // =====================================================================
    private Biome getExtremeMountainBiome(double temp, double moist, double peaks) {
        // Highest peaks — always bare stone/ice regardless of climate
        if (peaks > 0.60) {
            if (temp < -0.10) return Biomes.ICE_MOUNTAINS;
            return Biomes.EXTREME_HILLS;
        }

        // High ridges
        if (peaks > 0.35) {
            if (temp < -0.30) return Biomes.ICE_MOUNTAINS;
            if (temp < 0.10)  return Biomes.EXTREME_HILLS;
            // Hot extreme mountains — mesa formations
            if (moist < -0.20) return Biomes.MESA_ROCK;
            return Biomes.EXTREME_HILLS;
        }

        // Mountain slopes — forested based on climate
        if (temp < -0.40) {
            return moist > -0.10 ? Biomes.COLD_TAIGA : Biomes.ICE_MOUNTAINS;
        }
        if (temp < -0.10) {
            if (moist > 0.25) return Biomes.REDWOOD_TAIGA;
            return Biomes.EXTREME_HILLS_WITH_TREES;
        }
        if (temp < 0.25) {
            if (moist > 0.30) return Biomes.ROOFED_FOREST;
            return Biomes.EXTREME_HILLS_WITH_TREES;
        }
        // Hot extreme mountains
        if (moist < -0.25) return Biomes.MESA_ROCK;
        if (moist > 0.30)  return Biomes.JUNGLE_HILLS;
        return Biomes.SAVANNA_PLATEAU;
    }

    // =====================================================================
    //  Mountain biomes — standard mountains with vertical zonality
    // =====================================================================
    private Biome getMountainBiome(double temp, double moist, double peaks, boolean extreme) {
        if (extreme) {
            if (temp < -0.15) return Biomes.ICE_MOUNTAINS;
            return Biomes.EXTREME_HILLS;
        }

        // Upper mountain — sparse/bare
        if (peaks > 0.30) {
            if (temp < -0.25) return Biomes.ICE_MOUNTAINS;
            if (temp < 0.15)  return Biomes.EXTREME_HILLS;
            if (moist < -0.20) return Biomes.MESA_CLEAR_ROCK;
            return Biomes.EXTREME_HILLS;
        }

        // Mid mountain — wooded
        if (peaks > 0.05) {
            if (temp < -0.35) {
                return moist > 0.0 ? Biomes.COLD_TAIGA_HILLS : Biomes.ICE_MOUNTAINS;
            }
            if (temp < -0.05) {
                if (moist > 0.20) return Biomes.REDWOOD_TAIGA_HILLS;
                return Biomes.EXTREME_HILLS_WITH_TREES;
            }
            if (temp < 0.30) {
                if (moist > 0.35) return Biomes.BIRCH_FOREST_HILLS;
                if (moist > 0.05) return Biomes.FOREST_HILLS;
                return Biomes.EXTREME_HILLS_WITH_TREES;
            }
            // Hot mid-mountain
            if (moist > 0.25) return Biomes.JUNGLE_HILLS;
            if (moist > -0.10) return Biomes.SAVANNA_PLATEAU;
            return Biomes.MESA_ROCK;
        }

        // Lower mountain / foothills — climate-appropriate forest
        if (temp < -0.35) {
            return moist > 0.10 ? Biomes.COLD_TAIGA : Biomes.ICE_PLAINS;
        }
        if (temp < -0.05) {
            if (moist > 0.30) return Biomes.REDWOOD_TAIGA;
            if (moist > -0.10) return Biomes.TAIGA;
            return Biomes.EXTREME_HILLS_WITH_TREES;
        }
        if (temp < 0.30) {
            if (moist > 0.40) return Biomes.ROOFED_FOREST;
            if (moist > 0.10) return Biomes.FOREST;
            return Biomes.EXTREME_HILLS_WITH_TREES;
        }
        // Hot foothills
        if (moist > 0.30) return Biomes.JUNGLE_EDGE;
        if (moist > -0.10) return Biomes.SAVANNA;
        return Biomes.DESERT_HILLS;
    }

    // =====================================================================
    //  Whittaker biome table — comprehensive climate-based selection
    //  with rare biome overlays and swamp detection
    //
    //  Temperature zones (7 bands):
    //    FROZEN   < -0.50
    //    COLD     -0.50 to -0.25
    //    COOL     -0.25 to -0.02
    //    MILD      -0.02 to 0.18
    //    WARM      0.18 to 0.38
    //    HOT       0.38 to 0.58
    //    SCORCHING > 0.58
    //
    //  Moisture zones (5 bands):
    //    ARID     < -0.35
    //    DRY      -0.35 to -0.08
    //    MODERATE -0.08 to 0.18
    //    WET       0.18 to 0.45
    //    SOAKING  > 0.45
    // =====================================================================
    private Biome getWhittakerBiome(double temp, double moist, double rareVal, double swampVal, double cont) {

        // =================================================================
        //  Swamp overlay — occurs in wet, warm-to-mild, flat lowland areas
        //  Uses its own noise to create swamp patches
        // =================================================================
        if (swampVal > 0.40 && moist > 0.10 && temp > -0.10 && temp < 0.45 && cont < 0.25) {
            return Biomes.SWAMPLAND;
        }

        // ==== SCORCHING (temp > 0.58) ====
        if (temp > 0.58) {
            if (moist < -0.45) return Biomes.MESA;
            if (moist < -0.20) return Biomes.MESA_CLEAR_ROCK;
            if (moist < 0.05)  return Biomes.DESERT;
            if (moist < 0.30)  return Biomes.SAVANNA;
            if (moist < 0.50)  return Biomes.JUNGLE_EDGE;
            return Biomes.JUNGLE;
        }

        // ==== HOT (temp 0.38 to 0.58) ====
        if (temp > 0.38) {
            if (moist < -0.40) return Biomes.MESA_CLEAR_ROCK;
            if (moist < -0.15) return Biomes.DESERT;
            if (moist < 0.08) {
                // Rare: flower-rich savanna → use savanna
                return Biomes.SAVANNA;
            }
            if (moist < 0.30)  return Biomes.PLAINS;
            if (moist < 0.48)  return Biomes.JUNGLE_EDGE;
            return Biomes.JUNGLE;
        }

        // ==== WARM (temp 0.18 to 0.38) ====
        if (temp > 0.18) {
            if (moist < -0.40) return Biomes.DESERT;
            if (moist < -0.15) {
                // Transition: desert → savanna → plains
                return Biomes.SAVANNA;
            }
            if (moist < 0.05) return Biomes.PLAINS;
            if (moist < 0.25) {
                // Rare: flower forest in warm moderate moisture
                if (rareVal > 0.65) return Biomes.MUTATED_FOREST; // Flower Forest
                return Biomes.FOREST;
            }
            if (moist < 0.45) {
                // Rare: roofed forest patches in warm wet
                if (rareVal > 0.55) return Biomes.ROOFED_FOREST;
                return Biomes.BIRCH_FOREST;
            }
            // Very wet + warm
            if (rareVal > 0.60) return Biomes.JUNGLE_EDGE;
            return Biomes.SWAMPLAND;
        }

        // ==== MILD (temp -0.02 to 0.18) ====
        if (temp > -0.02) {
            if (moist < -0.38) return Biomes.PLAINS;
            if (moist < -0.10) {
                // Dry mild: open plains with occasional trees
                if (rareVal > 0.70) return Biomes.MUTATED_FOREST; // Flower Forest
                return Biomes.PLAINS;
            }
            if (moist < 0.12) {
                // Classic temperate forest
                if (rareVal > 0.68) return Biomes.ROOFED_FOREST;
                return Biomes.FOREST;
            }
            if (moist < 0.32) {
                // Wetter temperate
                if (rareVal > 0.60) return Biomes.ROOFED_FOREST;
                return Biomes.BIRCH_FOREST;
            }
            if (moist < 0.50) return Biomes.ROOFED_FOREST;
            // Soaking mild — mega taiga transition
            return Biomes.REDWOOD_TAIGA;
        }

        // ==== COOL (temp -0.25 to -0.02) ====
        if (temp > -0.25) {
            if (moist < -0.35) {
                // Cool dry steppe
                return Biomes.PLAINS;
            }
            if (moist < -0.05) {
                // Cool moderate — mixed forest
                if (rareVal > 0.65) return Biomes.BIRCH_FOREST;
                return Biomes.FOREST;
            }
            if (moist < 0.18) {
                // Cool wet — transition to taiga
                return Biomes.TAIGA;
            }
            if (moist < 0.42) {
                // Cool very wet — mega spruce
                if (rareVal > 0.55) return Biomes.REDWOOD_TAIGA;
                return Biomes.TAIGA;
            }
            return Biomes.REDWOOD_TAIGA;
        }

        // ==== COLD (temp -0.50 to -0.25) ====
        if (temp > -0.50) {
            if (moist < -0.30) {
                // Cold dry — tundra edge
                return Biomes.ICE_PLAINS;
            }
            if (moist < 0.00) {
                // Cold moderate — sparse cold taiga
                return Biomes.COLD_TAIGA;
            }
            if (moist < 0.30) {
                // Cold wet — dense cold taiga
                return Biomes.COLD_TAIGA;
            }
            // Cold soaking — transition to taiga
            return Biomes.TAIGA;
        }

        // ==== FROZEN (temp < -0.50) ====
        if (moist < -0.25) return Biomes.ICE_PLAINS;
        if (moist < 0.10)  return Biomes.ICE_PLAINS;
        if (moist < 0.35)  return Biomes.COLD_TAIGA;
        return Biomes.ICE_MOUNTAINS;
    }

    // =====================================================================
    //  Hilly variants
    // =====================================================================
    private Biome getHillyVariant(Biome base) {
        if (base == Biomes.PLAINS)            return Biomes.FOREST_HILLS;
        if (base == Biomes.FOREST)            return Biomes.FOREST_HILLS;
        if (base == Biomes.BIRCH_FOREST)      return Biomes.BIRCH_FOREST_HILLS;
        if (base == Biomes.TAIGA)             return Biomes.TAIGA_HILLS;
        if (base == Biomes.COLD_TAIGA)        return Biomes.COLD_TAIGA_HILLS;
        if (base == Biomes.JUNGLE)            return Biomes.JUNGLE_HILLS;
        if (base == Biomes.JUNGLE_EDGE)       return Biomes.JUNGLE_HILLS;
        if (base == Biomes.DESERT)            return Biomes.DESERT_HILLS;
        if (base == Biomes.ICE_PLAINS)        return Biomes.ICE_MOUNTAINS;
        if (base == Biomes.SAVANNA)           return Biomes.SAVANNA_PLATEAU;
        if (base == Biomes.REDWOOD_TAIGA)     return Biomes.REDWOOD_TAIGA_HILLS;
        if (base == Biomes.MESA)              return Biomes.MESA_ROCK;
        if (base == Biomes.MESA_CLEAR_ROCK)   return Biomes.MESA_ROCK;
        if (base == Biomes.ROOFED_FOREST)     return Biomes.FOREST_HILLS;
        if (base == Biomes.SWAMPLAND)         return Biomes.SWAMPLAND; // Swamp stays swamp
        if (base == Biomes.MUTATED_FOREST)    return Biomes.FOREST_HILLS; // Flower forest hills
        return base;
    }

    // =====================================================================
    //  BiomeProvider overrides
    // =====================================================================

    @Override
    public Biome[] getBiomesForGeneration(Biome[] biomes, int x, int z, int width, int height) {
        if (biomes == null || biomes.length < width * height) {
            biomes = new Biome[width * height];
        }
        for (int j = 0; j < height; ++j) {
            for (int i = 0; i < width; ++i) {
                biomes[i + j * width] = getBiomeAt(x + i, z + j);
            }
        }
        return biomes;
    }

    @Override
    public Biome[] getBiomes(@Nullable Biome[] oldBiomeList, int x, int z, int width, int depth,
                             boolean cacheFlag) {
        if (oldBiomeList == null || oldBiomeList.length < width * depth) {
            oldBiomeList = new Biome[width * depth];
        }
        for (int j = 0; j < depth; ++j) {
            for (int i = 0; i < width; ++i) {
                oldBiomeList[i + j * width] = getBiomeAt(x + i, z + j);
            }
        }
        return oldBiomeList;
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        return getBiomeAt(pos.getX(), pos.getZ());
    }

    @Override
    public Biome getBiome(BlockPos pos, Biome defaultBiome) {
        return getBiomeAt(pos.getX(), pos.getZ());
    }
}
