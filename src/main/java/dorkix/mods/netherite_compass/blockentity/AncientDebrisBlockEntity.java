package dorkix.mods.netherite_compass.blockentity;

import dorkix.mods.NetheriteCompassMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

public class AncientDebrisBlockEntity extends BlockEntity {

    public AncientDebrisBlockEntity(BlockPos pos, BlockState state) {
        super(NetheriteCompassMod.ANCIENT_DEBRIS_BLOCK_ENTITY, pos, state);
    }

}
