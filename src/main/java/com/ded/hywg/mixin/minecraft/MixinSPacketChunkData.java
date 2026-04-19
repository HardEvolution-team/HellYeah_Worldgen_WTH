package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import com.ded.hywg.access.IExtendedChunkData;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;

@Mixin(SPacketChunkData.class)
public abstract class MixinSPacketChunkData implements IExtendedChunkData {

    @Shadow
    private int chunkX;
    @Shadow
    private int chunkZ;
    @Shadow
    @Mutable
    private int availableSections;
    @Shadow
    @Mutable
    private byte[] buffer;
    @Shadow
    private boolean fullChunk;

    @Unique
    private long[] cam$mask;

    

    @Override
    public long[] cam$getSectionMask() {
        if (cam$mask == null) {
            cam$mask = new long[WorldHeightConfig.BITMASK_LONGS];
        }
        return cam$mask;
    }

    @Override
    public boolean cam$hasSection(int i) {
        if (cam$mask == null || i < 0 || i >= WorldHeightConfig.CHUNK_SECTIONS) return false;
        return (cam$mask[i >> 6] & (1L << (i & 63))) != 0;
    }

    

    @Inject(method = "<init>(Lnet/minecraft/world/chunk/Chunk;I)V", at = @At("RETURN"))
    private void cam$onInit(Chunk chunk, int filter, CallbackInfo ci) {
        boolean sky = !chunk.getWorld().provider.isNether();
        ExtendedBlockStorage[] ebs = chunk.getBlockStorageArray();

        
        cam$mask = new long[WorldHeightConfig.BITMASK_LONGS];
        for (int i = 0; i < ebs.length; i++) {
            if (ebs[i] != null && !ebs[i].isEmpty()) {
                cam$mask[i >> 6] |= (1L << (i & 63));
            }
        }

        
        int size = 0;
        for (int i = 0; i < ebs.length; i++) {
            if (cam$hasSection(i)) {
                size += ebs[i].getData().getSerializedSize();
                size += ebs[i].getBlockLight().getData().length;
                if (sky && ebs[i].getSkyLight() != null) {
                    size += ebs[i].getSkyLight().getData().length;
                }
            }
        }
        if (fullChunk) {
            size += 256; 
        }

        
        this.buffer = new byte[size];
        PacketBuffer pb = new PacketBuffer(Unpooled.wrappedBuffer(this.buffer));
        pb.writerIndex(0);

        for (int i = 0; i < ebs.length; i++) {
            if (cam$hasSection(i)) {
                ebs[i].getData().write(pb);
                pb.writeBytes(ebs[i].getBlockLight().getData());
                if (sky && ebs[i].getSkyLight() != null) {
                    pb.writeBytes(ebs[i].getSkyLight().getData());
                }
            }
        }

        if (fullChunk) {
            pb.writeBytes(chunk.getBiomeArray());
        }

        this.availableSections = (int) cam$mask[0];
    }

    

    @Inject(method = "writePacketData", at = @At("HEAD"), cancellable = true)
    private void cam$write(PacketBuffer buf, CallbackInfo ci) {
        buf.writeInt(chunkX);
        buf.writeInt(chunkZ);
        buf.writeBoolean(fullChunk);
        long[] m = cam$getSectionMask();
        for (long l : m) {
            buf.writeLong(l);
        }
        buf.writeVarInt(buffer.length);
        buf.writeBytes(buffer);
        ci.cancel();
    }

    

    @Inject(method = "readPacketData", at = @At("HEAD"), cancellable = true)
    private void cam$read(PacketBuffer buf, CallbackInfo ci) throws IOException {
        chunkX = buf.readInt();
        chunkZ = buf.readInt();
        fullChunk = buf.readBoolean();
        cam$mask = new long[WorldHeightConfig.BITMASK_LONGS];
        for (int i = 0; i < WorldHeightConfig.BITMASK_LONGS; i++) {
            cam$mask[i] = buf.readLong();
        }
        availableSections = (int) cam$mask[0];
        int sz = buf.readVarInt();
        if (sz > 0x4000000) {
            throw new IOException("Chunk data too large: " + sz);
        }
        buffer = new byte[sz];
        buf.readBytes(buffer);
        ci.cancel();
    }

    

    @Inject(method = "extractChunkData", at = @At("HEAD"), cancellable = true)
    private void cam$extract(PacketBuffer b, Chunk c, boolean s, int f, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(availableSections);
    }

    @Inject(method = "calculateChunkSize", at = @At("HEAD"), cancellable = true)
    private void cam$calcSize(Chunk c, boolean s, int f, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(buffer != null ? buffer.length : 0);
    }
}
