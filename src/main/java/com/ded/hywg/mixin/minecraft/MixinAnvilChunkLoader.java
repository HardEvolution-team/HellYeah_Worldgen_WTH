package com.ded.hywg.mixin.minecraft;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(AnvilChunkLoader.class)
public abstract class MixinAnvilChunkLoader {

    
    
    
    
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
}
