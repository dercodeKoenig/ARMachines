package ARMachines.holoProjector;

import ARLib.gui.GuiHandlerMainHandItem;
import ARLib.gui.IguiOnClientTick;
import ARLib.gui.modules.GuiModuleBase;import ARLib.gui.modules.guiModuleButton;
import ARLib.gui.modules.guiModuleScrollContainer;
import ARLib.multiblockCore.EntityMultiblockMaster;
import ARLib.network.INetworkItemStackTagReceiver;
import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketPlayerMainHand;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

class BlockInfo{
    BlockInfo(){}
    List<Block> allowedBlocks;
    BlockPos pos;
}

public class itemHoloProjector extends Item implements INetworkItemStackTagReceiver, IguiOnClientTick {
static GuiHandlerMainHandItem guiHandler;

    public static Map<String, List<BlockInfo>> structureBlocks= new HashMap<>();
    public static Map<Integer, String> buttonIdToMachineName= new HashMap<>();
    static int id = 0;

public static void registerMultiblock(String name, Object[][][] structure,HashMap<Character, List<Block>> charMapping) {
    List<BlockInfo> blockInfoList = new ArrayList<>();
    for (int y = 0; y < structure.length; y++) {
        for (int z = 0; z < structure[y].length; z++) {
            for (int x = 0; x < structure[y][z].length; x++) {
                List<Block> allowed_blocks = itemHoloProjector.getAllowableBlocks(structure[y][z][x], charMapping);
                BlockInfo info = new BlockInfo();
                info.pos = new BlockPos(x, y, z);
                info.allowedBlocks = allowed_blocks;
                blockInfoList.add(info);
            }
        }
    }
    structureBlocks.put(name, blockInfoList);
    buttonIdToMachineName.put(id, name);
    id += 1;
}

    public static List<Block> getAllowableBlocks(Object input, HashMap<Character, List<Block>> charMapping ) {
        if (input instanceof Character && charMapping .containsKey(input)) {
            return charMapping .get(input);
        } else if (input instanceof String) { //OreDict entry

        } else if (input instanceof Block) {
            List<Block> list = new ArrayList<>();
            list.add((Block) input);
            return list;
        } else if (input instanceof List) {
            return (List<Block>) input;
        }
        return new ArrayList<>();
    }


    public itemHoloProjector(Properties properties) {
        super(properties);
        NeoForge.EVENT_BUS.addListener(this::handleScroll);
    }

    private void handleScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player != null &&
                player.getMainHandItem().is(this) &&
                player.isShiftKeyDown()) {

            double scrollDelta = event.getScrollDeltaY();

            UUID id = player.getUUID();
            CompoundTag tag = new CompoundTag();
            if (scrollDelta > 0) {
                // Scrolled up
                tag.putBoolean("scroll",true);
            } else if (scrollDelta < 0) {
                // Scrolled down
                tag.putBoolean("scroll",false);
            }

            PacketDistributor.sendToServer(new PacketPlayerMainHand(id,tag));
            event.setCanceled(true);
        }
    }

    public void initGui(){
        guiHandler = new GuiHandlerMainHandItem(this);
        List<GuiModuleBase> containerModules = new ArrayList<>();

        for (int id : buttonIdToMachineName.keySet()) {
            String name = buttonIdToMachineName.get(id);
            guiModuleButton button = new guiModuleButton(id, name, guiHandler, 25, 20*id+20, 50, 20, ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_button_red.png"), 64, 20);
            containerModules.add(button);
        }
        guiModuleScrollContainer container = new guiModuleScrollContainer(containerModules, 0xFFA0A0A0, guiHandler, 10, 10, 80, 80);
        guiHandler.registerModule(container);
    }
    @Override
    public InteractionResult useOn(UseOnContext context) {

        return InteractionResult.SUCCESS;
    }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemstack = player.getItemInHand(usedHand);
        if(player.isShiftKeyDown()){
            if(level.isClientSide) {
                initGui();
                guiHandler.openGui(100, 100);
            }
            return InteractionResultHolder.success(itemstack);
        }
        return InteractionResultHolder.pass(itemstack);
}

CompoundTag getStacktagOrEmpty(ItemStack stack){

    try {
        return stack.get(DataComponents.CUSTOM_DATA).copyTag();
    } catch (Exception e) {
        return new CompoundTag();
    }
}
void setTag(ItemStack stack, CompoundTag tag){
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
}
int machineHeight(String name){
    List<BlockInfo> blocks = structureBlocks.get(name);
    int maxY=0;
    for (BlockInfo i : blocks){
        maxY = Math.max(maxY,i.pos.getY());
    }
    return maxY;
}

    @Override
    public void readServer(CompoundTag compoundTag, ItemStack stack) {
        CompoundTag itemTag = getStacktagOrEmpty(stack);

        if (compoundTag.contains("guiButtonClick")) {
            int buttonId = compoundTag.getInt("guiButtonClick");
            String name = buttonIdToMachineName.get(buttonId);
            itemTag.putString("selectedMachine", name);
            setTag(stack, itemTag);
        }

        if (compoundTag.contains("scroll") && itemTag.contains("selectedMachine")) {
            boolean was_upScroll = compoundTag.getBoolean("scroll");

            int y = 0;
            if(itemTag.contains("y")){
                y = itemTag.getInt("y");
            }
            if(was_upScroll && y < machineHeight(itemTag.getString("selectedMachine"))){
                y+=1;
            }

            if(!was_upScroll && y > 0){
                y-=1;
            }
            itemTag.putInt("y", y);
            setTag(stack, itemTag);


            System.out.println(getStacktagOrEmpty(stack));


        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {

    }

    @Override
    public void onGuiClientTick() {

    }
}
