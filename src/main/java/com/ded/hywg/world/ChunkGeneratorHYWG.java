package com.ded.hywg.world;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.IChunkGenerator;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

public class ChunkGeneratorHYWG implements IChunkGenerator {

    private final World world;

    public ChunkGeneratorHYWG(World world) {
        this.world = world;
    }

    @Override
    public Chunk generateChunk(int x, int z) {
        ChunkPrimer chunkprimer = new ChunkPrimer();
        
        IBlockState bedrock = Blocks.BEDROCK.getDefaultState();
        IBlockState stone = Blocks.STONE.getDefaultState();
        IBlockState dirt = Blocks.DIRT.getDefaultState();
        IBlockState grass = Blocks.GRASS.getDefaultState();

        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                chunkprimer.setBlockState(i, 0, j, bedrock);
                for (int y = 1; y <= 2043; ++y) {
                    chunkprimer.setBlockState(i, y, j, stone);
                }
                for (int y = 2044; y <= 2046; ++y) {
                    chunkprimer.setBlockState(i, y, j, dirt);
                }
                chunkprimer.setBlockState(i, 2047, j, grass);
            }
        }

        Chunk chunk = new Chunk(this.world, chunkprimer, x, z);
        
        Biome[] abiome = this.world.getBiomeProvider().getBiomes(null, x * 16, z * 16, 16, 16);
        byte[] abyte = chunk.getBiomeArray();
        for (int l = 0; l < abyte.length; ++l) {
            abyte[l] = (byte)Biome.getIdForBiome(abiome[l]);
        }
        
        chunk.generateSkylightMap();
        return chunk;
    }

    @Override
    public void populate(int x, int z) {

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

    @Override
    public @Nullable BlockPos getNearestStructurePos(World worldIn, String structureName, BlockPos position, boolean findUnexplored) {
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
