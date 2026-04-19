package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGeneratorOverworld.class)
public class MixinChunkGeneratorOverworld {

    
    
    
    private static final int NEW_VERTICAL = WorldHeightConfig.WORLD_HEIGHT / 8 + 1; 
    private static final int NEW_BUFFER_SIZE = 5 * 5 * NEW_VERTICAL; 

    @Shadow @Final @Mutable private double[] heightMap;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void cam$resizeBuffer(CallbackInfo ci) {
        this.heightMap = new double[NEW_BUFFER_SIZE];
    }

    

    
    @ModifyConstant(method = "setBlocksInChunk", constant = @Constant(intValue = 256), require = 0)
    private int cam$setBlocks256(int v) { return WorldHeightConfig.WORLD_HEIGHT; }

    
    @ModifyConstant(method = "setBlocksInChunk", constant = @Constant(intValue = 32), require = 0)
    private int cam$setBlocks32(int v) { return WorldHeightConfig.WORLD_HEIGHT / 8; }

    
    @ModifyConstant(method = "setBlocksInChunk", constant = @Constant(intValue = 33), require = 0)
    private int cam$setBlocks33(int v) { return NEW_VERTICAL; }

    

    @ModifyConstant(method = "generateHeightmap", constant = @Constant(intValue = 33), require = 0)
    private int cam$genHM33(int v) { return NEW_VERTICAL; }

    
    
    
    @ModifyConstant(method = "generateHeightmap", constant = @Constant(intValue = 29), require = 0)
    private int cam$genHM29(int v) { return NEW_VERTICAL - 4; }

    
    
    

    

    @ModifyConstant(method = "replaceBiomeBlocks", constant = @Constant(intValue = 256), require = 0)
    private int cam$replaceBiome256(int v) { return WorldHeightConfig.WORLD_HEIGHT; }

    

    @ModifyConstant(method = "populate", constant = @Constant(intValue = 256), require = 0)
    private int cam$populate256(int v) { return WorldHeightConfig.WORLD_HEIGHT; }

    
    
}
