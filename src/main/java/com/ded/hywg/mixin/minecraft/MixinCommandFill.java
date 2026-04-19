package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.command.CommandFill;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(CommandFill.class)
public class MixinCommandFill {

    @ModifyConstant(
            method = "execute",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$execute256(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }
}
