package connor135246.campfirebackport;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.google.common.collect.ObjectArrays;

import cpw.mods.fml.relauncher.CoreModManager;
import cpw.mods.fml.relauncher.FMLInjectionData;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.common.config.Configuration;

/**
 * Detects mods that we want to apply mixins to.
 */
@IFMLLoadingPlugin.MCVersion(CampfireBackportMixins.VERSION)
@IFMLLoadingPlugin.Name(CampfireBackportMixins.NAME)
public class CampfireBackportMixins implements IFMLLoadingPlugin, IMixinConfigPlugin
{

    public static final String NAME = "campfirebackportmixins", VERSION = "1.7.10";

    public static final Logger coreLog = LogManager.getLogger(NAME);

    public static File mcDir;
    public static File modsDir;

    public static Configuration config;
    public static boolean mixins;
    public static boolean[] vanillaMixins = new boolean[4];
    public static boolean[] witcheryMixins = new boolean[7];
    public static boolean[] thaumcraftMixins = new boolean[3];
    public static boolean enableLoadEarly;
    public static boolean skipModCheck;

    static
    {
        getConfigDefaults();
    }

    public static void getConfigDefaults()
    {
        mixins = true;
        Arrays.fill(vanillaMixins, true);
        Arrays.fill(witcheryMixins, true);
        Arrays.fill(thaumcraftMixins, true);
        enableLoadEarly = true;
        skipModCheck = false;
    }

    public CampfireBackportMixins()
    {
        mcDir = (File) FMLInjectionData.data()[6];

        doConfig();

        if (mixins && (vanillaMixins[0] || witcheryMixins[0] || thaumcraftMixins[0]))
        {
            coreLog.info("Mixins are enabled!");

            MixinBootstrap.init();
            Mixins.addConfiguration("campfirebackport.mixin.json");

            if (vanillaMixins[0])
                coreLog.info("Vanilla mixins will be applied.");

            modsDir = new File(mcDir, "mods");

            if (modsDir.isDirectory() || skipModCheck)
            {
                // hacky thing we do to make mixins work on witchery: load the witchery .jar early
                // thanks to ClientHax & Rongmario
                witcheryMixins[0] = witcheryMixins[0] && findMod("Witchery", true);

                // thaumcraft has a coremod (it downloads baubles), so its .jar is already loaded
                thaumcraftMixins[0] = thaumcraftMixins[0] && findMod("Thaumcraft", false);
            }
            else
            {
                coreLog.error("Could not find mods directory. Mod mixins can't be applied.");
            }
        }
        else
        {
            coreLog.info("Mixins are disabled.");
        }
    }

    /**
     * Config for mixins.<br>
     * Almost all mixins apply to code within a !world.isRemote check, so it's probably not particularly important to make sure they're synchronized between client and server. The
     * only mixin that isn't directly below a !world.isRemote check is PathFinder, but that's probably okay since 99.9% of the time it will be called on the server.
     */
    private void doConfig()
    {
        config = new Configuration(new File(mcDir, "config" + File.separatorChar + NAME + ".cfg"));

        config.load();

        final String CAT_MIXIN = "mixins", CAT_VANILLA = "vanilla mixins", CAT_WITCHERY = "witchery mixins", CAT_THAUMCRAFT = "thaumcraft mixins";

        mixins = getConfigGeneral(CAT_MIXIN, "all");
        vanillaMixins[0] = getConfigGeneral(CAT_VANILLA, "all vanilla");
        vanillaMixins[1] = getConfigMixin(CAT_VANILLA, "EntityPotion");
        vanillaMixins[2] = getConfigMixin(CAT_VANILLA, "EntitySmallFireball");
        vanillaMixins[3] = getConfigMixin(CAT_VANILLA, "PathFinder");
        witcheryMixins[0] = getConfigGeneral(CAT_WITCHERY, "all Witchery");
        witcheryMixins[1] = getConfigMixin(CAT_WITCHERY, "TileEntityCauldron");
        witcheryMixins[2] = getConfigMixin(CAT_WITCHERY, "TileEntityKettle");
        witcheryMixins[3] = getConfigMixin(CAT_WITCHERY, "brews.EntitySplatter");
        witcheryMixins[4] = getConfigMixin(CAT_WITCHERY, "brews.Extinguish");
        witcheryMixins[5] = getConfigMixin(CAT_WITCHERY, "symbols.Aguamenti");
        witcheryMixins[6] = getConfigMixin(CAT_WITCHERY, "symbols.Incendio");
        thaumcraftMixins[0] = getConfigGeneral(CAT_THAUMCRAFT, "all Thaumcraft");
        thaumcraftMixins[1] = getConfigMixin(CAT_THAUMCRAFT, "TileCrucible");
        thaumcraftMixins[2] = getConfigMixin(CAT_THAUMCRAFT, "TileThaumatorium");
        enableLoadEarly = config.get(CAT_MIXIN, "Enable Early Mod Loading", true,
                "Enables early mod loading trick in order to apply mixins to certain mods. \nApplies to: Witchery").setRequiresMcRestart(true).getBoolean();
        skipModCheck = config.get(CAT_MIXIN, "Skip Mod Checking", false,
                "If enabled, skips checking for mods before trying to apply mod compatibility mixins. Any mod mixins that are enabled will always try to apply!")
                .setRequiresMcRestart(true).getBoolean();

        config.save();
    }

    private boolean getConfigGeneral(String category, String commentpart)
    {
        // reminder for next time: it's category and then name, not name and then category...
        return config.get(category, Configuration.CATEGORY_GENERAL, true, "Set to false to disable " + commentpart + " mixins")
                .setRequiresMcRestart(true).getBoolean();
    }

    private boolean getConfigMixin(String category, String name)
    {
        return config.get(category, name, true, "Set to false to disable " + name + " mixin").setRequiresMcRestart(true).getBoolean();
    }

    @Override
    public List<String> getMixins()
    {
        List<String> list = new ArrayList<String>();
        if (mixins)
        {
            if (vanillaMixins[0])
            {
                if (vanillaMixins[1])
                    list.add("MixinEntityPotion");
                if (vanillaMixins[2])
                    list.add("MixinEntitySmallFireball");
                if (vanillaMixins[3])
                    list.add("MixinPathFinder");
            }
            if (witcheryMixins[0])
            {
                if (witcheryMixins[1])
                    list.add("witchery.MixinTileEntityCauldron");
                if (witcheryMixins[2])
                    list.add("witchery.MixinTileEntityKettle");
                if (witcheryMixins[3])
                    list.add("witchery.brews.MixinEntitySplatter");
                if (witcheryMixins[4])
                    list.add("witchery.brews.MixinExtinguish");
                if (witcheryMixins[5])
                    list.add("witchery.symbols.MixinAguamenti");
                if (witcheryMixins[6])
                    list.add("witchery.symbols.MixinIncendio");
            }
            if (thaumcraftMixins[0])
            {
                if (thaumcraftMixins[1])
                    list.add("thaumcraft.MixinTileCrucible");
                if (thaumcraftMixins[2])
                    list.add("thaumcraft.MixinTileThaumatorium");
            }
        }
        return list;
    }

    /**
     * Finds the mod .jar. Then, if loadEarly is true, loads the mod jar to the class loader and tells the CoreModManager about it.<br>
     * <br>
     * The jar finding bit copy-pasted from {@link CoreModManager#discoverCoreMods}.<br>
     * The jar loading bit copy-pasted from ClientHax/Rongmario:<br>
     * {@code https://github.com/clienthax/PixelmonBridge2/blob/master/src/main/java/moe/clienthax/pixelmonbridge/impl/PixelmonBridgeCoreMod.java } <br>
     * {@code https://github.com/LoliKingdom/KemonoFixer/blob/master/src/main/java/com/rong/kemonofixer/KemonoFixerCoreMod.java }
     * 
     * @return true if the mod was found (and loaded), false otherwise
     */
    private boolean findMod(String modname, boolean loadEarly)
    {
        boolean result = false;

        if (skipModCheck)
            result = true;
        else
        {
            FilenameFilter modJarsFilter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name)
                {
                    return name.toLowerCase().contains(modname.toLowerCase()) && name.endsWith(".jar");
                }
            };

            try
            {
                File[] modfiles = modsDir.listFiles(modJarsFilter);

                File versionedModsDir = new File(modsDir, VERSION);
                if (versionedModsDir.isDirectory())
                {
                    File[] versionedModfiles = versionedModsDir.listFiles(modJarsFilter);
                    modfiles = ObjectArrays.concat(modfiles, versionedModfiles, File.class);
                }

                if (modfiles.length == 0)
                    coreLog.info("Did not find the mod \"" + modname + "\".");
                else
                {
                    for (File modfile : modfiles)
                    {
                        if (loadEarly && enableLoadEarly)
                        {
                            try
                            {
                                ((LaunchClassLoader) this.getClass().getClassLoader()).addURL(modfile.toURI().toURL());
                                CoreModManager.getReparseableCoremods().add(modfile.getName());
                                coreLog.info("Successfully loaded \"" + modfile.getName() + "\" early.");
                                result = true;
                            }
                            catch (Exception excep)
                            {
                                coreLog.error("An error occured when trying to load \"" + modfile.getName() + "\" early.");
                            }
                        }
                        else
                        {
                            coreLog.info("Found \"" + modfile.getName() + "\".");
                            result = true;
                            break;
                        }
                    }
                }
            }
            catch (Exception excep)
            {
                coreLog.error("An error occured when searching for the mod \"" + modname + "\".");
            }
        }

        if (result)
            coreLog.info("\"" + modname + "\" mixins will be applied.");
        else
            coreLog.info("\"" + modname + "\" mixins will NOT be applied.");

        return result;
    }

    //

    @Override
    public String[] getASMTransformerClass()
    {
        return new String[] {};
    }

    @Override
    public String getModContainerClass()
    {
        return null;
    }

    @Override
    public String getSetupClass()
    {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data)
    {

    }

    @Override
    public String getAccessTransformerClass()
    {
        return null;
    }

    //

    @Override
    public void onLoad(String mixinPackage)
    {

    }

    @Override
    public String getRefMapperConfig()
    {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
    {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets)
    {

    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
    {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
    {

    }
}
