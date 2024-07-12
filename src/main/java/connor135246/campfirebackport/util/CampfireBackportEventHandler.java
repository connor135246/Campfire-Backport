package connor135246.campfirebackport.util;

import connor135246.campfirebackport.client.particle.EntityBigSmokeFX.EntityBigSmokeFXConstructingEvent;
import connor135246.campfirebackport.common.CommonProxy;
import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.blocks.BlockCampfire.CampfireStateChangeEvent;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.common.compat.CampfireBackportCompat;
import connor135246.campfirebackport.common.items.ItemBlockCampfire;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.config.ConfigNetworkManager;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.event.FuelBurnTimeEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

public class CampfireBackportEventHandler
{

    /**
     * Registers the item textures, no matter which sprite sheet the item says it should be on.
     */
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onTextureStitchPre(TextureStitchEvent.Pre event)
    {
        if (event.map.getTextureType() == 1)
        {
            for (Block block : CampfireBackportBlocks.LIT_CAMPFIRES)
                ((ItemBlockCampfire) Item.getItemFromBlock(block)).registerIconsEvent(event.map);
            for (Block block : CampfireBackportBlocks.UNLIT_CAMPFIRES)
                ((ItemBlockCampfire) Item.getItemFromBlock(block)).registerIconsEvent(event.map);
        }
    }

    /**
     * Applies changes when config is changed via in-game GUI.
     */
    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
    {
        if (event.modID.equals(Reference.MODID))
        {
            if (!CampfireBackportConfig.useDefaults)
            {
                if (event.isWorldRunning && !Minecraft.getMinecraft().isSingleplayer())
                    CampfireBackportConfig.doConfig(10, false, false);
                else
                    CampfireBackportConfig.doConfig(14, false, false);
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
            CommonProxy.simpleNetwork.sendTo(new ConfigNetworkManager.SendMixinConfigMessage(), (EntityPlayerMP) event.player);
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
            ConfigNetworkManager.getMixinsHere();
            CampfireBackportConfig.doConfig(15, true, true);
        }
    }

    /**
     * Sets the player's respawn point to the campfire when they sneak-right-click it with an empty hand, if the relevant config options are set.
     */
    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        if (event.useBlock != Event.Result.DENY && event.entityPlayer != null && !CampfireBackportConfig.spawnpointableAltTrigger
                && CampfireBackportConfig.spawnpointable != EnumCampfireType.NEITHER && event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK
                && event.entityPlayer.isSneaking() && event.entityPlayer.getCurrentEquippedItem() == null)
        {
            Block block = event.world.getBlock(event.x, event.y, event.z);
            if (block instanceof BlockCampfire && ((BlockCampfire) block).setRespawnPoint(event.world, event.x, event.y, event.z, event.entityPlayer))
            {
                if (!event.world.isRemote)
                    event.setCanceled(true);
                event.entityPlayer.swingItem();
            }
        }
    }

    /**
     * Burns out the campfire the player is respawning at, if the relevant config options are set.
     */
    @SubscribeEvent
    public void onPlayerRespawn(PlayerRespawnEvent event)
    {
        if (!event.player.worldObj.isRemote && CampfireBackportConfig.spawnpointable != EnumCampfireType.NEITHER
                && CampfireBackportConfig.burnOutOnRespawn != EnumCampfireType.NEITHER)
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
     * Prevent campfires from being used as furnace fuel. (By default they're given 1.5 items since they have a Material of wood.)
     */
    @SubscribeEvent
    public void onFuelBurnTime(FuelBurnTimeEvent event)
    {
        if (event.fuel.getItem() instanceof ItemBlockCampfire && event.getResult() == Event.Result.DEFAULT)
        {
            event.burnTime = 0;
            event.setResult(Event.Result.DENY);
        }
    }

    /**
     * When a campfire is attempting to be ignited but there's water on top of it, it fails (if the config option is set). <br>
     * Galacticraft / Advanced Rocketry compatibility: When a campfire is attempting to be ignited but there's no oxygen, it fails.
     */
    @SubscribeEvent
    public void onCampfireStateChange(CampfireStateChangeEvent event)
    {
        if (!event.isCanceled() && event.mode == 1)
        {
            if (!CampfireBackportCompat.hasOxygen(event.world, event.block, event.x, event.y, event.z))
            {
                event.useGoods = false;
                event.setCanceled(true);
            }
            else if (event.block.waterCheck(event.world, event.x, event.y, event.z))
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
        if (!event.isCanceled())
        {
            event.particleGravity *= CampfireBackportCompat.getGravityMultiplier(event.entity.worldObj);

            event.motionY *= 1 / MathHelper.clamp_float(CampfireBackportCompat.getAtmosphereDensity(event.entity.worldObj,
                    MathHelper.floor_double(event.entity.posY)), 0.25F, 8.0F);
        }
    }

}
