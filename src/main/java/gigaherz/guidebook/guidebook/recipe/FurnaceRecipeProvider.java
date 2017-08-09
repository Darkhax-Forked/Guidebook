package gigaherz.guidebook.guidebook.recipe;

import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.IRenderDelegate;
import gigaherz.guidebook.guidebook.elements.Image;
import gigaherz.guidebook.guidebook.elements.Stack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

/**
 * @author joazlazer
 * A class designed to provide furnace recipes for display in Guidebooks
 */
public class FurnaceRecipeProvider extends RecipeProvider {
    private static final int INPUT_SLOT_X = 19;
    private static final int INPUT_SLOT_Y = 3;
    private static final int OUTPUT_SLOT_X = 64;
    private static final int OUTPUT_SLOT_Y = 14;

    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation(GuidebookMod.MODID, "gui/recipe_backgrounds");
    private static final int BACKGROUND_U = 0;
    private static final int BACKGROUND_V = 101;
    private static final int BACKGROUND_W = 100;
    private static final int BACKGROUND_H = 39;

    private static final int HEIGHT = BACKGROUND_H;
    private static final int LEFT_OFFSET = 38;

    public FurnaceRecipeProvider() {
        this.setRegistryName(new ResourceLocation(GuidebookMod.MODID, "furnace"));
    }

    @Override
    public boolean hasRecipe(@Nonnull ItemStack targetOutput) {
        for(ItemStack result : FurnaceRecipes.instance().getSmeltingList().values()) {
            if(result.isItemEqual(targetOutput)) return true;
        }
        return false;
    }

    @Override
    public boolean hasRecipe(@Nonnull ResourceLocation recipeKey) {
        GuidebookMod.logger.warn(String.format("[FurnaceRecipeProvider] Furnace recipe specified via recipeKey '%s', however furnace recipes are not registered using a ResourceLocation. Ignoring.", recipeKey.toString()));
        return false;
    }

    @Override
    @Nullable
    public ProvidedComponents provideRecipeComponents(@Nonnull ItemStack targetOutput, int recipeIndex) {
        // Ignore recipeIndex because a furnace recipe can show each recipe by alternating the slots

        ArrayList<ItemStack> inputStacks = new ArrayList<>();
        for(ItemStack key : FurnaceRecipes.instance().getSmeltingList().keySet()) {
            if(FurnaceRecipes.instance().getSmeltingList().get(key).isItemEqual(targetOutput)) {
                inputStacks.addAll(copyAndExpand(key));
            }
        }

        if(inputStacks.size() > 0) { // Should always be true
            IRenderDelegate additionalRenderer = (nav, left, right) -> { }; // No additional rendering needed
            Stack[] recipeComponents = new Stack[2];

            // Set up input slot element
            Stack inputSlot = new Stack();
            recipeComponents[0] = inputSlot;
            inputSlot.stacks = new ItemStack[inputStacks.size()];
            inputStacks.toArray(inputSlot.stacks);
            inputSlot.x = INPUT_SLOT_X + LEFT_OFFSET;
            inputSlot.y = INPUT_SLOT_Y;

            // Set up output slot element
            Stack outputSlot = new Stack();
            recipeComponents[1] = outputSlot;
            ArrayList<ItemStack> outputStacks = new ArrayList<>();
            outputSlot.stacks = new ItemStack[inputStacks.size()];
            // Add output stacks for each recipe in the same order as the input ones (in case the item quantities vary)
            for(ItemStack inputStack : inputStacks) {
                outputStacks.addAll(copyAndExpand(FurnaceRecipes.instance().getSmeltingResult(inputStack)));
            }
            outputStacks.toArray(outputSlot.stacks);
            outputSlot.x = OUTPUT_SLOT_X + LEFT_OFFSET;
            outputSlot.y = OUTPUT_SLOT_Y;

            // Set up background image
            Image background = new Image();
            background.textureLocation = BACKGROUND_TEXTURE;
            background.x = 0 + LEFT_OFFSET;
            background.y = 0;
            background.tx = BACKGROUND_U;
            background.ty = BACKGROUND_V;
            background.w = BACKGROUND_W;
            background.h = BACKGROUND_H;

            // Set up overall height
            int height = HEIGHT;

            return new ProvidedComponents(height, recipeComponents, background, additionalRenderer);
        } else GuidebookMod.logger.error(String.format("[FurnaceRecipeProvider] Recipe not found for '%s' although hasRecipe(...) returned true. Something is wrong!", targetOutput.toString()));
        return null;
    }

    @Nullable
    @Override
    public ProvidedComponents provideRecipeComponents(@Nonnull ResourceLocation recipeKey) {
        GuidebookMod.logger.warn(String.format("[FurnaceRecipeProvider] Furnace recipe specified via recipeKey '%s', however furnace recipes are not registered using a ResourceLocation. Ignoring.", recipeKey.toString()));
        return null;
    }
}
