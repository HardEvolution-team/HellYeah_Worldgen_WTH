package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.world.gen.MapGenRavine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MapGenRavine.class)
public class MixinMapGenRavine {

    
    @Shadow
    @Final
    @Mutable
    private float[] rs;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void cam$resizeRsArray(CallbackInfo ci) {
        this.rs = new float[WorldHeightConfig.WORLD_HEIGHT];
    }

    
    @ModifyConstant(
            method = "addTunnel",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$tunnel256(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    
    @ModifyConstant(
            method = "addTunnel",
            constant = @Constant(intValue = 248),
            require = 0
    )
    private int cam$tunnel248(int v) {
        return WorldHeightConfig.WORLD_HEIGHT - 8;
    }

    @ModifyConstant(
            method = "recursiveGenerate",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$recursive256(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }
}
