package connor135246.campfirebackport.util;

import org.lwjgl.opengl.GL11;

import connor135246.campfirebackport.client.particle.EntityBigSmokeFX;
import connor135246.campfirebackport.common.CommonProxy;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.config.ConfigNetworkManager;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderWorldLastEvent;

public class CampfireBackportEventHandler
{

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event)
    {
        // botania license attribution clause, etc etc
        // damn you vazkii!!!!! but thanks

        Profiler profiler = Minecraft.getMinecraft().mcProfiler;

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.003921569F);

        profiler.startSection("cb_bigsmokeparticles");
        EntityBigSmokeFX.dispatchQueuedRenders(Tessellator.instance);
        profiler.endSection();

        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
        GL11.glDisable(GL11.GL_BLEND);
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
    {
        if (event.modID.equals(Reference.MODID))
        {
            if (!CampfireBackportConfig.useDefaultConfig)
            {
                CampfireBackportConfig.doConfig(1, false);
            }
            else
            {
                CommonProxy.modlog.warn(StatCollector.translateToLocal(Reference.MODID + ".config.rename_old_config.error"));
                CommonProxy.modlog.warn(StatCollector.translateToLocal(Reference.MODID + ".config.rename_old_config.error.1"));
                CommonProxy.modlog.warn(StatCollector.translateToLocal(Reference.MODID + ".config.rename_old_config.error.2"));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerLoggedInEvent event)
    {
        if (event.player instanceof EntityPlayerMP && !((EntityPlayerMP) event.player).mcServer.isSinglePlayer())
        {
            CommonProxy.modlog.info(StringParsers.translatePacket("send_config"));
            CommonProxy.simpleNetwork.sendTo(new ConfigNetworkManager.SendConfigMessage(), (EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onClientDisconnect(ClientDisconnectionFromServerEvent event)
    {
        if (!event.manager.isLocalChannel())
        {
            CommonProxy.modlog.info(StringParsers.translatePacket("restore_config"));
            CampfireBackportConfig.doConfig(0, true);
        }
    }

}
