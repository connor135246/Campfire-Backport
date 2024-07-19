package connor135246.campfirebackport.client.compat.nei;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.recipe.IUsageHandler;
import connor135246.campfirebackport.util.Reference;

public class NEIConfig implements IConfigureNEI
{

    @Override
    public void loadConfig()
    {
        NEICampfireRecipeHandler crecipes = new NEICampfireRecipeHandler();
        NEICampfireStateChangerHandler cstates = new NEICampfireStateChangerHandler();
        NEISignalFireBlocksHandler sblocks = new NEISignalFireBlocksHandler();

        API.registerRecipeHandler((ICraftingHandler) crecipes);
        API.registerUsageHandler((IUsageHandler) crecipes);

        API.registerRecipeHandler((ICraftingHandler) cstates);
        API.registerUsageHandler((IUsageHandler) cstates);

        API.registerRecipeHandler((ICraftingHandler) sblocks);
        API.registerUsageHandler((IUsageHandler) sblocks);
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
