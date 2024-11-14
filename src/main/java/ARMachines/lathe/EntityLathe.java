package ARMachines.lathe;


import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.IGuiHandler;
import ARLib.multiblockCore.EntityMultiblockMaster;
import ARLib.multiblockCore.MultiblockRecipeManager;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.MachineRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

import static ARLib.ARLibRegistry.BLOCK_MOTOR;
import static ARLib.ARLibRegistry.BLOCK_STRUCTURE;
import static ARMachines.MultiblockRegistry.BLOCK_LATHE;
import static ARMachines.MultiblockRegistry.ENTITY_LATHE;

public class EntityLathe extends EntityMultiblockMaster {


    static List<MachineRecipe> recipes = new ArrayList<>();

    public static void addRecipe(MachineRecipe recipe) {
        recipes.add(recipe);
    }

    IGuiHandler guiHandler;
    public MultiblockRecipeManager<EntityLathe> recipeManager = new MultiblockRecipeManager<>(this);
    boolean isRunning;
    int client_recipeMaxTime;
    int client_recipeProgress;

    public EntityLathe(BlockPos pos, BlockState state) {
        super(ENTITY_LATHE.get(), pos, state);
        guiHandler = new GuiHandlerBlockEntity(this);
        //this.alwaysOpenMasterGui = true;
        recipeManager.recipes = EntityLathe.recipes;
    }
    @Override
    public void onLoad(){
        super.onLoad();
        if(level.isClientSide) {
            CompoundTag info = new CompoundTag();
            info.putByte("client_onload", (byte) 0);
            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, info));
        }
    }

    void setIsRunning(boolean isrunning) {
        if (this.isRunning != isrunning) {
            this.isRunning = isrunning;
            if(this.isRunning) {
                CompoundTag info = new CompoundTag();
                info.putBoolean("isRunning", this.isRunning);
                info.putInt("recipeProgress", recipeManager.progress);
                info.putInt("recipeTime", recipeManager.currentRecipe.ticksRequired);
                PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, info));
            }
        }
    }

    @Override
    public void setupCharmappings() {
        super.setupCharmappings();
        List<Block> c = new ArrayList<>();
        c.add(BLOCK_LATHE.get());
        setMapping('c', c);
    }

    public static final Object[][][] structure = {
            {{'c', BLOCK_MOTOR.get(), Blocks.AIR, 'I'}},
            {{'P', BLOCK_STRUCTURE.get(), BLOCK_STRUCTURE.get(), 'O'}},
    };

    @Override
    public Object[][][] getStructure() {
        return structure;
    }


    public void openGui() {
        guiHandler.openGui(176, 126);
    }

    @Override
    public void readServer(CompoundTag tag) {
        guiHandler.readServer(tag);
        super.readServer(tag);
        if(tag.contains("client_onload")){
            CompoundTag info = new CompoundTag();
            info.putBoolean("isRunning", this.isRunning);
            if(this.isRunning) {
                info.putInt("recipeProgress", recipeManager.progress);
                info.putInt("recipeTime", recipeManager.currentRecipe.ticksRequired);
            }
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, info));
        }
    }

    @Override
    public void readClient(CompoundTag tag) {
        guiHandler.readClient(tag);
        if (tag.contains("openGui")) {
            openGui();
        }
        if (tag.contains("isRunning")) {
            this.isRunning = tag.getBoolean("isRunning");
        }
        if (tag.contains("recipeProgress")) {
            client_recipeProgress = tag.getInt("recipeProgress");
        }
        if (tag.contains("recipeTime")) {
            client_recipeMaxTime = tag.getInt("recipeTime");
        }
        super.readClient(tag);
    }

    public static <x extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, x t) {
        EntityLathe t1 = (EntityLathe) t;
        if (!level.isClientSide) {
            IGuiHandler.serverTick(t1.guiHandler);

            if (t1.isMultiblockFormed()) {
                t1.setIsRunning(t1.recipeManager.update());
            }
        }
        if(level.isClientSide){
            if(t1.isRunning){
                t1.client_recipeProgress++;
                if(t1.client_recipeProgress >= t1.client_recipeMaxTime){
                    t1.isRunning=false;
                }
            }
        }
    }
}
