package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AnvilChunkLoader.class)
public abstract class MixinAnvilChunkLoader {

    // -----------------------------------------------------------------------
    // WRITE SIDE: store section Y as Short instead of Byte (byte overflows at 128+)
    // -----------------------------------------------------------------------

    @Redirect(
            method = "writeChunkToNBT",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/NBTTagCompound;setByte(Ljava/lang/String;B)V")
    )
    private void cam$writeYAsShort(NBTTagCompound nbt, String key, byte value) {
        if ("Y".equals(key)) {
            nbt.setShort(key, (short)(value & 0xFF));
        } else {
            nbt.setByte(key, value);
        }
    }

    // -----------------------------------------------------------------------
    // READ SIDE: expand the local EBS array from 16 to CHUNK_SECTIONS (256)
    // -----------------------------------------------------------------------

    @ModifyConstant(
            method = "func_75823_a",
            constant = @Constant(intValue = 16),
            require = 0
    )
    private int cam$expandReadArray(int original) {
        return WorldHeightConfig.CHUNK_SECTIONS;
    }

    // -----------------------------------------------------------------------
    // READ SIDE: robust parsing using ThreadLocals to prevent byte overflow
    // and array index out of bounds while preserving all section data.
    // -----------------------------------------------------------------------

    @Unique
    private static final ThreadLocal<ExtendedBlockStorage[]> cam$storages = new ThreadLocal<>();
    @Unique
    private static final ThreadLocal<Integer> cam$pendingY = new ThreadLocal<>();

    @org.spongepowered.asm.mixin.injection.Inject(
            method = "func_75823_a",
            at = @At("HEAD")
    )
    private void cam$initStorages(net.minecraft.world.World worldIn, NBTTagCompound compound, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.world.chunk.Chunk> cir) {
        cam$storages.set(new ExtendedBlockStorage[WorldHeightConfig.CHUNK_SECTIONS]);
    }

    @Redirect(
            method = "func_75823_a",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/NBTTagCompound;getByte(Ljava/lang/String;)B")
    )
    private byte cam$readYAsUnsigned(NBTTagCompound nbt, String key) {
        if ("Y".equals(key)) {
            int fullY = nbt.getShort(key) & 0xFFFF;
            cam$pendingY.set(fullY);
            // We ALWAYS return 0 to Vanilla. This prevents ANY byte-to-negative-int 
            // sign extension crashes. Vanilla will assign the EBS to vanillaArray[0].
            return 0;
        }
        return nbt.getByte(key);
    }

    @Redirect(
            method = "func_75823_a",
            at = @At(value = "NEW", target = "net/minecraft/world/chunk/storage/ExtendedBlockStorage")
    )
    private ExtendedBlockStorage cam$createEBSAtCorrectY(int yOffset, boolean hasSky) {
        Integer pending = cam$pendingY.get();
        int realY = (pending != null) ? pending : (yOffset >> 4);
        return new ExtendedBlockStorage(realY << 4, hasSky);
    }

    @Redirect(
            method = "func_75823_a",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;recalculateRefCounts()V")
    )
    private void cam$captureEBS(ExtendedBlockStorage ebs) {
        ebs.recalculateRefCounts();
        Integer y = cam$pendingY.get();
        if (y != null && y >= 0 && y < WorldHeightConfig.CHUNK_SECTIONS) {
            ExtendedBlockStorage[] arr = cam$storages.get();
            if (arr != null) {
                arr[y] = ebs;
            }
        }
    }

    @Redirect(
            method = "func_75823_a",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;setStorageArrays([Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;)V")
    )
    private void cam$applyStorages(net.minecraft.world.chunk.Chunk chunk, ExtendedBlockStorage[] vanillaArray) {
        // Vanilla is trying to apply its broken array (where everything was shoved into index 0).
        // We give it our perfectly assembled array instead!
        ExtendedBlockStorage[] ourArray = cam$storages.get();
        chunk.setStorageArrays(ourArray != null ? ourArray : vanillaArray);
    }

    @org.spongepowered.asm.mixin.injection.Inject(
            method = "func_75823_a",
            at = @At("RETURN")
    )
    private void cam$cleanup(net.minecraft.world.World worldIn, NBTTagCompound compound, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.world.chunk.Chunk> cir) {
        cam$storages.remove();
        cam$pendingY.remove();
    }
}
