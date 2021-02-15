package connor135246.campfirebackport.common.compat.advancedrocketry;

import connor135246.campfirebackport.common.compat.CampfireBackportCompat;
import connor135246.campfirebackport.common.compat.CampfireBackportCompat.ISpaceHandler;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import zmaster587.advancedRocketry.api.AdvancedRocketryAPI;
import zmaster587.advancedRocketry.atmosphere.AtmosphereHandler;

/**
 * For when AdvancedRocketry is loaded.
 */
public class ActiveAdvancedRocketryHandler implements ISpaceHandler
{

    public static void load()
    {
        CampfireBackportCompat.advancedRocketryHandler = new ActiveAdvancedRocketryHandler();
    }

    @Override
    public boolean canGetDimensionProperties(World world)
    {
        return AdvancedRocketryAPI.dimensionManager.isDimensionCreated(world.provider.dimensionId);
    }

    @Override
    public boolean atmosphericCombustion(World world)
    {
        return AdvancedRocketryAPI.dimensionManager.getDimensionProperties(world.provider.dimensionId).getAtmosphere().allowsCombustion();
    }

    @Override
    public boolean localizedCombustion(World world, Block block, int x, int y, int z)
    {
        // why is this not in the API
        AtmosphereHandler handler = AtmosphereHandler.getOxygenHandler(world.provider.dimensionId);

        if (handler != null)
            return handler.getAtmosphereType(x, y, z).allowsCombustion();
        else
            return true;
    }

    @Override
    public float getGravityMultiplier(World world)
    {
        return AdvancedRocketryAPI.dimensionManager.getDimensionProperties(world.provider.dimensionId).getGravitationalMultiplier();
    }

    @Override
    public float getAtmosphereDensity(World world, int x, int y, int z)
    {
        return AdvancedRocketryAPI.dimensionManager.getDimensionProperties(world.provider.dimensionId).getAtmosphereDensityAtHeight(y);
    }

}
