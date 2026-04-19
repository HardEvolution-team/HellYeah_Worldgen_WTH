package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.item.ItemBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;


@Mixin(ItemBlock.class)
public class MixinItemBlock {

    @ModifyConstant(
            method = "onItemUse",
            constant = @Constant(intValue = 255),
            require = 0
    )
    private int cam$onItemUse255(int v) {
        return WorldHeightConfig.WORLD_HEIGHT_M1;
    }
}
