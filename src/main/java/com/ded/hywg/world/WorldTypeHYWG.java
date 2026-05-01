package com.ded.hywg.world;

import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.IChunkGenerator;

public class WorldTypeHYWG extends WorldType {

    public WorldTypeHYWG() {
        super("hywg");
    }

    @Override
    public IChunkGenerator getChunkGenerator(World world, String generatorOptions) {
        return new ChunkGeneratorHYWG(world);
    }

    @Override
    public net.minecraft.world.biome.BiomeProvider getBiomeProvider(World world) {
        return new BiomeProviderHYWG(world);
    }
}
