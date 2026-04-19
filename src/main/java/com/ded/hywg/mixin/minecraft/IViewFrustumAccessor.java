package com.ded.hywg.mixin.minecraft;

import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ViewFrustum.class)
public interface IViewFrustumAccessor {

    @Invoker("getRenderChunk")
    RenderChunk cam$getRenderChunk(BlockPos pos);
}
