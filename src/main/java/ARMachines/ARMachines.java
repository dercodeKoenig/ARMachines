package ARMachines;

import ARLib.holoProjector.itemHoloProjector;
import ARLib.multiblockCore.EntityMultiblockMaster;
import ARLib.utils.MachineRecipe;
import ARLib.utils.RecipeLoader;
import ARMachines.crystallizer.EntityCrystallizer;
import ARMachines.lathe.EntityLathe;
import ARMachines.rollingMachine.EntityRollingMachine;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static ARMachines.RecipeLoader.loadRecipes;

@Mod(ARMachines.MODID)
public class ARMachines {
    public static final String MODID = "armachines";

    public ARMachines(IEventBus modEventBus, ModContainer modContaine) {
        //NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::loadComplete);
        modEventBus.addListener(this::registerEntityRenderers);

        MultiblockRegistry.register(modEventBus);


    }

    public void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        MultiblockRegistry.registerRenderers(event);
    }


    private void addCreative(BuildCreativeModeTabContentsEvent e) {
        MultiblockRegistry.addCreative(e);
    }

    private void loadComplete(FMLLoadCompleteEvent e) {

        itemHoloProjector.registerMultiblock("Lathe", EntityLathe.structure, EntityLathe.charMapping);
        itemHoloProjector.registerMultiblock("Rolling Machine", EntityRollingMachine.structure, EntityRollingMachine.charMapping);
        itemHoloProjector.registerMultiblock("Crystallizer", EntityCrystallizer.structure, EntityCrystallizer.charMapping);

        loadRecipes();
    }
}
