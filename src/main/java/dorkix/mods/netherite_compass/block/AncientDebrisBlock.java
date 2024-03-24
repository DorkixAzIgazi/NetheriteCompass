package dorkix.mods.netherite_compass.block;

import com.mojang.serialization.MapCodec;

import dorkix.mods.netherite_compass.blockentity.AncientDebrisBlockEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

public class AncientDebrisBlock extends BlockWithEntity {

    public AncientDebrisBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new AncientDebrisBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        throw new UnsupportedOperationException("Unimplemented method 'getCodec'");
    }

}
