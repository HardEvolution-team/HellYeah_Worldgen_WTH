package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.world.WorldProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(WorldProvider.class)
public class MixinWorldProvider {

    
    @Overwrite(remap = false)
    public int getHeight() {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    
    @Overwrite(remap = false)
    public int getActualHeight() {
        return WorldHeightConfig.WORLD_HEIGHT;
    }
}
