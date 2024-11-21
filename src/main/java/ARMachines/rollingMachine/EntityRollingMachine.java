package ARMachines.rollingMachine;


import ARLib.ARLib;
import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.IGuiHandler;
import ARLib.gui.modules.*;
import ARLib.multiblockCore.EntityMultiblockMaster;
import ARLib.multiblockCore.MultiblockRecipeManager;
import ARLib.network.PacketBlockEntity;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import ARLib.utils.MachineRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static ARLib.ARLibRegistry.*;
import static ARMachines.MultiblockRegistry.*;

public class EntityRollingMachine extends EntityMultiblockMaster {


    static List<MachineRecipe> recipes = new ArrayList<>();
    public static void addRecipe(MachineRecipe recipe) {
        recipes.add(recipe);
    }


    // defines what blocks are valid for a char in the structure
    public static HashMap<Character, List<Block>> charMapping = new HashMap<>();
    // structure is defined by char / Block objects. char objects can have multiple valid blocks
    // "c" is ALWAYS used for the controller/master block.
    public     static Object[][][] structure = {
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
    public  Object[][][] getStructure() {
        return structure;
    }
    @Override
    public  HashMap<Character, List<Block>> getCharMapping(){
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

        ResourceLocation modelsrc = ResourceLocation.fromNamespaceAndPath("armachines", "multiblock/rollingmachine.obj");
        try {
            model = new WavefrontObject(modelsrc);
        } catch (ModelFormatException e) {
            throw new RuntimeException(e);
        };
    }

    BlockState coil1 = Blocks.AIR.defaultBlockState();
    BlockState coil2 = Blocks.AIR.defaultBlockState();
    @Override
    public void onStructureComplete() {
        // create a empty guiHandler
        guiHandler = new GuiHandlerBlockEntity(this);

        Object[][][] structure = getStructure();
        Direction front = getFront();
        if (front == null) return;
        Vec3i offset = getControllerOffset(structure);

        int globalX1 = getBlockPos().getX() + (0 - offset.getX()) * front.getStepZ() - (1 - offset.getZ()) * front.getStepX();
        int globalY1 = getBlockPos().getY() - 1 + offset.getY();
        int globalZ1 = getBlockPos().getZ() - (0 - offset.getX()) * front.getStepX() - (1 - offset.getZ()) * front.getStepZ();
        BlockPos globalPos1 = new BlockPos(globalX1, globalY1, globalZ1);
        coil1 = level.getBlockState(globalPos1);

        int globalX2 = getBlockPos().getX() + (0 - offset.getX()) * front.getStepZ() - (2 - offset.getZ()) * front.getStepX();
        int globalY2 = getBlockPos().getY() - 1 + offset.getY();
        int globalZ2 = getBlockPos().getZ() - (0 - offset.getX()) * front.getStepX() - (2 - offset.getZ()) * front.getStepZ();
        BlockPos globalPos2 = new BlockPos(globalX2, globalY2, globalZ2);
        coil2 = level.getBlockState(globalPos2);
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
    }

    // I want the gui only to open when the structure is formed and always only on client side
    public void openGui() {
        if (isMultiblockFormed() && level.isClientSide) {
            this.guiHandler.openGui(176, 165);
        }
    }

    void getUpdateTag(CompoundTag info){
        info.putBoolean("isRunning", this.isRunning);
        info.putInt("recipeProgress", recipeManager.progress);
        info.putBoolean("hasRecipe", recipeManager.currentRecipe != null);
        if(recipeManager.currentRecipe != null) {
            info.putInt("recipeTime", recipeManager.currentRecipe.ticksRequired);
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
        if(tag.contains("time") && tag.getLong("time") > lastUpdateTime) {
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
//                t1.progressBar6px.setProgress((double) t1.client_recipeProgress / t1.client_recipeMaxTime);
                if (t1.client_recipeProgress >= t1.client_recipeMaxTime) {
                    t1.isRunning = false;
                }
            }
        }
    }
}
