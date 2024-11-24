package ARMachines._jei;

import ARMachines._jei.machineCategories.Crystallizer;
import ARMachines._jei.machineCategories.Lathe;
import ARMachines._jei.machineCategories.RollingMachine;
import ARMachines.lathe.EntityLathe;
import ARMachines.rollingMachine.EntityRollingMachine;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class YourMachineJEIPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("armachines", "plugin");
    }
@Override
    public void registerRecipes(IRecipeRegistration registration) {
    registration.addRecipes(Lathe.MACHINE_RECIPE_TYPE, EntityLathe.recipes);
    registration.addRecipes(RollingMachine.MACHINE_RECIPE_TYPE, EntityRollingMachine.recipes);
    registration.addRecipes(Crystallizer.MACHINE_RECIPE_TYPE, EntityLathe.recipes);
    }
@Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
    registration.addRecipeCategories(new Lathe());
    registration.addRecipeCategories(new RollingMachine());
    registration.addRecipeCategories(new Crystallizer());
    }
}