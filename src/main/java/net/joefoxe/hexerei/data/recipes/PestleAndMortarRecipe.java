package net.joefoxe.hexerei.data.recipes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.joefoxe.hexerei.Hexerei;
import net.joefoxe.hexerei.block.ModBlocks;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class PestleAndMortarRecipe implements Recipe<SimpleContainer> {

    private final ResourceLocation id;
    private final ItemStack output;
    private final int grindingTime;
    private final NonNullList<Ingredient> recipeItems;
    protected static final List<Boolean> itemMatchesSlot = new ArrayList<>();


    @Override
    public boolean isSpecial() {
        return true;
    }
    public PestleAndMortarRecipe(ResourceLocation id, ItemStack output,
                                 NonNullList<Ingredient> recipeItems, int grindingTime) {
        this.id = id;
        this.output = output;
        this.recipeItems = recipeItems;
        this.grindingTime = grindingTime;

        for(int i = 0; i < 5; i++) {
            itemMatchesSlot.add(false);
        }

    }


    @Override
    public boolean matches(SimpleContainer inv, Level worldIn) {

        for(int i = 0; i < 5; i++)
            itemMatchesSlot.set(i, false);

        // the flag is to break out early in case nothing matches for that slot
        boolean flag = false;
        int numberOfEmpty = 5 - this.recipeItems.size();
        // cycle through each recipe slot
        for (Ingredient recipeItem : this.recipeItems) {
            //cycle through each slot for each recipe slot
            for (int i = 0; i < 5; i++) {
                //if the recipe matches a slot
                if (recipeItem.test(inv.getItem(i))) {
                    // if the slot is not taken up
                    if (!itemMatchesSlot.get(i)) {
                        //mark the slot as taken up
                        itemMatchesSlot.set(i, true);
                        flag = true;
                        break;
                    }
                }
            }
            //this is where it breaks out early to stop the craft
            if (!flag)
                break;
            //reset the flag for the next iteration
            flag = false;
        }
        // cycle through each recipe slot
        for(int j = 0; j < numberOfEmpty; j++) {
            //cycle through each slot for each recipe slot
            for (int i = 0; i < 5; i++) {
                //if the recipe matches a slot
                if (inv.getItem(i).isEmpty()) {
                    // if the slot is not taken up
                    if (!itemMatchesSlot.get(i)) {
                        //mark the slot as taken up
                        itemMatchesSlot.set(i, true);
                        flag = true;
                        break;
                    }
                }
            }
            //this is where it breaks out early to stop the craft
            if(!flag)
                break;
            //reset the flag for the next iteration
            flag = false;
        }


        // checks if a slot is not taken up, if it's not taken up then itll not craft
        for(int i = 0; i < 5; i++) {
            if (!itemMatchesSlot.get(i)) {
                return false;
            }
        }
        //if it reaches here that means it has completed the shapeless craft and should craft it
        return true;


//        SHAPED CRAFTING - maybe bring this back as another config in the recipe to see if its shaped or shapeless
//        if(recipeItems.get(0).test(inv.getItem(0)) &&
//            recipeItems.get(1).test(inv.getItem(1)) &&
//            recipeItems.get(2).test(inv.getItem(2)) &&
//            recipeItems.get(3).test(inv.getItem(3)) &&
//            recipeItems.get(4).test(inv.getItem(4)) &&
//            recipeItems.get(5).test(inv.getItem(5)) &&
//            recipeItems.get(6).test(inv.getItem(6)) &&
//            recipeItems.get(7).test(inv.getItem(7)))
//        {
//            return true;
//        }
//        return false;

    }

    @Override
    public ItemStack assemble(SimpleContainer pContainer, RegistryAccess pRegistryAccess) {
        return output;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return recipeItems;
    }

    @Override
    public boolean canCraftInDimensions(int p_43999_, int p_44000_) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess pRegistryAccess) {

        return getOutput();
    }

    public ItemStack getOutput(){
        return output.copy();
    }

    public ItemStack getToastSymbol() {
        return new ItemStack(ModBlocks.PESTLE_AND_MORTAR.get());
    }

    public int getGrindingTime() { return this.grindingTime; }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.PESTLE_AND_MORTAR_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }


    public static class Type implements RecipeType<PestleAndMortarRecipe> {
        private Type() { }
        public static final PestleAndMortarRecipe.Type INSTANCE = new PestleAndMortarRecipe.Type();
        public static final String ID = "pestle_and_mortar";
    }


    // for Serializing the recipe into/from a json
    public static class Serializer implements RecipeSerializer<PestleAndMortarRecipe> {
        public static final PestleAndMortarRecipe.Serializer INSTANCE = new PestleAndMortarRecipe.Serializer();
        public static final ResourceLocation ID =
                new ResourceLocation(Hexerei.MOD_ID,"pestle_and_mortar");

        @Override
        public PestleAndMortarRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            ItemStack output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "output"));

            JsonArray ingredients = GsonHelper.getAsJsonArray(json, "ingredients");
            NonNullList<Ingredient> inputs = NonNullList.withSize(ingredients.size(), Ingredient.EMPTY);
            int grindingTime = GsonHelper.getAsInt(json, "grindingTime");

            for (int i = 0; i < inputs.size(); i++) {
                inputs.set(i, Ingredient.fromJson(ingredients.get(i)));
            }

            return new PestleAndMortarRecipe(recipeId, output,
                    inputs, grindingTime);
        }

        @Nullable
        @Override
        public PestleAndMortarRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            NonNullList<Ingredient> inputs = NonNullList.withSize(buffer.readInt(), Ingredient.EMPTY);

            for (int i = 0; i < inputs.size(); i++) {
                inputs.set(i, Ingredient.fromNetwork(buffer));
            }

            return new PestleAndMortarRecipe(recipeId, buffer.readItem(),
                    inputs, buffer.readInt());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, PestleAndMortarRecipe recipe) {
            buffer.writeInt(recipe.getIngredients().size());
            for (Ingredient ing : recipe.getIngredients()) {
                ing.toNetwork(buffer);
            }
            buffer.writeItem(recipe.output);
            buffer.writeInt(recipe.getGrindingTime());
        }

    }
}
