package ARMachines;

import ARLib.utils.MachineRecipe;
import ARMachines.crystallizer.EntityCrystallizer;
import ARMachines.lathe.EntityLathe;
import ARMachines.rollingMachine.EntityRollingMachine;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class RecipeLoader {

    public static void CrystallizerRecipes(){
        List<MachineRecipe> DefaultRecipes = new ArrayList<>();
        MachineRecipe r = new MachineRecipe();
        r.addInput("minecraft:water", 10, 2);
        r.addInput("c:ingots/iron", 10, 2);
        r.addOutput("immersiveengineering:stick_iron", 1, 2);
        r.energyPerTick = 0;
        r.ticksRequired = 500;
        DefaultRecipes.add(r);
        MachineRecipe r2 = new MachineRecipe();
        r2.addInput("minecraft:water", 10, 2);
        r2.addInput("c:ingots/gold", 1, 2);
        r2.addOutput("immersiveengineering:plate_iron", 1, 2);
        r2.addOutput("immersiveengineering:plate_gold", 1, 2);
        r2.energyPerTick = 0;
        r2.ticksRequired = 800;
        DefaultRecipes.add(r2);

        Path configDir = Paths.get(FMLPaths.CONFIGDIR.get().toString(), "armachines");
        String filename = "cystallizer.xml";
        if (!Files.exists(configDir.resolve(filename))) {
            ARLib.utils.RecipeLoader.createRecipeFile(configDir,filename,DefaultRecipes);
        }
        List<MachineRecipe> recipes =  ARLib.utils.RecipeLoader.loadRecipes(configDir,filename);
        for (MachineRecipe i : recipes) {
            EntityCrystallizer.addRecipe(i);
        }
    }
    public static void LatheRecipes(){
        List<MachineRecipe> DefaultRecipes = new ArrayList<>();
        MachineRecipe r = new MachineRecipe();
        r.addInput("c:ingots/iron", 1, 2);
        r.addOutput("immersiveengineering:stick_iron", 1, 2);
        r.energyPerTick = 50;
        r.ticksRequired = 200;
        DefaultRecipes.add(r);

        Path configDir = Paths.get(FMLPaths.CONFIGDIR.get().toString(), "armachines");
        String filename = "lathe.xml";
        if (!Files.exists(configDir.resolve(filename))) {
            ARLib.utils.RecipeLoader.createRecipeFile(configDir,filename,DefaultRecipes);
        }
        List<MachineRecipe> recipes =  ARLib.utils.RecipeLoader.loadRecipes(configDir,filename);
        for (MachineRecipe i : recipes) {
            EntityLathe.addRecipe(i);
        }
    }

    public static void rollingMachineRecipes(){
        List<MachineRecipe> DefaultRecipes = new ArrayList<>();
        MachineRecipe r = new MachineRecipe();
        r.addInput("c:ingots/iron", 1,0.8f);
        r.addInput("c:ingots/gold", 1,2);
        r.addInput("minecraft:water", 1000,0.1f);
        r.addOutput("immersiveengineering:plate_iron", 5,0.5f);
        r.addOutput("immersiveengineering:plate_gold", 2,0.2f);
        r.energyPerTick = 50;
        r.ticksRequired = 200;
        DefaultRecipes.add(r);

        Path configDir = Paths.get(FMLPaths.CONFIGDIR.get().toString(), "armachines");
        String filename = "rollingmachine.xml";
        if (!Files.exists(configDir.resolve(filename))) {
            ARLib.utils.RecipeLoader.createRecipeFile(configDir,filename,DefaultRecipes);
        }
        List<MachineRecipe> recipes =  ARLib.utils.RecipeLoader.loadRecipes(configDir,filename);
        for (MachineRecipe i : recipes) {
            EntityRollingMachine.addRecipe(i);
        }
    }

    public static void loadRecipes(){
        RecipeLoader.LatheRecipes();
        RecipeLoader.rollingMachineRecipes();
        RecipeLoader.CrystallizerRecipes();
    }
}
