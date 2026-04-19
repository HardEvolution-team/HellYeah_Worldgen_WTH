package com.ded.hywg.mixin.minecraft;

import com.ded.hywg.WorldHeightConfig;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(Chunk.class)
public abstract class MixinChunk {

    @Shadow @Final private World world;
    @Shadow @Final private Map<BlockPos, TileEntity> tileEntities;
    @Shadow private boolean dirty;
    @Shadow @Final private int[] precipitationHeightMap;
    @Shadow @Final private int[] heightMap;
    @Shadow @Final @Mutable
    private ExtendedBlockStorage[] storageArrays;
    @Shadow @Final @Mutable private ClassInheritanceMultiMap<Entity>[] entityLists;
    @Shadow @Final public int x;
    @Shadow @Final public int z;

    @Shadow public abstract void generateHeightMap();
    @Shadow public abstract void generateSkylightMap();
    @Shadow public abstract void relightBlock(int x, int y, int z);
    @Shadow public abstract void propagateSkylightOcclusion(int x, int z);
    @Shadow public abstract TileEntity getTileEntity(BlockPos pos, Chunk.EnumCreateEntityType creationMode);
    @Shadow public abstract IBlockState getBlockState(int x, int y, int z);
    @Shadow public abstract int getLightFor(EnumSkyBlock type, BlockPos pos);
    @Shadow public abstract boolean canSeeSky(BlockPos pos);

    @Inject(method = "<init>(Lnet/minecraft/world/World;II)V", at = @At("RETURN"))
    private void cam$expandArrays(World worldIn, int x, int z, CallbackInfo ci) {
        this.storageArrays = new ExtendedBlockStorage[WorldHeightConfig.CHUNK_SECTIONS];
        this.entityLists = (ClassInheritanceMultiMap<Entity>[]) new ClassInheritanceMultiMap[WorldHeightConfig.CHUNK_SECTIONS];
        for (int i = 0; i < WorldHeightConfig.CHUNK_SECTIONS; ++i) {
            this.entityLists[i] = new ClassInheritanceMultiMap<>(Entity.class);
        }
    }

    @ModifyConstant(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/world/chunk/ChunkPrimer;II)V", constant = @Constant(intValue = 256))
    private int cam$modifyPrimerLoop(int original) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    

    @ModifyConstant(method = "isEmptyBetween", constant = @Constant(intValue = 256))
    private int cam$isEmptyBetween256(int original) {
        return WorldHeightConfig.WORLD_HEIGHT;
    }

    @ModifyConstant(method = "isEmptyBetween", constant = @Constant(intValue = 255))
    private int cam$isEmptyBetween255(int original) {
        return WorldHeightConfig.WORLD_HEIGHT_M1;
    }

    

    @Inject(method = "getBlockState(III)Lnet/minecraft/block/state/IBlockState;", at = @At("HEAD"), cancellable = true)
    private void cam$getBlockState(int x, int y, int z, CallbackInfoReturnable<IBlockState> cir) {
        if (y >= 0 && y < WorldHeightConfig.WORLD_HEIGHT) {
            int i = y >> 4;
            if (i < this.storageArrays.length) {
                ExtendedBlockStorage ebs = this.storageArrays[i];
                if (ebs != null) {
                    cir.setReturnValue(ebs.get(x & 15, y & 15, z & 15));
                    return;
                }
            }
        }
        cir.setReturnValue(Blocks.AIR.getDefaultState());
    }

    

    @Inject(method = "setBlockState", at = @At("HEAD"), cancellable = true)
    private void cam$setBlockState(BlockPos pos, IBlockState state, CallbackInfoReturnable<IBlockState> cir) {
        int x = pos.getX() & 15;
        int y = pos.getY();
        int z = pos.getZ() & 15;
        int l = z << 4 | x;

        if (y >= this.precipitationHeightMap[l] - 1) {
            this.precipitationHeightMap[l] = -999;
        }

        int i1 = this.heightMap[l];
        IBlockState iblockstate = this.getBlockState(x, y, z);

        if (iblockstate == state) {
            cir.setReturnValue(null);
        } else {
            Block block = state.getBlock();
            Block block1 = iblockstate.getBlock();
            int chunkY = y >> 4;

            if (chunkY < 0 || chunkY >= this.storageArrays.length) {
                cir.setReturnValue(null);
                return;
            }

            ExtendedBlockStorage ebs = this.storageArrays[chunkY];
            boolean flag = false;

            if (ebs == null) {
                if (block == Blocks.AIR) {
                    cir.setReturnValue(null);
                    return;
                }
                ebs = new ExtendedBlockStorage(chunkY << 4, this.world.provider.hasSkyLight());
                this.storageArrays[chunkY] = ebs;
                flag = y >= i1;
            }

            ebs.set(x, y & 15, z, state);

            if (block1 != block) {
                if (!this.world.isRemote) {
                    block1.breakBlock(this.world, pos, iblockstate);
                } else if (block1.hasTileEntity(iblockstate)) {
                    TileEntity te = this.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
                    if (te != null && te.shouldRefresh(this.world, pos, iblockstate, state)) {
                        this.world.removeTileEntity(pos);
                    }
                }
            }

            if (ebs.get(x, y & 15, z).getBlock() != block) {
                cir.setReturnValue(null);
            } else {
                if (flag) {
                    this.generateSkylightMap();
                } else {
                    int j1 = state.getLightOpacity(this.world, pos);
                    int k1 = iblockstate.getLightOpacity(this.world, pos);

                    if (j1 > 0) {
                        if (y >= i1) {
                            this.relightBlock(x, y + 1, z);
                        }
                    } else if (y == i1 - 1) {
                        this.relightBlock(x, y, z);
                    }

                    if (j1 != k1 && (j1 < k1 || this.getLightFor(EnumSkyBlock.SKY, pos) > 0 || this.getLightFor(EnumSkyBlock.BLOCK, pos) > 0)) {
                        this.propagateSkylightOcclusion(x, z);
                    }
                }

                if (block.hasTileEntity(state)) {
                    TileEntity tileentity = this.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
                    if (tileentity == null) {
                        tileentity = block.createTileEntity(this.world, state);
                        this.world.setTileEntity(pos, tileentity);
                    }
                    if (tileentity != null) {
                        tileentity.updateContainingBlockInfo();
                    }
                }

                this.dirty = true;
                cir.setReturnValue(iblockstate);
            }
        }
    }

    

    @Inject(method = "getLightFor", at = @At("HEAD"), cancellable = true)
    private void cam$getLightFor(EnumSkyBlock type, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        int x = pos.getX() & 15;
        int y = pos.getY();
        int z = pos.getZ() & 15;
        int chunkY = y >> 4;

        if (chunkY < 0 || chunkY >= this.storageArrays.length) {
            cir.setReturnValue(type.defaultLightValue);
            return;
        }

        ExtendedBlockStorage ebs = this.storageArrays[chunkY];
        if (ebs == null) {
            cir.setReturnValue(this.canSeeSky(pos) ? type.defaultLightValue : 0);
        } else if (type == EnumSkyBlock.SKY) {
            cir.setReturnValue(!this.world.provider.hasSkyLight() ? 0 : ebs.getSkyLight().get(x, y & 15, z));
        } else {
            cir.setReturnValue(type == EnumSkyBlock.BLOCK ? ebs.getBlockLight().get(x, y & 15, z) : type.defaultLightValue);
        }
    }


    

    @Inject(method = "setLightFor", at = @At("HEAD"), cancellable = true)
    private void cam$setLightFor(EnumSkyBlock type, BlockPos pos, int value, CallbackInfo ci) {
        int x = pos.getX() & 15;
        int y = pos.getY();
        int z = pos.getZ() & 15;
        int chunkY = y >> 4;

        if (chunkY >= 0 && chunkY < this.storageArrays.length) {
            ExtendedBlockStorage ebs = this.storageArrays[chunkY];
            if (ebs == null) {
                ebs = new ExtendedBlockStorage(chunkY << 4, this.world.provider.hasSkyLight());
                this.storageArrays[chunkY] = ebs;
                this.generateHeightMap();
            }

            this.dirty = true;
            if (type == EnumSkyBlock.SKY) {
                if (this.world.provider.hasSkyLight()) {
                    ebs.getSkyLight().set(x, y & 15, z, value);
                }
            } else if (type == EnumSkyBlock.BLOCK) {
                ebs.getBlockLight().set(x, y & 15, z, value);
            }
            ci.cancel();
        }
    }
}

