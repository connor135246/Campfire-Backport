package connor135246.campfirebackport.util;

import org.lwjgl.opengl.GL11;

import connor135246.campfirebackport.client.particle.EntityBigSmokeFX;
import connor135246.campfirebackport.common.CommonProxy;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderWorldLastEvent;

public class CampfireBackportEventHandler
{
    public static int bigSmokeCount = 0;

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onRenderWorldLast(RenderWorldLastEvent event)
    {
        // botania license attribution clause, etc etc
        // damn you vazkii!!!!! but thanks

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
                CommonProxy.modlog.warn(StatCollector.translateToLocal(Reference.MODID + ".config.rename_old_config.error"));
                CommonProxy.modlog.warn(StatCollector.translateToLocal(Reference.MODID + ".preinit.rename_old_config.error.1"));
                CommonProxy.modlog.warn(StatCollector.translateToLocal(Reference.MODID + ".preinit.rename_old_config.error.2"));
            }
        }
    }

}
