package ARMachines.lathe;


import ARLib.multiblockCore.BlockMultiblockMaster;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

import static ARMachines.MultiblockRegistry.ENTITY_LATHE;

public class BlockLathe extends BlockMultiblockMaster {

    public BlockLathe(Properties properties) {super(properties);}

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_LATHE.get().create(blockPos,blockState);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityLathe::tick;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (world.getBlockEntity(pos) instanceof EntityLathe e &&
                state.getBlock() instanceof BlockLathe b) {
            if (world.isClientSide && state.getValue(STATE_MULTIBLOCK_FORMED)) {
                e.openGui();
            }
        }
        return super.useWithoutItem(state, world, pos, player, hitResult);
    }



}
