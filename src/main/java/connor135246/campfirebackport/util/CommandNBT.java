package connor135246.campfirebackport.util;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * Prints the NBT of a held item or the NBT of a tile entity at x y z.
 */
public class CommandNBT implements ICommand
{

    @Override
    public int compareTo(Object o)
    {
        return 0;
    }

    @Override
    public String getCommandName()
    {
        return "campfirebackport_nbt";
    }

    @Override
    public String getCommandUsage(ICommandSender p_71518_1_)
    {
        return Reference.MODID + ".command.nbt.usage";
    }

    @Override
    public List getCommandAliases()
    {
        return null;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] arguments)
    {
        World world = sender.getEntityWorld();

        if (!world.isRemote)
        {
            if (arguments.length == 0)
            {
                EntityPlayerMP player;

                try
                {
                    player = CommandBase.getCommandSenderAsPlayer(sender);
                }
                catch (PlayerNotFoundException excep)
                {
                    throw new CommandException(Reference.MODID + ".command.nbt.not_a_player", new Object[0]);
                }

                if (player.getCurrentEquippedItem() != null)
                    sender.addChatMessage(new ChatComponentText("NBT: " + player.getCurrentEquippedItem().getTagCompound()));
                else
                    throw new CommandException(Reference.MODID + ".command.nbt.no_held_item", new Object[0]);
            }
            else if (arguments.length == 3)
            {
                int i = MathHelper.floor_double(CommandBase.func_110666_a(sender, sender.getPlayerCoordinates().posX, arguments[0]));
                int j = MathHelper.floor_double(CommandBase.func_110666_a(sender, sender.getPlayerCoordinates().posY, arguments[1]));
                int k = MathHelper.floor_double(CommandBase.func_110666_a(sender, sender.getPlayerCoordinates().posZ, arguments[2]));

                if (!world.blockExists(i, j, k))
                {
                    throw new CommandException(Reference.MODID + ".command.nbt.block_out_of_world", new Object[0]);
                }
                else
                {
                    TileEntity tileent = world.getTileEntity(i, j, k);

                    if (tileent != null)
                    {
                        NBTTagCompound nbt = new NBTTagCompound();
                        tileent.writeToNBT(nbt);
                        sender.addChatMessage(new ChatComponentText("NBT: " + nbt));
                    }
                    else
                    {
                        throw new CommandException(Reference.MODID + ".command.nbt.no_tile_entity", new Object[0]);
                    }
                }
            }
            else
            {
                throw new CommandException(getCommandUsage(sender), new Object[0]);
            }
        }
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender)
    {
        return sender.canCommandSenderUseCommand(this.getRequiredPermissionLevel(), this.getCommandName());
    }

    private int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public List addTabCompletionOptions(ICommandSender p_71516_1_, String[] p_71516_2_)
    {
        return null;
    }

    @Override
    public boolean isUsernameIndex(String[] p_82358_1_, int p_82358_2_)
    {
        return false;
    }

}
