package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;


@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {

    
    @Overwrite
    public int getBuildLimit() {
        return WorldHeightConfig.WORLD_HEIGHT;
    }
}
