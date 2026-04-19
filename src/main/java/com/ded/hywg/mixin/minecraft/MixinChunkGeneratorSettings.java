package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.world.gen.ChunkGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ChunkGeneratorSettings.Factory.class)
public class MixinChunkGeneratorSettings {

    @ModifyConstant(
            method = "<init>",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$factory256(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    @ModifyConstant(
            method = "setDefaults",
            constant = @Constant(intValue = 256),
            require = 0,
            remap = false
    )
    private int cam$defaults256(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }
}
