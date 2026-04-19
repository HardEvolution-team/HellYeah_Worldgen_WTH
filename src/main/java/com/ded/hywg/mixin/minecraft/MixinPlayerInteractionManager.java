package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.server.management.PlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(PlayerInteractionManager.class)
public class MixinPlayerInteractionManager {

    @ModifyConstant(
            method = "processRightClickBlock",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$rightClick256(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    @ModifyConstant(
            method = "onBlockClicked",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$blockClicked256(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }
}
