package com.ded.hywg.mixin.minecraft;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Biome.class)
public class MixinBiome {

    /**
     * Clamps the Y coordinate used for temperature calculations in Biomes.
     * In vanilla 1.12.2, temperature drops linearly with height above Y=64.
     * At extreme heights (like 2048+), this causes the temperature to drop below 0,
     * making grass and foliage appear bluish/grey (the "cold" color).
     * By clamping the Y value to 255, we ensure colors remain within vanilla-like ranges.
     */
    @Redirect(
        method = "getTemperature(Lnet/minecraft/util/math/BlockPos;)F",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;getY()I")
    )
    private int cam$clampYForTemperature(BlockPos pos) {
        return Math.min(pos.getY(), 255);
    }
}
