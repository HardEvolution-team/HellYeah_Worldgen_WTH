package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = World.class, priority = 999)
public abstract class MixinWorld {

    
    @Overwrite
    public boolean isOutsideBuildHeight(BlockPos pos) {
        return pos.getY() < 0 || pos.getY() >= WorldHeightConfig.WORLD_HEIGHT;
    }

    
    @Overwrite
    public boolean isValid(BlockPos pos) {
        return pos.getX() >= -30000000 && pos.getZ() >= -30000000
                && pos.getX() < 30000000 && pos.getZ() < 30000000
                && pos.getY() >= 0 && pos.getY() < WorldHeightConfig.WORLD_HEIGHT;
    }

    @Overwrite
    public int getHeight() {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    
    @ModifyConstant(
            method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$sbs(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    


    


    
    @ModifyConstant(
            method = "getLightFor",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$lightFor(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    
    @ModifyConstant(
            method = "setLightFor",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$setLight(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    


    
    @ModifyConstant(
            method = "getChunksLowestHorizon",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$horizon(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    

    
    
    @ModifyConstant(
            method = "getRawLight",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$rawLight(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    
    
    

    
    

    
    
    
    @ModifyConstant(
            method = "isAreaLoaded(IIIIIIZ)Z",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$isAreaLoaded(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    
    @ModifyConstant(
            method = "markBlocksDirtyVertical",
            constant = @Constant(intValue = 256),
            require = 0
    )
    private int cam$markDirty(int v) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    
    
}
