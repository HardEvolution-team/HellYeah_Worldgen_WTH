package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.world.gen.MapGenCaves;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(MapGenCaves.class)
public class MixinMapGenCaves {

    @ModifyConstant(
            method = "addTunnel",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$tunnel256(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    @ModifyConstant(
            method = "addRoom",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$room256(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    @ModifyConstant(
            method = "recursiveGenerate",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$recursive256(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    @ModifyConstant(
            method = "addTunnel",
            constant = @Constant(intValue = 128),
            require = 0
    )
    private int cam$tunnel128(int v) {
        return WorldHeightConfig.WORLD_HEIGHT / 2;
    }

    @ModifyConstant(
            method = "addRoom",
            constant = @Constant(intValue = 128),
            require = 0
    )
    private int cam$room128(int v) {
        return WorldHeightConfig.WORLD_HEIGHT / 2;
    }
}
