package ARMachines;

import ARLib.holoProjector.itemHoloProjector;
import ARLib.multiblockCore.EntityMultiblockMaster;
import ARLib.utils.MachineRecipe;
import ARLib.utils.RecipeLoader;
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

@Mod(ARMachines.MODID)
public class ARMachines
{
    public static final String MODID = "armachines";

    public ARMachines(IEventBus modEventBus, ModContainer modContaine) {
        //NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::loadComplete);
        modEventBus.addListener(this::registerEntityRenderers);

        MultiblockRegistry.register(modEventBus);


    }

    public  void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        MultiblockRegistry.registerRenderers(event);
    }


    private void addCreative(BuildCreativeModeTabContentsEvent e){
        MultiblockRegistry.addCreative(e);
    }

    private void loadComplete(FMLLoadCompleteEvent e){


        itemHoloProjector.registerMultiblock("Lathe", EntityLathe.structure, EntityLathe.charMapping);
        itemHoloProjector.registerMultiblock("Rolling Machine", EntityRollingMachine.structure, EntityRollingMachine.charMapping);


        List<MachineRecipe> latheDefaultRecipes = new ArrayList<>();
        MachineRecipe r = new MachineRecipe();
        r.addInput("c:ingots/iron", 1);
        r.addOutput("immersiveengineering:stick_iron", 1);
        r.energyPerTick = 50;
        r.ticksRequired = 100;
        latheDefaultRecipes.add(r);

        Path configDir = Paths.get(Minecraft.getInstance().gameDirectory.toString(), "config", "armachines");
        String filename = "lathe.xml";
        List<MachineRecipe> recipesLathe =  RecipeLoader.loadRecipes(configDir,filename);
        if (recipesLathe.isEmpty()){
            RecipeLoader.createRecipeFile(configDir,"example_"+filename,latheDefaultRecipes);
            recipesLathe = latheDefaultRecipes;
        }
        for (MachineRecipe i : recipesLathe) {
            EntityLathe.addRecipe(i);
            EntityRollingMachine.addRecipe(i);
        }
    }
    }
