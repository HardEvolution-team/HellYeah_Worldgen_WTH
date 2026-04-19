package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@SideOnly(Side.CLIENT)
@Mixin(ViewFrustum.class)
public abstract class MixinViewFrustum {

    // -----------------------------------------------------------------------
    // Set the vertical chunk count to 256 instead of 16.
    // Depths-Update uses @ModifyConstant here as well, returning their
    // EXTENDED_STORAGE_SECTIONS (20). We return our CHUNK_SECTIONS (256).
    // -----------------------------------------------------------------------
    @ModifyConstant(method = "setCountChunksXYZ", constant = @Constant(intValue = 16), require = 0)
    private int cam$modifyCountChunksY(int original) {
        return WorldHeightConfig.CHUNK_SECTIONS;
    }

}