package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Chunk.class, priority = 1001)
public abstract class MixinChunkRead {

    @Shadow
    @Final
    private ExtendedBlockStorage[] storageArrays;

    @Shadow
    @Final
    private World world;

    @Shadow
    @Final
    private boolean[] updateSkylightColumns;

    @Shadow
    private boolean isTerrainPopulated;

    @Shadow
    private boolean isLightPopulated;

    @Shadow
    private byte[] blockBiomeArray;

    @Shadow
    public abstract void generateHeightMap();

    @Shadow
    public abstract void setModified(boolean modified);

    @Shadow
    public abstract java.util.Map<net.minecraft.util.math.BlockPos, TileEntity> getTileEntityMap();

    
    @Overwrite
    public void read(PacketBuffer buf, int availableSections, boolean fullChunk) {
        long[] mask = WorldHeightConfig.SECTION_MASK_TL.get();
        boolean hasSkyLight = !this.world.provider.isNether();

        for (int i = 0; i < this.storageArrays.length; i++) {
            boolean available;

            if (mask != null) {
                available = (mask[i >> 6] & (1L << (i & 63))) != 0;
            } else {
                
                available = i < 32 && (availableSections & (1 << i)) != 0;
            }

            if (available) {
                if (this.storageArrays[i] == null) {
                    this.storageArrays[i] = new ExtendedBlockStorage(i << 4, hasSkyLight);
                }
                this.storageArrays[i].getData().read(buf);
                buf.readBytes(this.storageArrays[i].getBlockLight().getData());
                if (hasSkyLight) {
                    buf.readBytes(this.storageArrays[i].getSkyLight().getData());
                }
                this.storageArrays[i].recalculateRefCounts();
            } else if (fullChunk) {
                this.storageArrays[i] = null;
            }
        }

        if (fullChunk) {
            buf.readBytes(this.blockBiomeArray);
        }

        this.generateHeightMap();

        for (int i = 0; i < this.updateSkylightColumns.length; i++) {
            this.updateSkylightColumns[i] = true;
        }

        this.isTerrainPopulated = true;
        this.isLightPopulated = true;
        this.setModified(true);

        for (TileEntity te : this.getTileEntityMap().values()) {
            te.updateContainingBlockInfo();
        }

        WorldHeightConfig.SECTION_MASK_TL.remove();
    }
}
