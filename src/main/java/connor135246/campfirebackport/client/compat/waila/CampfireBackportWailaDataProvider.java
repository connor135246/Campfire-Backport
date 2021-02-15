package connor135246.campfirebackport.client.compat.waila;

import java.util.List;

import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.util.Reference;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
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
    public List<String> getWailaBody(ItemStack stack, List<String> tooltip, IWailaDataAccessor accessor, IWailaConfigHandler arg3)
    {
        if (accessor.getTileEntity() instanceof TileEntityCampfire)
        {
            if (accessor.getPlayer().isSneaking())
            {
                if (((TileEntityCampfire) accessor.getTileEntity()).hasCustomInventoryName())
                    tooltip.add(" \"" + ((TileEntityCampfire) accessor.getTileEntity()).getInventoryName() + "\"");

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

                tooltip.add("  " + StatCollector.translateToLocal(Reference.MODID + ".waila.facing") + " "
                        + StatCollector.translateToLocal(Reference.MODID + ".waila." + direction));
            }

            NBTTagCompound data = accessor.getNBTData();
            NBTTagList itemList = data.getTagList(TileEntityCampfire.KEY_Items, 10);

            if (itemList.tagCount() == 0)
                tooltip.add(EnumChatFormatting.ITALIC + StatCollector.translateToLocal(Reference.MODID + ".waila.empty"));
            else
            {
                int[] cookTimes = data.getIntArray(TileEntityCampfire.KEY_CookingTimes);
                int[] cookTotalTimes = data.getIntArray(TileEntityCampfire.KEY_CookingTotalTimes);

                for (int i = 0; i < itemList.tagCount(); ++i)
                {
                    NBTTagCompound itemCompound = itemList.getCompoundTagAt(i);
                    byte slot = itemCompound.getByte(TileEntityCampfire.KEY_Slot);
                    if (slot >= 0 && slot < 4)
                    {
                        ItemStack invStack = ItemStack.loadItemStackFromNBT(itemCompound);
                        int percentCooked = Math.min(Math.round((((float) cookTimes[slot]) / ((float) cookTotalTimes[slot])) * 100F), 100);
                        tooltip.add(invStack.getDisplayName() + " (" + (cookTimes[slot] > cookTotalTimes[slot] ? EnumChatFormatting.ITALIC : "")
                                + percentCooked + "%" + EnumChatFormatting.RESET + ")");
                    }
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
