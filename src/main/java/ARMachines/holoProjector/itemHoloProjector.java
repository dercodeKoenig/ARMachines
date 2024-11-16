package ARMachines.holoProjector;

import ARLib.gui.GuiHandlerMainHandItem;
import ARLib.gui.IguiOnClientTick;
import ARLib.gui.modules.GuiModuleBase;
import ARLib.gui.modules.GuiModuleScrollContainer;
import ARLib.gui.modules.guiModuleButton;
import ARLib.network.INetworkTagReceiver;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

import java.util.ArrayList;
import java.util.List;

public class itemHoloProjector extends Item implements INetworkTagReceiver, IguiOnClientTick {

GuiHandlerMainHandItem guiHandler;

    public itemHoloProjector(Properties properties) {
        super(properties);
        guiHandler = new GuiHandlerMainHandItem(this);
        guiModuleButton button = new guiModuleButton(0,"text",guiHandler,30,20,50,20, ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_button_red.png"),64,20);
        List<GuiModuleBase> containerModules =new ArrayList<>();
        containerModules.add(button);
        GuiModuleScrollContainer container = new GuiModuleScrollContainer(containerModules,0xFFA0A0A0,guiHandler,10,10,80,80);
        guiHandler.registerModule(container);
    }

    public InteractionResult useOn(UseOnContext context) {
        guiHandler.openGui(100,100);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void readServer(CompoundTag compoundTag) {
        if(compoundTag.contains("guiButtonClick")){

        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {

    }

    @Override
    public void onGuiClientTick() {

    }
}
