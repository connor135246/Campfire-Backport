package connor135246.campfirebackport;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

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
public class CampfireBackportMixins implements IFMLLoadingPlugin
{

    public static final String NAME = "campfirebackportmixins", VERSION = "1.7.10";

    public static final Logger coreLog = LogManager.getLogger(NAME);

    public static File mcDir;
    public static File modsDir;

    public static Configuration config;
    public static boolean mixins = true;
    public static boolean vanillaMixins = true;
    public static boolean witcheryMixins = true;
    public static boolean thaumcraftMixins = true;

    public CampfireBackportMixins()
    {
        doConfig();

        if (mixins && (vanillaMixins || witcheryMixins || thaumcraftMixins))
        {
            MixinBootstrap.init();

            if (vanillaMixins)
                Mixins.addConfiguration("campfirebackport.mixin.json");

            mcDir = (File) FMLInjectionData.data()[6];
            modsDir = new File(mcDir, "mods");

            if (modsDir.isDirectory())
            {
                // hacky thing we do to make mixins work on witchery: load the witchery .jar early
                // thanks to ClientHax & Rongmario
                if (witcheryMixins && findMod("Witchery", true))
                    Mixins.addConfiguration("campfirebackport.mixin.witchery.json");

                // thaumcraft has a coremod (it downloads baubles), so its .jar is already loaded
                if (thaumcraftMixins && findMod("Thaumcraft", false))
                    Mixins.addConfiguration("campfirebackport.mixin.thaumcraft.json");
            }
            else
            {
                coreLog.error("Could not find mods directory. Mod mixins can't be applied.");
            }
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

        mixins = config.get("Mixins", Configuration.CATEGORY_GENERAL, true,
                "Set to false to disable all mixins.").setRequiresMcRestart(true).getBoolean();
        vanillaMixins = config.get("Vanilla Mixins", Configuration.CATEGORY_GENERAL, true,
                "Set to false to disable vanilla mixins:\nEntityPotion, EntitySmallFireball, PathFinder").setRequiresMcRestart(true).getBoolean();
        witcheryMixins = config.get("Witchery Mixins", Configuration.CATEGORY_GENERAL, true,
                "Set to false to disable Witchery mixins:\nTileEntityCauldron, TileEntityKettle").setRequiresMcRestart(true).getBoolean();
        thaumcraftMixins = config.get("Thaumcraft Mixins", Configuration.CATEGORY_GENERAL, true,
                "Set to false to disable Thaumcraft mixins:\nTileCrucible, TileThaumatorium").setRequiresMcRestart(true).getBoolean();

        config.save();
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
        FilenameFilter modJarsFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name)
            {
                return name.toLowerCase().contains(modname.toLowerCase()) && name.endsWith(".jar");
            }
        };

        boolean result = false;

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
                    if (loadEarly)
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

}
