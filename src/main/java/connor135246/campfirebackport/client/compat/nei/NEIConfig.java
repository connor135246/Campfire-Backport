package connor135246.campfirebackport.client.compat.nei;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.recipe.IUsageHandler;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.common.compat.CampfireBackportCompat;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.util.Reference;
import net.minecraft.item.ItemStack;

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

        if (!(CampfireBackportCompat.isNetherliciousLoaded || CampfireBackportConfig.showExtraCampfires))
        {
            API.hideItem(new ItemStack(CampfireBackportBlocks.foxfire_campfire));
            API.hideItem(new ItemStack(CampfireBackportBlocks.foxfire_campfire_base));
            API.hideItem(new ItemStack(CampfireBackportBlocks.shadow_campfire));
            API.hideItem(new ItemStack(CampfireBackportBlocks.shadow_campfire_base));
        }
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
