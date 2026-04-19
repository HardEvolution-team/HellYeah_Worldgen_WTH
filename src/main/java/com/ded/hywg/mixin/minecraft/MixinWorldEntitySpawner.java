package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.world.WorldEntitySpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(WorldEntitySpawner.class)
public class MixinWorldEntitySpawner {

    @ModifyConstant(
            method = "getRandomChunkPosition",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private static int cam$randomPos(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }
}
