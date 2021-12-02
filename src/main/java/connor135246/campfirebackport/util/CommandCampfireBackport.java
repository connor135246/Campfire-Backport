package connor135246.campfirebackport.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import connor135246.campfirebackport.common.CommonProxy;
import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.config.ConfigNetworkManager;
import connor135246.campfirebackport.config.ConfigReference;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

public class CommandCampfireBackport implements ICommand
{

    private static final String HELP = "help", // lists options
            NBT = "nbt", // prints the NBT of a held item, or the NBT of a tile entity at x y z
            LOCATIONINFO = "locationinfo", // prints the biome and dimension IDs at x z
            RELOAD = "reload", // reloads the config from file
            DUMPINFO = "dumpinfo", // dumps config info
            GETCAMPFIRE = "getcampfire"; // creates an item copy of a campfire at x y z

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
                    EntityPlayerMP player = arguments.length == 1 ? CommandBase.getCommandSenderAsPlayer(sender) : CommandBase.getPlayer(sender, arguments[1]);
                    ItemStack stack = player.getCurrentEquippedItem();

                    if (stack != null)
                    {
                        sender.addChatMessage(new ChatComponentTranslation(Reference.MODID + ".command.nbt.item",
                                player.getCommandSenderName(), stack.getDisplayName()).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GREEN)));

                        sender.addChatMessage(new ChatComponentText(" " + stack.getTagCompound()));
                    }
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

                            sender.addChatMessage(new ChatComponentTranslation(Reference.MODID + ".command.nbt.tile")
                                    .setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GREEN)));

                            sender.addChatMessage(new ChatComponentText(" " + nbt));
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
            else if (arguments[0].equals(LOCATIONINFO))
            {
                World world;
                int x, z;

                if (arguments.length < 3)
                {
                    EntityPlayerMP player = arguments.length > 1 ? CommandBase.getPlayer(sender, arguments[1]) : CommandBase.getCommandSenderAsPlayer(sender);
                    world = player.worldObj;
                    x = MathHelper.floor_double(player.posX);
                    z = MathHelper.floor_double(player.posZ);
                }
                else
                {
                    world = sender.getEntityWorld();
                    x = MathHelper.floor_double(CommandBase.func_110666_a(sender, sender.getPlayerCoordinates().posX, arguments[1]));
                    z = MathHelper.floor_double(CommandBase.func_110666_a(sender, sender.getPlayerCoordinates().posZ, arguments[2]));
                }

                if (world.blockExists(x, 0, z))
                {
                    BiomeGenBase biome = world.getBiomeGenForCoords(x, z);

                    sender.addChatMessage(makeHoverAndClickTranslation(Reference.MODID + ".command.locationinfo.biome", "" + biome.biomeID,
                            EnumChatFormatting.AQUA, biome.biomeName, biome.biomeID));

                    sender.addChatMessage(makeHoverAndClickTranslation(Reference.MODID + ".command.locationinfo.dimension", "" + world.provider.dimensionId,
                            EnumChatFormatting.BLUE, world.provider.getDimensionName(), world.provider.dimensionId));
                }
                else
                    throw new CommandException(Reference.MODID + ".command.nbt.block_out_of_world");
            }
            else if (arguments[0].equals(RELOAD))
            {
                CampfireBackportConfig.doConfig(15, false);

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
                    explanationWriter.println("--- " + Reference.MODID + "-" + Reference.VERSION + " ---");
                    for (int i = 0; i <= 556; ++i)
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
            else if (arguments[0].equals(GETCAMPFIRE))
            {
                World world;
                int x, y, z;

                if (arguments.length < 4)
                {
                    EntityPlayerMP player = arguments.length > 1 ? CommandBase.getPlayer(sender, arguments[1]) : CommandBase.getCommandSenderAsPlayer(sender);
                    world = player.worldObj;
                    x = MathHelper.floor_double(player.posX);
                    y = MathHelper.floor_double(player.posY);
                    z = MathHelper.floor_double(player.posZ);
                }
                else
                {
                    world = sender.getEntityWorld();
                    x = MathHelper.floor_double(CommandBase.func_110666_a(sender, sender.getPlayerCoordinates().posX, arguments[1]));
                    y = MathHelper.floor_double(CommandBase.func_110665_a(sender, sender.getPlayerCoordinates().posY, arguments[2], 0, 256));
                    z = MathHelper.floor_double(CommandBase.func_110666_a(sender, sender.getPlayerCoordinates().posZ, arguments[3]));
                }

                if (world.blockExists(x, y, z))
                {
                    Block block = world.getBlock(x, y, z);
                    TileEntity tile = world.getTileEntity(x, y, z);

                    if (block instanceof BlockCampfire && tile instanceof TileEntityCampfire)
                    {
                        ItemStack stack = new ItemStack(block);

                        NBTTagCompound blockEntityTag = new NBTTagCompound();
                        tile.writeToNBT(blockEntityTag);
                        blockEntityTag.removeTag("x");
                        blockEntityTag.removeTag("y");
                        blockEntityTag.removeTag("z");
                        blockEntityTag.removeTag("id");
                        blockEntityTag.removeTag(TileEntityCampfire.KEY_SignalFire);
                        stack.setTagInfo(TileEntityCampfire.KEY_BlockEntityTag, blockEntityTag);

                        TileEntityCampfire.popItem(stack, world, x, y, z);

                        HoverEvent hoverNotice = new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new ChatComponentTranslation(Reference.MODID + ".command.get.hover.0").appendSibling(new ChatComponentText("\n").appendSibling(
                                        new ChatComponentTranslation(Reference.MODID + ".command.get.hover.1").appendSibling(new ChatComponentText("\n")
                                                .appendSibling(new ChatComponentTranslation(Reference.MODID + ".command.get.hover.2"))))));

                        sender.addChatMessage(new ChatComponentTranslation(Reference.MODID + ".command.get.result")
                                .setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GREEN)));
                        sender.addChatMessage(new ChatComponentText(" /give @p " + GameData.getItemRegistry().getNameForObject(stack.getItem())
                                + " 1 0 " + stack.getTagCompound()).setChatStyle(new ChatStyle().setChatHoverEvent(hoverNotice)));
                    }
                    else
                        throw new CommandException(Reference.MODID + ".command.get.not_campfire");
                }
                else
                    throw new CommandException(Reference.MODID + ".command.nbt.block_out_of_world");
            }
            else if (arguments[0].equals(HELP))
            {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD +
                        "--- " + StatCollector.translateToLocal(getCommandUsage(sender)) + " ---"));

                for (int i = 0; i <= 9; ++i)
                {
                    if (i == 1)
                    {
                        sender.addChatMessage(makeHoverAndClickTranslation(Reference.MODID + ".command.help.1",
                                "https://github.com/connor135246/Campfire-Backport/wiki", EnumChatFormatting.GRAY));
                    }
                    else
                    {
                        sender.addChatMessage(new ChatComponentTranslation(Reference.MODID + ".command.help." + i)
                                .setChatStyle(new ChatStyle().setColor(i % 2 == 1 ? EnumChatFormatting.GRAY : EnumChatFormatting.WHITE)));
                    }
                }
            }
            else
                throw new WrongUsageException(getCommandUsage(sender));
        }
        else
            throw new WrongUsageException(getCommandUsage(sender));
    }

    private static final HoverEvent clickMe = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentTranslation(Reference.MODID + ".command.clickme"));

    private IChatComponent makeHoverAndClickTranslation(String key, String toCopy, EnumChatFormatting colour, Object... args)
    {
        return new ChatComponentTranslation(key, args).setChatStyle(
                new ChatStyle().setColor(colour).setChatHoverEvent(clickMe).setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, toCopy)));
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
            return CommandBase.getListOfStringsMatchingLastWord(arguments, new String[] { NBT, LOCATIONINFO, RELOAD, DUMPINFO, GETCAMPFIRE, HELP });
        else if (arguments.length == 2 && (arguments[0].equals(NBT) || arguments[0].equals(LOCATIONINFO) || arguments[0].equals(GETCAMPFIRE)))
            return CommandBase.getListOfStringsMatchingLastWord(arguments, MinecraftServer.getServer().getAllUsernames());
        else
            return null;
    }

    @Override
    public boolean isUsernameIndex(String[] arguments, int index)
    {
        return arguments.length == 2 && index == 1 && (arguments[0].equals(NBT) || arguments[0].equals(LOCATIONINFO) || arguments[0].equals(GETCAMPFIRE));
    }

}
