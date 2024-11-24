package ARMachines.crystallizer;

import ARLib.multiblockCore.BlockMultiblockMaster;
import ARMachines.lathe.EntityLathe;
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

import static ARMachines.MultiblockRegistry.ENTITY_CRYSTALLIZER;
import static ARMachines.MultiblockRegistry.ENTITY_LATHE;

public class BlockCrystallizer extends BlockMultiblockMaster {

    public BlockCrystallizer(Properties properties) {super(properties);}

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_CRYSTALLIZER.get().create(blockPos,blockState);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityCrystallizer::tick;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (world.getBlockEntity(pos) instanceof EntityCrystallizer e) {
            if (world.isClientSide && state.getValue(STATE_MULTIBLOCK_FORMED)) {
                e.openGui();
            }
        }
        return super.useWithoutItem(state, world, pos, player, hitResult);
    }
}
