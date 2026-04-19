package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import com.ded.hywg.access.IExtendedChunkData;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketChunkData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

    @Inject(
            method = "handleChunkData",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/Chunk;read(Lnet/minecraft/network/PacketBuffer;IZ)V"
            )
    )
    private void cam$setMask(SPacketChunkData packet, CallbackInfo ci) {
        if (packet instanceof IExtendedChunkData) {
            WorldHeightConfig.SECTION_MASK_TL.set(
                    ((IExtendedChunkData) packet).cam$getSectionMask()
            );
        }
    }

    
    @Inject(
            method = "handleChunkData",
            at = @At("RETURN")
    )
    private void cam$cleanupMask(SPacketChunkData packet, CallbackInfo ci) {
        WorldHeightConfig.SECTION_MASK_TL.remove();
    }

    
    @ModifyConstant(
            method = "handleChunkData",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$markRenderUpdate256(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }
}
