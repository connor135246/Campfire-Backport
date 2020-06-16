package connor135246.campfirebackport.util;

import org.lwjgl.opengl.GL11;

import connor135246.campfirebackport.CampfireBackport;
import connor135246.campfirebackport.CampfireBackportConfig;
import connor135246.campfirebackport.client.particle.EntityBigSmokeFX;
import connor135246.campfirebackport.common.CommonProxy;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.profiler.Profiler;
import net.minecraftforge.client.event.RenderWorldLastEvent;

public class CampfireBackportEventHandler
{
    public static int bigSmokeCount = 0;

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onRenderWorldLast(RenderWorldLastEvent event)
    {
        Tessellator tessellator = Tessellator.instance;

        Profiler profiler = Minecraft.getMinecraft().mcProfiler;

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.003921569F);

        profiler.startSection("cb_bigsmokeparticles");
        EntityBigSmokeFX.dispatchQueuedRenders(tessellator);
        profiler.endSection();

        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
        GL11.glDisable(GL11.GL_BLEND);
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
    {
        if (event.modID.equals(Reference.MODID))
        {
            if (!CommonProxy.useDefaultConfig)
            {
                CommonProxy.syncConfig();
            }
            else
            {
                CommonProxy.modlog.warn("You had an old (v1.3 or earlier) config file on game load! Config settings have changed a lot since then!");
                CommonProxy.modlog.warn("Delete or rename the old config file, then restart minecraft to get a new one.");
                CommonProxy.modlog.warn("No settings will be saved from the in-game config screen.");
            }
        }
    }
}
