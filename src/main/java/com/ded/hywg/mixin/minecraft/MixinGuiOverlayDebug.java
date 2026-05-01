package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.client.gui.GuiOverlayDebug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = GuiOverlayDebug.class, priority = 999)
public class MixinGuiOverlayDebug {
    
    @ModifyConstant(method = "getDebugInfoRight", constant = @Constant(intValue = 256), require = 0)
    private int cam$modifyDebugHeightRight(int original) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    @ModifyConstant(method = {"call", "renderDebugInfoLeft", "getDebugInfoLeft", "func_175237_c", "func_181552_b"}, constant = @Constant(intValue = 256), require = 0)
    private int cam$modifyDebugHeightLeftI(int original) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    @ModifyConstant(method = {"call", "renderDebugInfoLeft", "getDebugInfoLeft", "func_175237_c", "func_181552_b"}, constant = @Constant(intValue = 255), require = 0)
    private int cam$modifyDebugHeightLeftI2(int original) {
        return WorldHeightConfig.WORLD_HEIGHT_M1;
    }

    @ModifyConstant(method = {"call", "renderDebugInfoLeft", "getDebugInfoLeft", "func_175237_c", "func_181552_b"}, constant = @Constant(doubleValue = 256.0D), require = 0)
    private double cam$modifyDebugHeightLeftD(double original) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }
}
