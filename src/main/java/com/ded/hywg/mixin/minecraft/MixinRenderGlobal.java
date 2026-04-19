package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {



    @Redirect(
            method = "renderEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;getPosition()Lnet/minecraft/util/math/BlockPos;")
    )
    private BlockPos cam$redirectRenderChunkPosForEntityArray(RenderChunk renderChunk) {
        BlockPos pos = renderChunk.getPosition();
        
        if (pos.getY() < 0 || pos.getY() >= WorldHeightConfig.WORLD_HEIGHT) {
            
            
            return new BlockPos(pos.getX(), 0, pos.getZ());
        }
        return pos;
    }

    @Shadow
    private int renderDistanceChunks;

    @Shadow
    private ViewFrustum viewFrustum;

    @Shadow
    private net.minecraft.client.multiplayer.WorldClient world;

    @Inject(method = "getRenderChunkOffset", at = @At("HEAD"), cancellable = true)
    private void cam$getRenderChunkOffsetInject(BlockPos playerPos, RenderChunk renderChunkBase, net.minecraft.util.EnumFacing facing, CallbackInfoReturnable<RenderChunk> cir) {
        BlockPos blockpos = renderChunkBase.getBlockPosOffset16(facing);

        if (MathHelper.abs(playerPos.getX() - blockpos.getX()) > this.renderDistanceChunks * 16) {
            cir.setReturnValue(null);
        } else if (blockpos.getY() < 0 || blockpos.getY() >= WorldHeightConfig.WORLD_HEIGHT) {
            cir.setReturnValue(null);
        } else if (MathHelper.abs(playerPos.getZ() - blockpos.getZ()) > this.renderDistanceChunks * 16) {
            cir.setReturnValue(null);
        } else {
            
            if (facing == net.minecraft.util.EnumFacing.UP) {
                net.minecraft.world.chunk.Chunk chunk = this.world.getChunk(blockpos.getX() >> 4, blockpos.getZ() >> 4);
                if (renderChunkBase.getPosition().getY() > chunk.getTopFilledSegment() + 16) {
                    cir.setReturnValue(null);
                    return;
                }
            }
            cir.setReturnValue(((IViewFrustumAccessor) this.viewFrustum).cam$getRenderChunk(blockpos));
        }
    }

    @ModifyConstant(
            method = "setupTerrain",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$setupTerrain(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    @ModifyConstant(
            method = "setupTerrain",
            constant = @Constant(doubleValue = 256.0),
            require = 0
    )
    private double cam$setupTerrainD(double v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    @ModifyConstant(
            method = "setupTerrain",
            constant = @Constant(intValue = 255),
            require = 0
    )
    private int cam$setupTerrainM1(int v) {
        return WorldHeightConfig.WORLD_HEIGHT_M1;
    }

    @ModifyConstant(
            method = "renderWorldBorder",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$renderWorldBorder(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    @ModifyConstant(
            method = "renderWorldBorder",
            constant = @Constant(doubleValue = 256.0),
            require = 0
    )
    private double cam$renderWorldBorderD(double v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    @ModifyConstant(
            method = "markBlockRangeForRenderUpdate(IIIIII)V",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$markBlockRangeForRenderUpdate256(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    @ModifyConstant(
            method = "markBlockRangeForRenderUpdate",
            constant = @Constant(intValue = 255),
            require = 0
    )
    private int cam$markBlockRangeForRenderUpdate255(int v) {
        return WorldHeightConfig.WORLD_HEIGHT_M1;
    }

    @ModifyConstant(
            method = "loadRenderers",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$loadRenderers(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }
}
