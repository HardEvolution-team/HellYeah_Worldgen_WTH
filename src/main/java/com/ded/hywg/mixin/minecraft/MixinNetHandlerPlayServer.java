package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.network.NetHandlerPlayServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(NetHandlerPlayServer.class)
public class MixinNetHandlerPlayServer {

    
    @ModifyConstant(
            method = "*",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$netHandler256(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }
}
