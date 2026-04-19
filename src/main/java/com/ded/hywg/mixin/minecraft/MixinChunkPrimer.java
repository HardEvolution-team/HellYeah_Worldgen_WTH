package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.world.chunk.ChunkPrimer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkPrimer.class)
public class MixinChunkPrimer {

    @Shadow
    @Mutable
    @Final
    private char[] data;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void cam$init(CallbackInfo ci) {
        this.data = new char[WorldHeightConfig.PRIMER_SIZE];
    }

    
    @Overwrite
    private static int getBlockIndex(int x, int y, int z) {
        return (x * 16 + z) * WorldHeightConfig.WORLD_HEIGHT + y;
    }

    
    
}
