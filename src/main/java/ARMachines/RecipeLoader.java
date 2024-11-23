package ARMachines;

import ARLib.utils.MachineRecipe;
import ARMachines.lathe.EntityLathe;
import ARMachines.rollingMachine.EntityRollingMachine;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class RecipeLoader {

    public static void LatheRecipes(){
        List<MachineRecipe> DefaultRecipes = new ArrayList<>();
        MachineRecipe r = new MachineRecipe();
        r.addInput("c:ingots/iron", 1, 2);
        r.addOutput("immersiveengineering:stick_iron", 1, 2);
        r.energyPerTick = 50;
        r.ticksRequired = 200;
        DefaultRecipes.add(r);

        Path configDir = Paths.get(Minecraft.getInstance().gameDirectory.toString(), "config", "armachines");
        String filename = "lathe.xml";
        List<MachineRecipe> recipes =  ARLib.utils.RecipeLoader.loadRecipes(configDir,filename);
        if (recipes.isEmpty()){
            System.out.println("reset recipes");
            ARLib.utils.RecipeLoader.createRecipeFile(configDir,filename,DefaultRecipes);
            recipes = DefaultRecipes;
        }
        for (MachineRecipe i : recipes) {
            EntityLathe.addRecipe(i);
        }
    }

    public static void rollingMachineRecipes(){
        List<MachineRecipe> DefaultRecipes = new ArrayList<>();
        MachineRecipe r = new MachineRecipe();
        r.addInput("c:ingots/iron", 1,2);
        r.addInput("c:ingots/gold", 1,2);
        r.addOutput("immersiveengineering:plate_iron", 1,2);
        r.addOutput("immersiveengineering:plate_gold", 1,2);
        r.energyPerTick = 50;
        r.ticksRequired = 200;
        DefaultRecipes.add(r);

        Path configDir = Paths.get(Minecraft.getInstance().gameDirectory.toString(), "config", "armachines");
        String filename = "rollingmachine.xml";
        List<MachineRecipe> recipes =  ARLib.utils.RecipeLoader.loadRecipes(configDir,filename);
        if (recipes.isEmpty()){
            ARLib.utils.RecipeLoader.createRecipeFile(configDir,filename,DefaultRecipes);
            recipes = DefaultRecipes;
        }
        for (MachineRecipe i : recipes) {
            EntityRollingMachine.addRecipe(i);
        }
    }

    public static void loadRecipes(){
        RecipeLoader.rollingMachineRecipes();
        RecipeLoader.LatheRecipes();
    }
}
