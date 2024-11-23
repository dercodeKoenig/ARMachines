package ARMachines.jei;

import ARLib.utils.MachineRecipe;
import ARMachines.jei.machineCategories.Lathe;
import ARMachines.jei.machineCategories.RollingMachine;
import ARMachines.lathe.EntityLathe;
import ARMachines.rollingMachine.EntityRollingMachine;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;
import java.util.stream.Collectors;

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
    }
@Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
    registration.addRecipeCategories(new Lathe());
    registration.addRecipeCategories(new RollingMachine());
    }
}