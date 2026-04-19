package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.client.renderer.chunk.RenderChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;


@Mixin(RenderChunk.class)
public class MixinRenderChunk {

    @ModifyConstant(
            method = "rebuildChunk",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$rebuildChunk256(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    @ModifyConstant(
            method = "rebuildChunk",
            constant = @Constant(intValue = 255),
            require = 0
    )
    private int cam$rebuildChunk255(int v) {
        return WorldHeightConfig.WORLD_HEIGHT_M1;
    }

    @ModifyConstant(
            method = "setPosition",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$setPosition256(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }
}
