package ARMachines.rollingMachine;


import ARLib.ARLib;
import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.IGuiHandler;
import ARLib.gui.modules.*;
import ARLib.multiblockCore.BlockMultiblockPlaceholder;
import ARLib.multiblockCore.EntityMultiblockMaster;
import ARLib.multiblockCore.EntityMultiblockPlaceholder;
import ARLib.multiblockCore.MultiblockRecipeManager;
import ARLib.network.PacketBlockEntity;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import ARLib.utils.InventoryUtils;
import ARLib.utils.ItemFluidStacks;
import ARLib.utils.MachineRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static ARLib.ARLibRegistry.*;
import static ARLib.utils.ItemUtils.getItemStackFromId;
import static ARMachines.MultiblockRegistry.*;

public class EntityRollingMachine extends EntityMultiblockMaster {


    public static List<MachineRecipe> recipes = new ArrayList<>();

    public static void addRecipe(MachineRecipe recipe) {
        recipes.add(recipe);
    }


    // defines what blocks are valid for a char in the structure
    public static HashMap<Character, List<Block>> charMapping = new HashMap<>();
    // structure is defined by char / Block objects. char objects can have multiple valid blocks
    // "c" is ALWAYS used for the controller/master block.
    public static Object[][][] structure = {
            {{'c', null, Blocks.AIR, Blocks.AIR},
                    {'I', Blocks.AIR, BLOCK_STRUCTURE.get(), Blocks.AIR},
                    {'I', Blocks.AIR, BLOCK_STRUCTURE.get(), Blocks.AIR}},

            {{'P', 'i', BLOCK_STRUCTURE.get(), null},
                    {'C', BLOCK_STRUCTURE.get(), BLOCK_STRUCTURE.get(), 'O'},
                    {'C', BLOCK_STRUCTURE.get(), BLOCK_STRUCTURE.get(), 'O'}}
    };

    // setup all the blocks that can be used for a char in the structure
    // I is item input, O is item output, P is power input
    static {
        // "c" is ALWAYS used for the controller/master block.
        List<Block> c = new ArrayList<>();
        c.add(BLOCK_ROLLINGMACHINE.get());
        charMapping.put('c', c);

        List<Block> C = new ArrayList<>();
        C.add(BLOCK_COIL_COPPER.get());
        charMapping.put('C', C);

        List<Block> I = new ArrayList<>();
        I.add(BLOCK_ITEM_INPUT_BLOCK.get());
        charMapping.put('I', I);

        List<Block> i = new ArrayList<>();
        i.add(BLOCK_FLUID_INPUT_BLOCK.get());
        charMapping.put('i', i);

        List<Block> O = new ArrayList<>();
        O.add(BLOCK_ITEM_OUTPUT_BLOCK.get());
        charMapping.put('O', O);

        List<Block> P = new ArrayList<>();
        P.add(BLOCK_ENERGY_INPUT_BLOCK.get());
        charMapping.put('P', P);
    }

    @Override
    public Object[][][] getStructure() {
        return structure;
    }

    @Override
    public HashMap<Character, List<Block>> getCharMapping() {
        return charMapping;
    }


    // this is used for gui
    IGuiHandler guiHandler;

    // this is used for simple recipe management - you dont need to worry about recipe logic
    public MultiblockRecipeManager<EntityRollingMachine> recipeManager = new MultiblockRecipeManager<>(this);

    // self explaining
    boolean isRunning;

    // this is on client side, the client has no recipe manager but it still needs to know
    // the progress and current recipe total time for rendering the multiblock
    int client_recipeMaxTime = 1;
    int client_recipeProgress = 0;
    boolean client_hasRecipe = false;
    ItemFluidStacks client_nextConsumedStacks = new ItemFluidStacks();
    ItemFluidStacks client_nextProducedStacks = new ItemFluidStacks();

    // this gui module is not sync-able, it uses a set() method to set the value so we need to keep an instance of it
    guiModuleProgressBarHorizontal6px progressBar6px;

    WavefrontObject model;

    public EntityRollingMachine(BlockPos pos, BlockState state) {
        super(ENTITY_ROLLINGMACHINE.get(), pos, state);
        this.alwaysOpenMasterGui = true; // makes the master gui open no matter what block of the multiblock is clicked
        recipeManager.recipes = EntityRollingMachine.recipes; // copy static loaded recipes to the manager

        // create the guiHandler - this is only to prevent nullpointer when readClient or readServer or tick is called
        // it is just a placeholder for now
        // we fill modules only when the structure is complete
        // because we need access to the item/fluid blocks and
        // we only get this after the structure is completed
        guiHandler = new GuiHandlerBlockEntity(this);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ResourceLocation modelsrc = ResourceLocation.fromNamespaceAndPath("armachines", "multiblock/rollingmachine_new.obj");
            try {
                model = new WavefrontObject(modelsrc);
            } catch (ModelFormatException e) {
                throw new RuntimeException(e);
            }
        }
    }

    BlockState coil1 = Blocks.AIR.defaultBlockState();
    BlockState coil2 = Blocks.AIR.defaultBlockState();

    @Override
    public void onStructureComplete() {
        // create a empty guiHandler
        guiHandler = new GuiHandlerBlockEntity(this);

        guiModuleEnergy energyBar = new guiModuleEnergy(17, level.isClientSide ? null : this.energyInTiles.get(0), guiHandler, 10, 10);
        guiHandler.registerModule(energyBar);

        guiModuleFluidTankDisplay fluidInput = new guiModuleFluidTankDisplay(18,level.isClientSide ? null :fluidInTiles.get(0),0,guiHandler,30,10);
        guiHandler.registerModule(fluidInput);
        guiModuleItemHandlerSlot fluidInSlot = new guiModuleItemHandlerSlot(19, level.isClientSide ? null : this.fluidInTiles.get(0), 0, 1, 0, guiHandler, 50, 10);
        guiModuleItemHandlerSlot fluidOutSlot = new guiModuleItemHandlerSlot(20, level.isClientSide ? null : this.fluidInTiles.get(0), 1, 1, 0, guiHandler, 50, 45);
        guiHandler.registerModule(fluidInSlot);
        guiHandler.registerModule(fluidOutSlot);
        guiHandler.registerModule(new guiModuleImage(guiHandler, 50, 30, 16, 12, ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/arrow_down.png"), 16, 12));

        // 8 slots for the input block
        // every sot has a groupId and a instantTransferId - this way you can specify what slots will be targeted on instant-item-transfer during shift click
        guiModuleItemHandlerSlot slotI1 = new guiModuleItemHandlerSlot(1, level.isClientSide ? null : this.itemInTiles.get(0), 0, 1, 0, guiHandler, 90, 50);
        guiModuleItemHandlerSlot slotI2 = new guiModuleItemHandlerSlot(2, level.isClientSide ? null : this.itemInTiles.get(0), 1, 1, 0, guiHandler, 90, 70);
        guiModuleItemHandlerSlot slotI3 = new guiModuleItemHandlerSlot(3, level.isClientSide ? null : this.itemInTiles.get(0), 2, 1, 0, guiHandler, 110, 50);
        guiModuleItemHandlerSlot slotI4 = new guiModuleItemHandlerSlot(4, level.isClientSide ? null : this.itemInTiles.get(0), 3, 1, 0, guiHandler, 110, 70);
        guiHandler.registerModule(slotI1);
        guiHandler.registerModule(slotI2);
        guiHandler.registerModule(slotI3);
        guiHandler.registerModule(slotI4);
        guiModuleItemHandlerSlot slotI5 = new guiModuleItemHandlerSlot(5, level.isClientSide ? null : this.itemInTiles.get(1), 0, 1, 0, guiHandler, 90, 10);
        guiModuleItemHandlerSlot slotI6 = new guiModuleItemHandlerSlot(6, level.isClientSide ? null : this.itemInTiles.get(1), 1, 1, 0, guiHandler, 90, 30);
        guiModuleItemHandlerSlot slotI7 = new guiModuleItemHandlerSlot(7, level.isClientSide ? null : this.itemInTiles.get(1), 2, 1, 0, guiHandler, 110, 10);
        guiModuleItemHandlerSlot slotI8 = new guiModuleItemHandlerSlot(8, level.isClientSide ? null : this.itemInTiles.get(1), 3, 1, 0, guiHandler, 110, 30);
        guiHandler.registerModule(slotI5);
        guiHandler.registerModule(slotI6);
        guiHandler.registerModule(slotI7);
        guiHandler.registerModule(slotI8);

        // 8 slots for the output block
        guiModuleItemHandlerSlot slotO1 = new guiModuleItemHandlerSlot(9, level.isClientSide ? null : this.itemOutTiles.get(0), 0, 2, 0, guiHandler, 150, 50);
        guiModuleItemHandlerSlot slotO2 = new guiModuleItemHandlerSlot(10, level.isClientSide ? null : this.itemOutTiles.get(0), 1, 2, 0, guiHandler, 150, 70);
        guiModuleItemHandlerSlot slotO3 = new guiModuleItemHandlerSlot(11, level.isClientSide ? null : this.itemOutTiles.get(0), 2, 2, 0, guiHandler, 170, 50);
        guiModuleItemHandlerSlot slotO4 = new guiModuleItemHandlerSlot(12, level.isClientSide ? null : this.itemOutTiles.get(0), 3, 2, 0, guiHandler, 170, 70);
        guiHandler.registerModule(slotO1);
        guiHandler.registerModule(slotO2);
        guiHandler.registerModule(slotO3);
        guiHandler.registerModule(slotO4);
        guiModuleItemHandlerSlot slotO5 = new guiModuleItemHandlerSlot(13, level.isClientSide ? null : this.itemOutTiles.get(1), 0, 2, 0, guiHandler, 150, 10);
        guiModuleItemHandlerSlot slotO6 = new guiModuleItemHandlerSlot(14, level.isClientSide ? null : this.itemOutTiles.get(1), 1, 2, 0, guiHandler, 150, 30);
        guiModuleItemHandlerSlot slotO7 = new guiModuleItemHandlerSlot(15, level.isClientSide ? null : this.itemOutTiles.get(1), 2, 2, 0, guiHandler, 170, 10);
        guiModuleItemHandlerSlot slotO8 = new guiModuleItemHandlerSlot(16, level.isClientSide ? null : this.itemOutTiles.get(1), 3, 2, 0, guiHandler, 170, 30);
        guiHandler.registerModule(slotO5);
        guiHandler.registerModule(slotO6);
        guiHandler.registerModule(slotO7);
        guiHandler.registerModule(slotO8);

        // create the hotbar slots first, inventory-instant-item-transfer will try slots by the order they were registered
        List<guiModulePlayerInventorySlot> playerHotBar = guiModulePlayerInventorySlot.makePlayerHotbarModules(17, 180, 100, 0, 1, this.guiHandler);
        for (guiModulePlayerInventorySlot i : playerHotBar)
            guiHandler.registerModule(i);

        List<guiModulePlayerInventorySlot> playerInventory = guiModulePlayerInventorySlot.makePlayerInventoryModules(17, 110, 200, 0, 1, this.guiHandler);
        for (guiModulePlayerInventorySlot i : playerInventory)
            guiHandler.registerModule(i);


        progressBar6px = new guiModuleProgressBarHorizontal6px(-1, 0xFFF0F0F0, guiHandler, 10, 80);
        guiHandler.registerModule(progressBar6px);

        guiHandler.registerModule(new guiModuleImage(guiHandler, 130, 30, 16, 12, ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/arrow_right.png"), 16, 12));

    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level.isClientSide) {
            // when the client loads, send a packet to the server and request initial nbt required for rendering
            CompoundTag info = new CompoundTag();
            info.putUUID("client_onload", Minecraft.getInstance().player.getUUID());
            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, info));
        }
        coil1 = Blocks.AIR.defaultBlockState();
        coil2 = Blocks.AIR.defaultBlockState();
    }

    // I want the gui only to open when the structure is formed and always only on client side
    public void openGui() {
        if (isMultiblockFormed() && level.isClientSide) {
            this.guiHandler.openGui(196, 205);
        }
    }

    // we sync isRunning, recipeProgress, recipe time and inputs/outputs to be able to render / animate the machine
    void getUpdateTag(CompoundTag info) {
        info.putBoolean("isRunning", this.isRunning);
        info.putInt("recipeProgress", recipeManager.progress);
        info.putBoolean("hasRecipe", recipeManager.currentRecipe != null);
        if (recipeManager.currentRecipe != null) {
            info.putInt("recipeTime", recipeManager.currentRecipe.ticksRequired);
            ItemFluidStacks usedStacks = consumeInput(recipeManager.currentRecipe.inputs, true);
            CompoundTag usedStacksNBT = new CompoundTag();
            usedStacks.toNBT(usedStacksNBT, level.registryAccess());
            info.put("nextConsumedStacks", usedStacksNBT);


            ItemFluidStacks nextProducedStacks = recipeManager.getNextProducedItems();
            CompoundTag nextProducedStacksNBT = new CompoundTag();
            nextProducedStacks.toNBT(nextProducedStacksNBT, level.registryAccess());
            info.put("nextProducedStacks", nextProducedStacksNBT);

        }
        info.putLong("time", System.currentTimeMillis());
    }

    // used on serverside, will notify the client that the machine is running or not
    // and sends the recipe time and current progress.
    void setIsRunning(boolean isrunning) {
        if (this.isRunning != isrunning) {
            this.isRunning = isrunning;
            CompoundTag info = new CompoundTag();
            getUpdateTag(info);
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, info));
        }
    }

    @Override
    // incoming nbt from network to server will be received here
    public void readServer(CompoundTag tag) {
        // dont forget to pass the tag to the guiHandler as it uses the same interface to update the gui elements
        guiHandler.readServer(tag);

        if (tag.contains("client_onload")) {
            // here is the packet from the onload received
            // respond with the required data
            UUID targetId = tag.getUUID("client_onload");
            ServerPlayer targetPlayer = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(targetId);
            CompoundTag info = new CompoundTag();
            getUpdateTag(info);
            PacketDistributor.sendToPlayer(targetPlayer, PacketBlockEntity.getBlockEntityPacket(this, info));
        }

        //super.readServer(tag); // <- not needed here
    }

    long lastUpdateTime = 0; // because network packets can come in different order from what they are sent

    @Override
    // incoming nbt from network to this client will be received here
    public void readClient(CompoundTag tag) {
        // dont forget to pass the tag to the guiHandler as it uses the same interface to update the gui elements
        guiHandler.readClient(tag);

        // the super class uses this method too to update the status of is stucture complete
        // we do not need to call the super method in readServer as it is empty there
        super.readClient(tag);

        // this are the commands I use in communication
        if (tag.contains("openGui")) {
            openGui();
        }
        if (tag.contains("time") && tag.getLong("time") > lastUpdateTime) {
            lastUpdateTime = tag.getLong("time");
            if (tag.contains("isRunning")) {
                this.isRunning = tag.getBoolean("isRunning");
            }
            if (tag.contains("recipeProgress")) {
                client_recipeProgress = tag.getInt("recipeProgress");
            }
            if (tag.contains("hasRecipe")) {
                client_hasRecipe = tag.getBoolean("hasRecipe");
            }
            if (tag.contains("recipeTime")) {
                client_recipeMaxTime = tag.getInt("recipeTime");
            }
            if (tag.contains("nextConsumedStacks")) {
                CompoundTag nextConsumedStacks = tag.getCompound("nextConsumedStacks");
                client_nextConsumedStacks.fromNBT(nextConsumedStacks, level.registryAccess());
            }
            if (tag.contains("nextProducedStacks")) {
                CompoundTag nextProducedStacks = tag.getCompound("nextProducedStacks");
                client_nextProducedStacks.fromNBT(nextProducedStacks, level.registryAccess());
            }
        }
    }

    // this is the tick method
    public static <x extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, x t) {
        EntityRollingMachine t1 = (EntityRollingMachine) t;
        if (!level.isClientSide) {
            // update the guiHandler, it checks if anything has changed in the gui and sends changes to the clients tracking the gui
            IGuiHandler.serverTick(t1.guiHandler);
            if (t1.isMultiblockFormed()) {
                // if the machine is complete, let the recipe manager do it's job.
                // it will automatically scan for recipes and process them
                // it will return true if it is working and false if it has no work or is unable to work
                t1.setIsRunning(t1.recipeManager.update());
            }
        }


        if (level.isClientSide) {
            if (t1.isRunning) {
                // on client side, also update the progress.
                t1.client_recipeProgress++;
                t1.progressBar6px.setProgress((double) t1.client_recipeProgress / t1.client_recipeMaxTime);
                if (t1.client_recipeProgress >= t1.client_recipeMaxTime) {
                    t1.isRunning = false;
                }
            }
            if (t1.isMultiblockFormed()) {
                if (t1.coil1.getBlock().equals(Blocks.AIR) || t1.coil2.getBlock().equals(Blocks.AIR)) {
                    Object[][][] structure = t1.getStructure();
                    Direction front = t1.getFront();
                    if (front == null) return;
                    Vec3i offset = t1.getControllerOffset(structure);

                    int globalX1 = t1.getBlockPos().getX() + (0 - offset.getX()) * front.getStepZ() - (1 - offset.getZ()) * front.getStepX();
                    int globalY1 = t1.getBlockPos().getY() - 1 + offset.getY();
                    int globalZ1 = t1.getBlockPos().getZ() - (0 - offset.getX()) * front.getStepX() - (1 - offset.getZ()) * front.getStepZ();
                    BlockPos globalPos1 = new BlockPos(globalX1, globalY1, globalZ1);
                    t1.coil1 = level.getBlockState(globalPos1);
                    if (t1.coil1.getBlock() instanceof BlockMultiblockPlaceholder bmp) {
                        EntityMultiblockPlaceholder te1 = (EntityMultiblockPlaceholder) level.getBlockEntity(globalPos1);
                        t1.coil1 = te1.replacedState;
                    }

                    int globalX2 = t1.getBlockPos().getX() + (0 - offset.getX()) * front.getStepZ() - (2 - offset.getZ()) * front.getStepX();
                    int globalY2 = t1.getBlockPos().getY() - 1 + offset.getY();
                    int globalZ2 = t1.getBlockPos().getZ() - (0 - offset.getX()) * front.getStepX() - (2 - offset.getZ()) * front.getStepZ();
                    BlockPos globalPos2 = new BlockPos(globalX2, globalY2, globalZ2);
                    t1.coil2 = level.getBlockState(globalPos2);
                    if (t1.coil2.getBlock() instanceof BlockMultiblockPlaceholder bmp) {
                        EntityMultiblockPlaceholder te2 = (EntityMultiblockPlaceholder) level.getBlockEntity(globalPos2);
                        t1.coil2 = te2.replacedState;
                    }
                }
            }
        }
    }
}
