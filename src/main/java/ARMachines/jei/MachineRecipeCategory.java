package ARMachines.jei;

import ARLib.utils.MachineRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.library.plugins.debug.DebugRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static ARLib.utils.ItemUtils.getFluidStackFromId;
import static ARLib.utils.ItemUtils.getItemStackFromId;
import static ARMachines.MultiblockRegistry.BLOCK_LATHE;

public class MachineRecipeCategory implements IRecipeCategory<MachineRecipe> {

    public MachineRecipeCategory() {
    }
    public static final RecipeType<MachineRecipe> MACHINE_RECIPE_TYPE = new RecipeType<>(
            ResourceLocation.fromNamespaceAndPath("armachines", "machine_recipe"),
            MachineRecipe.class
    );

    @Override
    public RecipeType<MachineRecipe> getRecipeType() {
        return MACHINE_RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("Machine Recipe");
    }
    @Override
    public int getWidth(){
        return 100;
    }
    @Override
    public int getHeight(){
        return 100;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return null;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MachineRecipe recipe, IFocusGroup focuses) {
        // Define inputs
        int rn = 0;
        for (MachineRecipe.recipePart input : recipe.inputs) {
            rn+=1;
            String inputIdOrTag = input.id;
            int amount = input.num;

            ItemStack iStack = getItemStackFromId(inputIdOrTag, amount);
            FluidStack fStack = getFluidStackFromId(inputIdOrTag, amount);

            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, 10, 0 + rn * 20);
            if (iStack != null) {
                slot.addItemStack(iStack);
            } else if (fStack != null) {
                slot.addFluidStack(fStack.getFluid(), fStack.getAmount());
            } else {
                ResourceLocation tagLocation = ResourceLocation.tryParse(inputIdOrTag);
                TagKey<Fluid> fluidTag = TagKey.create(Registries.FLUID, tagLocation);
                List<FluidStack> fluidStacks = new ArrayList<>();

                BuiltInRegistries.FLUID.getTag(fluidTag).ifPresent(tag -> {
                    for (Holder<Fluid> fluidHolder : tag) {
                        Fluid fluid = fluidHolder.value();
                        fluidStacks.add(new FluidStack(fluid, amount));
                    }
                });

                if (!fluidStacks.isEmpty()) {
                    for (FluidStack f : fluidStacks) {
                        slot.addFluidStack(f.getFluid(), f.getAmount());
                    }
                } else {
                    slot.addIngredients(Ingredient.of(TagKey.create(Registries.ITEM, tagLocation)));
                }
            }
        }

        // Define outputs
        rn = 0;
        for (MachineRecipe.recipePart output : recipe.outputs) {
            rn+=1;
            String outputId = output.id;
            int amount = output.num;

            ItemStack iStack = getItemStackFromId(outputId, amount);
            FluidStack fStack = getFluidStackFromId(outputId, amount);

            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, 50, 0 + rn * 20);
            if (iStack != null) {
                slot.addItemStack(iStack);
            }
            if (fStack != null) {
                slot.addFluidStack(fStack.getFluid(), fStack.getAmount());
            }
        }
    }

    @Override
    public void draw(MachineRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // Optional: Draw custom text or graphics, such as energy cost.
        guiGraphics.drawString(
                Minecraft.getInstance().font,
                Component.translatable("Energy per tick: "+ recipe.energyPerTick),
                0, 0,
                0xFF404040,false
        );
    }

    @Override
    public boolean isHandled(MachineRecipe recipe) {
        // Define whether this recipe should be shown or filtered out.
        return true;
    }
}
