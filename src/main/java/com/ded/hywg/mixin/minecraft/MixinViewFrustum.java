package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(ViewFrustum.class)
public abstract class MixinViewFrustum {

    @Shadow protected int countChunksY;
    @Shadow protected net.minecraft.world.World world;

    @Inject(method = "setCountChunksXYZ", at = @At("RETURN"))
    private void cam$forceCountChunksY(int renderDistanceChunks, CallbackInfo ci) {
        this.countChunksY = WorldHeightConfig.CHUNK_SECTIONS;
    }

    @Inject(method = "markBlocksForUpdate", at = @At("HEAD"), cancellable = true)
    private void cam$markBlocksForUpdate(int x1, int y1, int z1, int x2, int y2, int z2, boolean updateImmediate, CallbackInfo ci) {
        int i = x1 >> 4;
        int j = y1 >> 4;
        int k = z1 >> 4;
        int l = x2 >> 4;
        int i1 = y2 >> 4;
        int j1 = z2 >> 4;

        for (int k1 = i; k1 <= l; ++k1) {
            for (int i2 = k; i2 <= j1; ++i2) {
                
                net.minecraft.world.chunk.Chunk chunk = this.world.getChunk(k1, i2);
                int top = chunk.getTopFilledSegment() + 16;
                
                for (int l1 = j; l1 <= i1; ++l1) {
                    
                    if (l1 * 16 <= top) {
                        RenderChunk renderchunk = ((IViewFrustumAccessor)this).cam$getRenderChunk(new BlockPos(k1 << 4, l1 << 4, i2 << 4));
                        if (renderchunk != null) {
                            renderchunk.setNeedsUpdate(updateImmediate);
                        }
                    }
                }
            }
        }
        ci.cancel();
    }
}