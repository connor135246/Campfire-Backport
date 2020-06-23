package connor135246.campfirebackport.client.compat;

import java.util.ArrayList;
import java.util.List;

import codechicken.nei.PositionedStack;
import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.recipe.IUsageHandler;
import codechicken.nei.recipe.TemplateRecipeHandler.CachedRecipe;
import connor135246.campfirebackport.common.crafting.CampfireRecipe;
import connor135246.campfirebackport.util.Reference;
import net.minecraft.item.ItemStack;

public class NEIConfig implements IConfigureNEI
{
    @Override
    public void loadConfig()
    {
        API.registerRecipeHandler((ICraftingHandler) new NEICampfireHandler());
        API.registerUsageHandler((IUsageHandler) new NEICampfireHandler());
    }

    @Override
    public String getName()
    {
        return "Campfire Backport NEI";
    }

    @Override
    public String getVersion()
    {
        return Reference.VERSION;
    }
}
