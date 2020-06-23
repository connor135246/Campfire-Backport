package connor135246.campfirebackport.client.compat;

import java.util.List;

import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
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
        TileEntity tileent = accessor.getTileEntity();
        if (tileent instanceof TileEntityCampfire)
        {
            NBTTagCompound data = accessor.getNBTData();
            NBTTagList items = data.getTagList("Items", 10);

            if (items.tagCount() == 0)
            {
                tooltip.add(EnumChatFormatting.GRAY + "" + EnumChatFormatting.ITALIC + "Empty");
            }
            else
            {
                int[] cookTimes = data.getIntArray("CookingTimes");
                int[] cookTotalTimes = data.getIntArray("CookingTotalTimes");

                for (int i = 0; i < items.tagCount(); ++i)
                {
                    NBTTagCompound compound = items.getCompoundTagAt(i);
                    byte slot = compound.getByte("Slot");
                    if (slot >= 0 && slot < 4)
                    {
                        ItemStack invStack = ItemStack.loadItemStackFromNBT(compound);
                        int percentCooked = Math.min(Math.round((((float)cookTimes[slot]) / ((float)cookTotalTimes[slot])) * 100F), 100);
                        tooltip.add(EnumChatFormatting.GRAY + invStack.getDisplayName() + " (" + percentCooked + "%)");
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
