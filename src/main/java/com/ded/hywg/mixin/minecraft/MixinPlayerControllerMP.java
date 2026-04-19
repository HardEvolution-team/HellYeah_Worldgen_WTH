package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;


@Mixin(PlayerControllerMP.class)
public class MixinPlayerControllerMP {

    @ModifyConstant(
            method = "processRightClickBlock",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$rightClick256(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    @ModifyConstant(
            method = "onPlayerDestroyBlock",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$destroyBlock256(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }
}
