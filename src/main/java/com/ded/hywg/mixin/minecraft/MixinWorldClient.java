package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(WorldClient.class)
public class MixinWorldClient {

    @ModifyConstant(
            method = "invalidateRegionAndSetBlock",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$invalidate256(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }
}
