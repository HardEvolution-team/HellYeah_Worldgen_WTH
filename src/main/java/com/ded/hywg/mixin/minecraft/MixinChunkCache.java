package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.world.ChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ChunkCache.class)
public class MixinChunkCache {

    @ModifyConstant(
            method = "*",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$modifyHeight256(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    @ModifyConstant(
            method = "*",
            constant = @Constant(intValue = 255),
            require = 0
    )
    private int cam$modifyHeight255(int v) {
        return WorldHeightConfig.WORLD_HEIGHT_M1;
    }
}
