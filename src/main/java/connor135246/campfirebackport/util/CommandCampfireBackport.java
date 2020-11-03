package connor135246.campfirebackport.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import connor135246.campfirebackport.common.CommonProxy;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.config.ConfigNetworkManager;
import connor135246.campfirebackport.config.ConfigReference;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

/**
 * Reloads the config from file, or prints the NBT of a held item / NBT of a tile entity at x y z.
 */
public class CommandCampfireBackport implements ICommand
{

    private static final String NBT = "nbt", RELOAD = "reload", DUMPINFO = "dumpinfo", HELP = "help";

    @Override
    public int compareTo(Object o)
    {
        return 0;
    }

    @Override
    public String getCommandName()
    {
        return "campfirebackport";
    }

    @Override
    public String getCommandUsage(ICommandSender p_71518_1_)
    {
        return Reference.MODID + ".command.usage";
    }

    @Override
    public List getCommandAliases()
    {
        return null;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] arguments)
    {
        if (arguments.length > 0)
        {
            if (arguments[0].equals(NBT))
            {
                if (arguments.length == 1 || arguments.length == 2)
                {
                    ItemStack stack = (arguments.length == 1 ? CommandBase.getCommandSenderAsPlayer(sender) : CommandBase.getPlayer(sender, arguments[1]))
                            .getCurrentEquippedItem();

                    if (stack != null)
                        sender.addChatMessage(new ChatComponentText("NBT: " + stack.getTagCompound()));
                    else
                        throw new CommandException(Reference.MODID + ".command.nbt.no_held_item");
                }
                else if (arguments.length == 4)
                {
                    World world = sender.getEntityWorld();
                    int x = MathHelper.floor_double(CommandBase.func_110666_a(sender, sender.getPlayerCoordinates().posX, arguments[1]));
                    int y = MathHelper.floor_double(CommandBase.func_110665_a(sender, sender.getPlayerCoordinates().posY, arguments[2], 0, 256));
                    int z = MathHelper.floor_double(CommandBase.func_110666_a(sender, sender.getPlayerCoordinates().posZ, arguments[3]));

                    if (world.blockExists(x, y, z))
                    {
                        TileEntity tile = world.getTileEntity(x, y, z);

                        if (tile != null)
                        {
                            NBTTagCompound nbt = new NBTTagCompound();
                            tile.writeToNBT(nbt);
                            sender.addChatMessage(new ChatComponentText("NBT: " + nbt));
                        }
                        else
                            throw new CommandException(Reference.MODID + ".command.nbt.no_tile_entity");
                    }
                    else
                        throw new CommandException(Reference.MODID + ".command.nbt.block_out_of_world");
                }
                else
                    throw new WrongUsageException(Reference.MODID + ".command.help.2");
            }
            else if (arguments[0].equals(RELOAD))
            {
                CampfireBackportConfig.doConfig(0, false);

                if (!MinecraftServer.getServer().isSinglePlayer())
                {
                    CommonProxy.modlog.info(StringParsers.translatePacket("send_config_to_all"));

                    Iterator iterator = MinecraftServer.getServer().getConfigurationManager().playerEntityList.iterator();
                    while (iterator.hasNext())
                        CommonProxy.simpleNetwork.sendTo(new ConfigNetworkManager.SendConfigMessage(), (EntityPlayerMP) iterator.next());
                }

                sender.addChatMessage(new ChatComponentTranslation(Reference.MODID + ".command.reload"));
            }
            else if (arguments[0].equals(DUMPINFO))
            {
                sender.addChatMessage(new ChatComponentTranslation(Reference.MODID + ".command.dumpinfo", ConfigReference.README_FILENAME));

                try
                {
                    File explanation = new File(CampfireBackportConfig.configDirectory, ConfigReference.README_FILENAME);

                    if (explanation.exists())
                        explanation.delete();
                    explanation.createNewFile();

                    PrintWriter explanationWriter = new PrintWriter(new FileWriter(explanation));
                    for (int i = 0; i < 390; ++i)
                        explanationWriter.println(StatCollector.translateToLocal(Reference.MODID + ".config.explanation." + i));
                    explanationWriter.close();
                }
                catch (IOException excep)
                {
                    sender.addChatMessage(new ChatComponentTranslation(Reference.MODID + ".command.dumpinfo.error.0")
                            .setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
                    sender.addChatMessage(new ChatComponentTranslation(Reference.MODID + ".command.dumpinfo.error.1")
                            .setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
                }
            }
            else if (arguments[0].equals(HELP))
            {
                for (int i = 0; i < 6; ++i)
                    sender.addChatMessage(new ChatComponentTranslation(Reference.MODID + ".command.help." + i)
                            .setChatStyle(new ChatStyle().setColor(i % 2 == 1 ? EnumChatFormatting.GRAY : EnumChatFormatting.WHITE)));
            }
            else
                throw new WrongUsageException(getCommandUsage(sender));
        }
        else
            throw new WrongUsageException(getCommandUsage(sender));
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
    public List addTabCompletionOptions(ICommandSender sender, String[] arguments)
    {
        if (arguments.length == 1)
            return CommandBase.getListOfStringsMatchingLastWord(arguments, new String[] { NBT, RELOAD, DUMPINFO, HELP });
        else if (arguments.length == 2 && arguments[0].equals(NBT))
            return CommandBase.getListOfStringsMatchingLastWord(arguments, MinecraftServer.getServer().getAllUsernames());
        else
            return null;
    }

    @Override
    public boolean isUsernameIndex(String[] arguments, int index)
    {
        return arguments.length > 0 ? (index == 1 && arguments[0].equals(NBT)) : false;
    }

}
