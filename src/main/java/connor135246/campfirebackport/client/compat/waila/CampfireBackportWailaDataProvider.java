package connor135246.campfirebackport.client.compat.waila;

import java.util.List;

import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.util.Reference;
import connor135246.campfirebackport.util.StringParsers;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

public class CampfireBackportWailaDataProvider implements IWailaDataProvider
{

    public static void register(IWailaRegistrar reg)
    {
        reg.registerBodyProvider(new CampfireBackportWailaDataProvider(), TileEntityCampfire.class);
    }

    @Override
    public NBTTagCompound getNBTData(EntityPlayerMP playerMP, TileEntity tileent, NBTTagCompound tag, World world, int arg4, int arg5, int arg6)
    {
        return tag;
    }

    @Override
    public List<String> getWailaBody(ItemStack wailaStack, List<String> tooltip, IWailaDataAccessor accessor, IWailaConfigHandler arg3)
    {
        if (accessor.getTileEntity() instanceof TileEntityCampfire)
        {
            TileEntityCampfire ctile = (TileEntityCampfire) accessor.getTileEntity();

            if (accessor.getPlayer().isSneaking())
            {
                if (ctile.hasCustomInventoryName())
                    tooltip.add(" \"" + ctile.getInventoryName() + "\"");

                String direction;
                switch (accessor.getMetadata())
                {
                default:
                    direction = "north";
                    break;
                case 5:
                    direction = "east";
                    break;
                case 3:
                    direction = "south";
                    break;
                case 4:
                    direction = "west";
                    break;
                }
                tooltip.add("  " + StringParsers.translateWAILA("facing") + " " + EnumChatFormatting.WHITE + StringParsers.translateWAILA(direction));

                tooltip.add("  " + StringParsers.translateWAILA("signal_fire") + " "
                        + (ctile.isSignalFire() ? EnumChatFormatting.GREEN + StringParsers.translateWAILA("yes")
                                : EnumChatFormatting.RED + StringParsers.translateWAILA("no")));
            }

            for (int slot = 0; slot < ctile.getSizeInventory(); ++slot)
            {
                ItemStack stack = ctile.getStackInSlot(slot);
                if (stack != null)
                {
                    int cookTime = ctile.getCookingTimeInSlot(slot);
                    int cookTotalTime = ctile.getCookingTotalTimeInSlot(slot);

                    int percentCooked = Math.min(Math.round((((float) cookTime) / ((float) cookTotalTime)) * 100F), 100);
                    tooltip.add("-" + stack.getDisplayName() + " (" + (cookTime > cookTotalTime ? EnumChatFormatting.ITALIC : "")
                            + percentCooked + "%" + EnumChatFormatting.RESET + ")");
                }
            }

            if (CampfireBackportBlocks.isLitCampfire(accessor.getBlock()) && ctile.canBurnOut())
            {
                if (ctile.getBaseBurnOutTimer() > -1)
                    tooltip.add(TileEntityCampfire.getBurnOutTip(ctile.getLife(), ctile.getStartingLife()));
                if (ctile.getRainAndSky() && CampfireBackportConfig.putOutByRain.matches(ctile))
                    tooltip.add(EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.ITALIC
                            + StatCollector.translateToLocal(Reference.MODID + ".tooltip.rained_out"));
            }
        }

        return tooltip;
    }

    @Override
    public List<String> getWailaHead(ItemStack stack, List<String> tooltip, IWailaDataAccessor accessor, IWailaConfigHandler arg3)
    {
        return tooltip;
    }

    @Override
    public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler arg1)
    {
        return null;
    }

    @Override
    public List<String> getWailaTail(ItemStack stack, List<String> tooltip, IWailaDataAccessor accessor, IWailaConfigHandler arg3)
    {
        return tooltip;
    }

}
