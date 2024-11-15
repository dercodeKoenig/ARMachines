package ARMachines.lathe;


import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.IGuiHandler;
import ARLib.gui.modules.*;
import ARLib.multiblockCore.EntityMultiblockMaster;
import ARLib.multiblockCore.MultiblockRecipeManager;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.MachineRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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

    guiModuleProgressBarHorizontal6px progressBar6px;

    public EntityLathe(BlockPos pos, BlockState state) {
        super(ENTITY_LATHE.get(), pos, state);
        this.alwaysOpenMasterGui = true;
        recipeManager.recipes = EntityLathe.recipes;
        guiHandler = new GuiHandlerBlockEntity(this); // we fill modules only when the structure is complete
    }

    @Override
    // this method will be called if the structure goes from incomplete to completes.
    // this usually happens when the master block is clicked && the multiblock is not complete
    // it will also be called onLoad() if the structure is scanned & completed OR on client side, if the structure is completed (blockstate value checked)
    public void onStructureComplete() {
        // do not create new guihandler instance, this will fuck it all up.
        // use .clearModules() to remove all modules
        guiHandler.clearModules();


        // client has no idea about the input/output tiles. only server needs to know them
        // they are not needed on the client side of the gui. do not try to access the tiles on the clientside, they do not exist here

        // 4 slots for the input block
        guiModuleItemHandlerSlot slotI1 = new guiModuleItemHandlerSlot(1, level.isClientSide ? null : this.itemInTiles.get(0), 0, 1, 0, guiHandler, 50, 10);
        guiModuleItemHandlerSlot slotI2 = new guiModuleItemHandlerSlot(2, level.isClientSide ? null : this.itemInTiles.get(0), 1, 1, 0, guiHandler, 50, 30);
        guiModuleItemHandlerSlot slotI3 = new guiModuleItemHandlerSlot(3, level.isClientSide ? null : this.itemInTiles.get(0), 2, 1, 0, guiHandler, 70, 10);
        guiModuleItemHandlerSlot slotI4 = new guiModuleItemHandlerSlot(4, level.isClientSide ? null : this.itemInTiles.get(0), 3, 1, 0, guiHandler, 70, 30);

        // 4 slots for the output block
        guiModuleItemHandlerSlot slotO1 = new guiModuleItemHandlerSlot(5, level.isClientSide ? null : this.itemOutTiles.get(0), 0, 2, 0, guiHandler, 130, 10);
        guiModuleItemHandlerSlot slotO2 = new guiModuleItemHandlerSlot(6, level.isClientSide ? null : this.itemOutTiles.get(0), 1, 2, 0, guiHandler, 130, 30);
        guiModuleItemHandlerSlot slotO3 = new guiModuleItemHandlerSlot(7, level.isClientSide ? null : this.itemOutTiles.get(0), 2, 2, 0, guiHandler, 110, 10);
        guiModuleItemHandlerSlot slotO4 = new guiModuleItemHandlerSlot(8, level.isClientSide ? null : this.itemOutTiles.get(0), 3, 2, 0, guiHandler, 110, 30);

        guiModuleEnergy energyBar = new guiModuleEnergy(9, level.isClientSide ? null : this.energyInTiles.get(0), guiHandler, 10, 10);

        List<guiModulePlayerInventorySlot> playerHotBar = guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 140, 100, 0, 1, this.guiHandler);
        for (guiModulePlayerInventorySlot i : playerHotBar)
            guiHandler.registerModule(i);

        List<guiModulePlayerInventorySlot> playerInventory = guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 70, 200, 0, 1, this.guiHandler);
        for (guiModulePlayerInventorySlot i : playerInventory)
            guiHandler.registerModule(i);

        guiHandler.registerModule(slotI1);
        guiHandler.registerModule(slotI2);
        guiHandler.registerModule(slotI3);
        guiHandler.registerModule(slotI4);

        guiHandler.registerModule(slotO1);
        guiHandler.registerModule(slotO2);
        guiHandler.registerModule(slotO3);
        guiHandler.registerModule(slotO4);

        guiHandler.registerModule(energyBar);


        progressBar6px = new guiModuleProgressBarHorizontal6px(-1, 0xFFF0F0F0, guiHandler, 60, 55);
        guiHandler.registerModule(progressBar6px);
        guiHandler.registerModule(new guiModuleImage(guiHandler, 90, 20, 16, 12, ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/arrow_right.png"), 16, 12));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level.isClientSide) {
            CompoundTag info = new CompoundTag();
            info.putByte("client_onload", (byte) 0);
            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, info));
        }
    }

    void setIsRunning(boolean isrunning) {
        if (this.isRunning != isrunning) {
            this.isRunning = isrunning;
            if (this.isRunning) {
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
        if (isMultiblockFormed() && level.isClientSide) {
            this.guiHandler.openGui(176, 165);
        }
    }

    @Override
    public void readServer(CompoundTag tag) {

        guiHandler.readServer(tag);
        super.readServer(tag);
        if (tag.contains("client_onload")) {
            CompoundTag info = new CompoundTag();
            info.putBoolean("isRunning", this.isRunning);
            if (this.isRunning) {
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
        if (level.isClientSide) {
            if (t1.isRunning) {
                t1.client_recipeProgress++;
                t1.progressBar6px.setProgress((double) t1.client_recipeProgress / t1.client_recipeMaxTime);

                if (t1.client_recipeProgress >= t1.client_recipeMaxTime) {
                    t1.isRunning = false;
                }
            }
        }
    }
}
