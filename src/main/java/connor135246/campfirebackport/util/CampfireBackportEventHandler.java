package connor135246.campfirebackport.util;

import org.lwjgl.opengl.GL11;

import connor135246.campfirebackport.client.particle.EntityBigSmokeFX;
import connor135246.campfirebackport.client.particle.EntityBigSmokeFX.EntityBigSmokeFXConstructingEvent;
import connor135246.campfirebackport.common.CommonProxy;
import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.blocks.BlockCampfire.CampfireStateChangeEvent;
import connor135246.campfirebackport.common.compat.CampfireBackportCompat;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.config.ConfigNetworkManager;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

public class CampfireBackportEventHandler
{

    /**
     * Renders campfire smoke particles.
     */
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

    /**
     * Applies changes when config is changed via in-game GUI.
     */
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

    /**
     * Sends server configs to players logging in to a dedicated server.
     */
    @SubscribeEvent
    public void onPlayerLogin(PlayerLoggedInEvent event)
    {
        if (event.player instanceof EntityPlayerMP && !((EntityPlayerMP) event.player).mcServer.isSinglePlayer())
        {
            CommonProxy.modlog.info(StringParsers.translatePacket("send_config"));
            CommonProxy.simpleNetwork.sendTo(new ConfigNetworkManager.SendConfigMessage(), (EntityPlayerMP) event.player);
        }
    }

    /**
     * Restores client configs when the player disconnects from a dedicated server.
     */
    @SubscribeEvent
    public void onClientDisconnect(ClientDisconnectionFromServerEvent event)
    {
        if (!event.manager.isLocalChannel())
        {
            CommonProxy.modlog.info(StringParsers.translatePacket("restore_config"));
            CampfireBackportConfig.doConfig(0, true);
        }
    }

    /**
     * Sets the player's respawn point to the campfire when they sneak-right-click it with an empty hand, if the relevant config options are set.
     */
    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        if (event.entityPlayer != null && !event.entityPlayer.worldObj.isRemote && CampfireBackportConfig.spawnpointable != EnumCampfireType.NEITHER
                && event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK && event.entityPlayer.isSneaking()
                && event.entityPlayer.getCurrentEquippedItem() == null)
        {
            Block block = event.entityPlayer.worldObj.getBlock(event.x, event.y, event.z);

            if (block instanceof BlockCampfire && ((BlockCampfire) block).isLit() && CampfireBackportConfig.spawnpointable.matches((BlockCampfire) block))
            {
                event.entityPlayer.addChatComponentMessage(new ChatComponentTranslation(Reference.MODID + ".set_spawn"));
                event.entityPlayer.setSpawnChunk(new ChunkCoordinates(event.x, event.y, event.z), false);
                event.setCanceled(true);
            }
        }
    }

    /**
     * Burns out the campfire the player is respawning at, if the relevant config options are set.
     */
    @SubscribeEvent
    public void onPlayerRespawn(PlayerRespawnEvent event)
    {
        if (!event.player.worldObj.isRemote && CampfireBackportConfig.spawnpointable != EnumCampfireType.NEITHER)
        {
            ChunkCoordinates bedlocation = event.player.getBedLocation(event.player.dimension);

            if (bedlocation != null)
            {
                TileEntity tile = event.player.worldObj.getTileEntity(bedlocation.posX, bedlocation.posY, bedlocation.posZ);
                if (tile instanceof TileEntityCampfire)
                {
                    TileEntityCampfire ctile = ((TileEntityCampfire) tile);

                    if (ctile.isLit() && CampfireBackportConfig.burnOutOnRespawn.matches(ctile) && ctile.canBurnOut())
                        ctile.burnOutOrToNothing();
                }
            }
        }
    }

    /**
     * When a campfire is attempting to be ignited but there's no oxygen, it fails. Galacticraft / Advanced Rocketry compatibility.
     */
    @SubscribeEvent
    public void onCampfireStateChange(CampfireStateChangeEvent event)
    {
        if (!((BlockCampfire) event.block).isLit() && !CampfireBackportCompat.hasOxygen(event.world, event.block, event.x, event.y, event.z))
        {
            event.useGoods = false;
            event.setCanceled(true);
        }
    }

    /**
     * Makes smoke from campfires move differently depending on the atmosphere. Galacticraft / Advanced Rocketry compatibility.
     */
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onEntityBigSmokeFXConstructing(EntityBigSmokeFXConstructingEvent event)
    {
        event.particleGravity *= CampfireBackportCompat.getGravityMultiplier(event.entity.worldObj);

        event.motionY *= 1 / MathHelper.clamp_float(CampfireBackportCompat.getAtmosphereDensity(event.entity.worldObj,
                MathHelper.floor_double(event.entity.posY)), 0.25F, 8.0F);
    }

}
