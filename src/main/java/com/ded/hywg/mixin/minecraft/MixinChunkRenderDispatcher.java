package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ChunkRenderDispatcher.class)
public class MixinChunkRenderDispatcher {

    @ModifyConstant(
            method = "*",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$dispatch256(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }
}
