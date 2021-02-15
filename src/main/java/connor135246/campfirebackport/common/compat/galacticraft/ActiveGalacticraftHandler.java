package connor135246.campfirebackport.common.compat.galacticraft;

import connor135246.campfirebackport.common.compat.CampfireBackportCompat;
import connor135246.campfirebackport.common.compat.CampfireBackportCompat.ISpaceHandler;
import micdoodle8.mods.galacticraft.api.world.IGalacticraftWorldProvider;
import micdoodle8.mods.galacticraft.api.world.OxygenHooks;
import net.minecraft.block.Block;
import net.minecraft.world.World;

/**
 * For when Galacticraft is loaded.
 */
public class ActiveGalacticraftHandler implements ISpaceHandler
{

    public static void load()
    {
        CampfireBackportCompat.galacticraftHandler = new ActiveGalacticraftHandler();
    }

    @Override
    public boolean canGetDimensionProperties(World world)
    {
        return world.provider instanceof IGalacticraftWorldProvider;
    }

    @Override
    public boolean atmosphericCombustion(World world)
    {
        return !OxygenHooks.noAtmosphericCombustion(world.provider);
    }

    @Override
    public boolean localizedCombustion(World world, Block block, int x, int y, int z)
    {
        if (!world.doChunksNearChunkExist(x, y, z, 9)) // if the adjacent chunks aren't loaded, an oxygen bubble distributor in them won't be loaded either...
            return true;

        return OxygenHooks.checkTorchHasOxygen(world, block, x, y, z);
    }

    @Override
    public float getGravityMultiplier(World world)
    {
        if (world.provider instanceof IGalacticraftWorldProvider)
            return ((IGalacticraftWorldProvider) world.provider).getFallDamageModifier();
        else
            return 1.0F;
    }

    @Override
    public float getAtmosphereDensity(World world, int y)
    {
        if (world.provider instanceof IGalacticraftWorldProvider)
            return (float) ((IGalacticraftWorldProvider) world.provider).getFuelUsageMultiplier();
        else
            return 1.0F;
    }

}
