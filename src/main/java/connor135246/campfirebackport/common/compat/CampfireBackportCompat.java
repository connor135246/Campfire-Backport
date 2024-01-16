package connor135246.campfirebackport.common.compat;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import connor135246.campfirebackport.common.CommonProxy;
import connor135246.campfirebackport.util.Reference;
import cpw.mods.fml.common.Loader;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

public class CampfireBackportCompat
{

    public static boolean isMineTweaker3Loaded = false,
            isGalacticraftLoaded = false,
            isAdvancedRocketryLoaded = false,
            isThaumcraftLoaded = false,
            isBotaniaLoaded = false,
            isNetherliciousLoaded = false;

    /**
     * checks for mods in preInit, before any content is registered
     */
    public static void preInit()
    {
        isGalacticraftLoaded = Loader.isModLoaded("GalacticraftCore");
        isAdvancedRocketryLoaded = Loader.isModLoaded("advancedRocketry");
        isThaumcraftLoaded = Loader.isModLoaded("Thaumcraft");
        isBotaniaLoaded = Loader.isModLoaded("Botania");
        isMineTweaker3Loaded = Loader.isModLoaded("MineTweaker3");
        isNetherliciousLoaded = Loader.isModLoaded("netherlicious");
    }

    /**
     * prepares specific mod compatibility in postInit
     */
    public static void postInit()
    {
        if (isGalacticraftLoaded)
            loadModHandler("GalacticraftCore", "handlers.GalacticraftHandler");

        if (isAdvancedRocketryLoaded)
            loadModHandler("advancedRocketry", "handlers.AdvancedRocketryHandler");

        if (isThaumcraftLoaded)
            loadModHandler("Thaumcraft", "handlers.ThaumcraftHandler");

        if (isBotaniaLoaded)
            loadModHandler("Botania", "botania.BotaniaHandler");

        if (isMineTweaker3Loaded)
            loadModHandler("MineTweaker3", "crafttweaker.CampfireBackportCraftTweaking");
    }

    private static void loadModHandler(String modid, String classnamepart)
    {
        try
        {
            Class.forName(Reference.MOD_PACKAGE + ".common.compat." + classnamepart).getDeclaredMethod("load").invoke(null);
        }
        catch (Exception excep)
        {
            logError(modid);
        }
    }

    public static void logError(String modid)
    {
        CommonProxy.modlog.error(StatCollector.translateToLocalFormatted(Reference.MODID + ".compat.error", modid));
    }

    // Handlers

    public static ISpaceHandler galacticraftHandler = new DummySpaceHandler();
    public static ISpaceHandler advancedRocketryHandler = new DummySpaceHandler();

    /**
     * A question for space handlers.
     */
    public static boolean hasOxygen(World world, Block block, int x, int y, int z)
    {
        return atmosphericCombustion(world) || localizedCombustion(world, block, x, y, z);
    }

    public static boolean atmosphericCombustion(World world)
    {
        if (galacticraftHandler.canGetDimensionProperties(world))
            return galacticraftHandler.atmosphericCombustion(world);
        else if (advancedRocketryHandler.canGetDimensionProperties(world))
            return advancedRocketryHandler.atmosphericCombustion(world);
        else
            return true;
    }

    public static boolean localizedCombustion(World world, Block block, int x, int y, int z)
    {
        return galacticraftHandler.localizedCombustion(world, block, x, y, z) || advancedRocketryHandler.localizedCombustion(world, block, x, y, z);
    }

    /**
     * A question for space handlers.
     */
    public static float getGravityMultiplier(World world)
    {
        return galacticraftHandler.canGetDimensionProperties(world) ? galacticraftHandler.getGravityMultiplier(world)
                : advancedRocketryHandler.canGetDimensionProperties(world) ? advancedRocketryHandler.getGravityMultiplier(world) : 1.0F;
    }

    /**
     * A question for space handlers.
     */
    public static float getAtmosphereDensity(World world, int y)
    {
        return advancedRocketryHandler.canGetDimensionProperties(world) ? advancedRocketryHandler.getAtmosphereDensity(world, y)
                : galacticraftHandler.canGetDimensionProperties(world) ? galacticraftHandler.getAtmosphereDensity(world, y) : 1.0F;
    }

    // Compat Interfaces and their Dummy Implementations
    // aka All the things that don't do anything should go in one place!

    // MineTweaker3 / CraftTweaker

    /**
     * An interface for holding a CraftTweaker IIngredient.
     */
    public static interface ICraftTweakerIngredient extends Comparable<ICraftTweakerIngredient>
    {

        /**
         * @return does the stack match this IIngredient?
         */
        public boolean matches(ItemStack stack, boolean inputSizeMatters);

        /**
         * @return whether the IIngredient has any transforms
         */
        public boolean hasTransforms();

        /**
         * @return the stack after transformations have been applied
         */
        public ItemStack applyTransform(ItemStack stack, EntityPlayer player);

        /**
         * @return list of example items for this IIngredient to display in NEI
         */
        public List<ItemStack> getItems();

        /**
         * @return extra lines of info to display when hovering over the IIngredient in NEI
         */
        public LinkedList<String> getNEITooltip();

        /**
         * @return the stack after this IIngredient's AbstractItemFunctions have done
         *         {@link connor135246.campfirebackport.common.compat.crafttweaker.AbstractItemFunction#modifyStackForDisplay} to it
         */
        public ItemStack modifyStackForDisplay(ItemStack stack);

        /**
         * @return true if the IIngredient is an IngredientAny or an IngredientAnyAdvanced
         */
        public boolean isWildcard();

        /**
         * @return true if the IIngredient had any {@link connor135246.campfirebackport.common.compat.crafttweaker.AbstractItemFunction} applied to it
         */
        public boolean hasFunctions();

        /**
         * @return an int representing the IIngredient's sort order compared to other types of IIngredients. larger number = later in the list.
         */
        public int getSortOrder();

    }

    /**
     * For when CraftTweaker isn't loaded.
     */
    public static class DummyCraftTweakerIngredient implements ICraftTweakerIngredient
    {

        public DummyCraftTweakerIngredient(Object unused)
        {
            ;
        }

        @Override
        public boolean matches(ItemStack stack, boolean inputSizeMatters)
        {
            return false;
        }

        @Override
        public boolean hasTransforms()
        {
            return false;
        }

        @Override
        public ItemStack applyTransform(ItemStack stack, EntityPlayer player)
        {
            return stack;
        }

        @Override
        public List<ItemStack> getItems()
        {
            return new ArrayList<ItemStack>(0);
        }

        @Override
        public LinkedList<String> getNEITooltip()
        {
            return new LinkedList<String>();
        }

        @Override
        public ItemStack modifyStackForDisplay(ItemStack stack)
        {
            return stack;
        }

        @Override
        public boolean isWildcard()
        {
            return false;
        }

        @Override
        public boolean hasFunctions()
        {
            return false;
        }

        @Override
        public int getSortOrder()
        {
            return 0;
        }

        @Override
        public int compareTo(ICraftTweakerIngredient other)
        {
            return 0;
        }

    }

    // Galacticraft / Advanced Rocketry

    /**
     * An interface for interacting with Galacticraft or Advanced Rocketry.
     */
    public static interface ISpaceHandler
    {

        /**
         * @return does this mod using this handler have information about this dimension's properties?
         */
        public boolean canGetDimensionProperties(World world);

        /**
         * @return do torches, fire, etc work in this dimension in general?
         */
        public boolean atmosphericCombustion(World world);

        /**
         * @return do torches, fire, etc work at this location due to localized oxygen (such as a sealed and oxygenated chamber or an oxygen bubble)?
         */
        public boolean localizedCombustion(World world, Block block, int x, int y, int z);

        /**
         * @return multiplier for gravity in this dimension
         */
        public float getGravityMultiplier(World world);

        /**
         * @return something like atmosphere density in this dimension at this height
         */
        public float getAtmosphereDensity(World world, int y);

    }

    /**
     * For when Galacticraft or Advanced Rocketry isn't loaded.
     */
    public static class DummySpaceHandler implements ISpaceHandler
    {
        @Override
        public boolean canGetDimensionProperties(World world)
        {
            return false;
        }

        @Override
        public boolean atmosphericCombustion(World world)
        {
            return false;
        }

        @Override
        public boolean localizedCombustion(World world, Block block, int x, int y, int z)
        {
            return false;
        }

        @Override
        public float getGravityMultiplier(World world)
        {
            return 1.0F;
        }

        @Override
        public float getAtmosphereDensity(World world, int y)
        {
            return 1.0F;
        }

    }

}
