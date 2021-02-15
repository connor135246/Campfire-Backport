package connor135246.campfirebackport.client.compat.nei;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.recipe.IUsageHandler;

public class NEIConfig implements IConfigureNEI
{

    @Override
    public void loadConfig()
    {
        NEICampfireRecipeHandler crecipes = new NEICampfireRecipeHandler();
        NEICampfireStateChangerHandler cstates = new NEICampfireStateChangerHandler();

        API.registerRecipeHandler((ICraftingHandler) crecipes);
        API.registerUsageHandler((IUsageHandler) crecipes);

        API.registerRecipeHandler((ICraftingHandler) cstates);
        API.registerUsageHandler((IUsageHandler) cstates);
    }

    @Override
    public String getName()
    {
        return "Campfire Backport NEI";
    }

    @Override
    public String getVersion()
    {
        return "1.7.10-1.8.1";
    }
}
