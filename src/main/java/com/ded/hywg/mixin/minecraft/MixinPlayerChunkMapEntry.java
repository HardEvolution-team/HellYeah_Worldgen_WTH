package com.ded.hywg.mixin.minecraft;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(PlayerChunkMapEntry.class)
public abstract class MixinPlayerChunkMapEntry {

    @Shadow private boolean sentToPlayers;
    @Shadow private int changes;
    @Shadow private int changedSectionFilter;
    @Shadow @Final private PlayerChunkMap playerChunkMap;
    @Shadow @Final private ChunkPos pos;
    @Shadow @Nullable private Chunk chunk;
    @Shadow @Final private List<EntityPlayerMP> players;

    @Shadow public abstract void sendPacket(Packet<?> packetIn);
    @Shadow protected abstract void sendBlockEntity(@Nullable TileEntity p_187273_1_);

    @Unique
    private int[] cam$changedBlocks = new int[64];

    
    @Inject(method = "blockChanged", at = @At("HEAD"), cancellable = true)
    private void cam$blockChanged(int x, int y, int z, CallbackInfo ci) {
        ci.cancel();

        if (this.sentToPlayers) {
            if (this.changes == 0) {
                this.playerChunkMap.entryChanged((PlayerChunkMapEntry) (Object) this);
            }

            
            
            
            
            
            
            int sectionY = y >> 4;
            if (sectionY >= 0 && sectionY < 32) {
                this.changedSectionFilter |= 1 << sectionY;
            } else {
                
                
                this.changedSectionFilter |= 0x80000000; 
            }

            
            int packed = (x << 28) | (z << 24) | (y & 0xFFFFFF);

            for (int i = 0; i < this.changes; ++i) {
                if (this.cam$changedBlocks[i] == packed) {
                    return;
                }
            }

            if (this.changes == this.cam$changedBlocks.length) {
                this.cam$changedBlocks = java.util.Arrays.copyOf(this.cam$changedBlocks, this.cam$changedBlocks.length << 1);
            }

            this.cam$changedBlocks[this.changes++] = packed;
        }
    }

    
    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void cam$update(CallbackInfo ci) {
        ci.cancel();

        if (this.sentToPlayers && this.chunk != null) {
            if (this.changes != 0) {
                if (this.changes == 1) {
                    int i = (this.cam$changedBlocks[0] >> 28 & 15) + this.pos.x * 16;
                    int k = (this.cam$changedBlocks[0] >> 24 & 15) + this.pos.z * 16;
                    int j = this.cam$changedBlocks[0] & 0xFFFFFF;
                    BlockPos blockpos = new BlockPos(i, j, k);
                    this.sendPacket(new SPacketBlockChange(this.playerChunkMap.getWorldServer(), blockpos));
                    net.minecraft.block.state.IBlockState state = this.playerChunkMap.getWorldServer().getBlockState(blockpos);

                    if (state.getBlock().hasTileEntity(state)) {
                        this.sendBlockEntity(this.playerChunkMap.getWorldServer().getTileEntity(blockpos));
                    }
                } else if (this.changes >= net.minecraftforge.common.ForgeModContainer.clumpingThreshold) {
                    
                    
                    
                    
                    this.sendPacket(new SPacketChunkData(this.chunk, this.changedSectionFilter));
                } else {
                    for (int l = 0; l < this.changes; ++l) {
                        int i1 = (this.cam$changedBlocks[l] >> 28 & 15) + this.pos.x * 16;
                        int k1 = (this.cam$changedBlocks[l] >> 24 & 15) + this.pos.z * 16;
                        int j1 = this.cam$changedBlocks[l] & 0xFFFFFF;
                        BlockPos blockpos1 = new BlockPos(i1, j1, k1);
                        this.sendPacket(new SPacketBlockChange(this.playerChunkMap.getWorldServer(), blockpos1));
                        net.minecraft.block.state.IBlockState state = this.playerChunkMap.getWorldServer().getBlockState(blockpos1);

                        if (state.getBlock().hasTileEntity(state)) {
                            this.sendBlockEntity(this.playerChunkMap.getWorldServer().getTileEntity(blockpos1));
                        }
                    }
                }

                this.changes = 0;
                this.changedSectionFilter = 0;
            }
        }
    }
}
