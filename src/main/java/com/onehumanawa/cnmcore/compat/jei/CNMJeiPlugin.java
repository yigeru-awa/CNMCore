package com.onehumanawa.cnmcore.compat.jei;

import com.onehumanawa.cnmcore.CNMCore;
import com.onehumanawa.cnmcore.compat.jei.category.BlockCraftingCategory;
import com.onehumanawa.cnmcore.foundation.recipe.blockcrafting.BlockCraftingRecipe;
import com.onehumanawa.cnmcore.foundation.recipe.blockcrafting.BlockCraftingRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class CNMJeiPlugin implements IModPlugin {

    private static final ResourceLocation ID = CNMCore.asResource("jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new BlockCraftingCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(BlockCraftingCategory.TYPE, BlockCraftingRegistry.all().stream().toList());
    }
}