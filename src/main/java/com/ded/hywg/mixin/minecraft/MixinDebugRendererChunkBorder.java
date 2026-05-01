package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.client.renderer.debug.DebugRendererChunkBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(DebugRendererChunkBorder.class)
public class MixinDebugRendererChunkBorder {
    
    @ModifyConstant(method = "render", constant = @Constant(doubleValue = 256.0D), require = 0)
    private double cam$modifyBorderHeightD(double original) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }
    
    @ModifyConstant(method = "render", constant = @Constant(intValue = 256), require = 0)
    private int cam$modifyBorderHeightI(int original) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }
}
