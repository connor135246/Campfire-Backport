package connor135246.campfirebackport.client.compat.waila;

import java.util.List;

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

    public static final IWailaDataProvider INSTANCE = new CampfireBackportWailaDataProvider();

    public static void register(IWailaRegistrar reg)
    {
        reg.registerBodyProvider(INSTANCE, TileEntityCampfire.class);
        reg.registerNBTProvider(INSTANCE, TileEntityCampfire.class);
    }

    /**
     * Sync cooking times when the player looks at a campfire. Everything else is already synced whenever it changes.
     */
    @Override
    public NBTTagCompound getNBTData(EntityPlayerMP playerMP, TileEntity tileent, NBTTagCompound tag, World world, int arg4, int arg5, int arg6)
    {
        if (tileent instanceof TileEntityCampfire)
        {
            TileEntityCampfire ctile = (TileEntityCampfire) tileent;

            int[] cookingTimes = new int[ctile.getSizeInventory()];
            for (int slot = 0; slot < cookingTimes.length; ++slot)
                cookingTimes[slot] = ctile.getCookingTimeInSlot(slot);
            tag.setIntArray(TileEntityCampfire.KEY_CookingTimes, cookingTimes);

            int[] cookingTotalTimes = new int[ctile.getSizeInventory()];
            for (int slot = 0; slot < cookingTotalTimes.length; ++slot)
                cookingTotalTimes[slot] = ctile.getCookingTotalTimeInSlot(slot);
            tag.setIntArray(TileEntityCampfire.KEY_CookingTotalTimes, cookingTotalTimes);
        }
        return tag;
    }

    /**
     * Displays Custom Name, facing direction, Signal Fire state, Inventory (and Cooking Times / Cooking Total Times), Burn Out Tip, and Rain Out Tip.
     */
    @Override
    public List<String> getWailaBody(ItemStack wailaStack, List<String> tooltip, IWailaDataAccessor accessor, IWailaConfigHandler arg3)
    {
        if (accessor.getTileEntity() instanceof TileEntityCampfire)
        {
            TileEntityCampfire ctile = (TileEntityCampfire) accessor.getTileEntity();
            NBTTagCompound data = accessor.getNBTData();

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

            int[] cookingTimes = data.getIntArray(TileEntityCampfire.KEY_CookingTimes);
            int[] cookingTotalTimes = data.getIntArray(TileEntityCampfire.KEY_CookingTotalTimes);
            for (int slot = 0; slot < ctile.getSizeInventory(); ++slot)
            {
                ItemStack stack = ctile.getStackInSlot(slot);
                if (stack != null)
                {
                    String tip = "-" + stack.getDisplayName();

                    if (slot < cookingTimes.length && slot < cookingTotalTimes.length)
                    {
                        tip += " (";

                        int cookTime = cookingTimes[slot];
                        int cookTotalTime = cookingTotalTimes[slot];

                        if (cookTime >= cookTotalTime)
                            tip += EnumChatFormatting.ITALIC + "100%" + EnumChatFormatting.RESET;
                        else
                            tip += Math.min(Math.round((((float) cookTime) / ((float) cookTotalTime)) * 100F), 99) + "%";

                        tip += ")";
                    }

                    tooltip.add(tip);
                }
            }

            if (ctile.isLit())
            {
                boolean canBurnOut = ctile.canBurnOut();
                boolean canRainOut = canBurnOut && ctile.getRainAndSky() && CampfireBackportConfig.putOutByRain.matches(ctile);
                boolean showBurnOutTip = (canBurnOut && ctile.getBaseBurnOutTimer() > -1) || (canRainOut && ctile.isOnReignitionCooldown());
                boolean showRainOutTip = canRainOut && !ctile.isOnReignitionCooldown();
                if (ctile.hasClientReignition() && (showBurnOutTip || showRainOutTip))
                    tooltip.add(TileEntityCampfire.getBurnOutTip(39, -1));
                else
                {
                    if (showBurnOutTip)
                        tooltip.add(TileEntityCampfire.getBurnOutTip(ctile.getLife(), ctile.getStartingLife()));
                    if (showRainOutTip)
                        tooltip.add(EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.ITALIC
                                + StatCollector.translateToLocal(Reference.MODID + ".tooltip.rained_out"));
                }
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
