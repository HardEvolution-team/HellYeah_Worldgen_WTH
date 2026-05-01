package com.ded.hywg.mixin.alfheim.minecraft;

import dev.redstudio.alfheim.Alfheim;
import dev.redstudio.alfheim.api.IChunkLightingData;
import dev.redstudio.alfheim.api.ILightingEngineProvider;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.nbt.NBTTagShort;
import com.ded.hywg.WorldHeightConfig;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


/// @author Luna Mira Lage (Desoroxxx)
/// @author Angeline (@jellysquid)
/// @since 1.0
@Mixin(AnvilChunkLoader.class)
public abstract class AnvilChunkLoaderMixin {

	@Unique
	private static final String NEIGHBOR_LIGHT_CHECKS_KEY = "NeighborLightChecks";

	/// Injects into the head of saveChunk() to forcefully process all pending light updates. Fail-safe.
	///
	/// @author Angeline (@jellysquid)
	@Inject(method = "saveChunk", at = @At("HEAD"))
	private void onConstructed(final World world, final Chunk chunk, final CallbackInfo callbackInfo) {
		((ILightingEngineProvider) world).getAlfheim$lightingEngine().processLightUpdates();
	}

	/// Injects the deserialization logic for chunk data on load so we can extract whether or not we've populated light yet.
	///
	/// @author Angeline (@jellysquid)
	@Inject(method = "readChunkFromNBT", at = @At("RETURN"))
	private void onReadChunkFromNBT(final World world, final NBTTagCompound compound, final CallbackInfoReturnable<Chunk> callbackInfoReturnable) {
		final Chunk chunk = callbackInfoReturnable.getReturnValue();

		alfheim$readNeighborLightChecksFromNBT(chunk, compound);

		((IChunkLightingData) chunk).alfheim$setLightInitialized(compound.getBoolean("LightPopulated"));
	}

	/// Injects the serialization logic for chunk data on save, so we can store whether or not we've populated light yet.
	///
	/// @author Angeline (@jellysquid)
	@Inject(method = "writeChunkToNBT", at = @At("RETURN"))
	private void onWriteChunkToNBT(final Chunk chunk, final World world, final NBTTagCompound compound, final CallbackInfo callbackInfo) {
		alfheim$writeNeighborLightChecksToNBT(chunk, compound);

		compound.setBoolean("LightPopulated", ((IChunkLightingData) chunk).alfheim$isLightInitialized());
	}

	@Unique
	private static void alfheim$readNeighborLightChecksFromNBT(final Chunk chunk, final NBTTagCompound compound) {
		((IChunkLightingData) chunk).alfheim$initNeighborLightChecks();

		final long[][] neighborLightChecks = ((IChunkLightingData) chunk).alfheim$getNeighborLightChecks();

		for (int i = 0; i < Alfheim.FLAG_COUNT; ++i) {
			final String key = NEIGHBOR_LIGHT_CHECKS_KEY + "_" + i;
			if (compound.hasKey(key, 11)) { // 11 is IntArray
				int[] ints = compound.getIntArray(key);
				if (ints.length == WorldHeightConfig.BITMASK_LONGS * 2) {
					for (int j = 0; j < WorldHeightConfig.BITMASK_LONGS; j++) {
						neighborLightChecks[i][j] = ((long) ints[j * 2] << 32) | (ints[j * 2 + 1] & 0xFFFFFFFFL);
					}
				}
			}
		}
	}

	@Unique
	private static void alfheim$writeNeighborLightChecksToNBT(final Chunk chunk, final NBTTagCompound compound) {
		final long[][] neighborLightChecks = ((IChunkLightingData) chunk).alfheim$getNeighborLightChecks();

		if (neighborLightChecks == null)
			return;

		for (int i = 0; i < Alfheim.FLAG_COUNT; ++i) {
			long[] flags = neighborLightChecks[i];
			boolean hasFlags = false;
			for (long f : flags) {
				if (f != 0) {
					hasFlags = true;
					break;
				}
			}

			if (hasFlags) {
				int[] ints = new int[WorldHeightConfig.BITMASK_LONGS * 2];
				for (int j = 0; j < WorldHeightConfig.BITMASK_LONGS; j++) {
					ints[j * 2] = (int) (flags[j] >> 32);
					ints[j * 2 + 1] = (int) flags[j];
				}
				compound.setIntArray(NEIGHBOR_LIGHT_CHECKS_KEY + "_" + i, ints);
			}
		}
	}
}
