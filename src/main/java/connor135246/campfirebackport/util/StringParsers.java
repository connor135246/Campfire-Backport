package connor135246.campfirebackport.util;

import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraftforge.oredict.OreDictionary;

public class StringParsers
{

    /**
     * Converts a string into an Object[]. The first element is either a String representing an ore dictionary name or an Item. The second element is the size (1 if none is given).
     * The third element is the meta (-1 if none was given). If the item/ore doesn't exist, it's all nulls.
     * 
     * @param input
     *            - a String in the format modid:name:meta or modid:name for a normal ItemStack; or modid:name:meta@size or modid:name@size for an ItemStack with a size; or
     *            ore:oreName for an ore dictionary name
     */
    public static Object[] parseItemStackOrOreWithSize(String input)
    {
        String[] segment = input.split("@");

        if (segment.length == 1)
            return parseItemStackOrOre(segment[0], 1);
        else
            return parseItemStackOrOre(segment[0], MathHelper.clamp_int(Integer.parseInt(segment[1]), 1, 64));
    }

    /**
     * The same as {@link #parseItemStackOrOreWithSize(String)} but instead of an Item, it's a Block.
     */
    public static Object[] parseBlockOrOreWithSize(String input)
    {
        String[] segment = input.split("@");

        if (segment.length == 1)
            return parseBlockOrOre(segment[0], 1);
        else
            return parseBlockOrOre(segment[0], MathHelper.clamp_int(Integer.parseInt(segment[1]), 1, 64));
    }

    public static Object[] parseItemStackOrOre(String input, int size)
    {
        if (input.startsWith("ore:"))
            return parseOre(input.substring(4), size);
        else
            return parseItemAndMaybeMeta(input, size);
    }

    public static Object[] parseBlockOrOre(String input, int size)
    {
        if (input.startsWith("ore:"))
            return parseOre(input.substring(4), size);
        else
            return parseBlockAndMaybeMeta(input, size);
    }

    public static Object[] parseOre(String input, int size)
    {
        if (OreDictionary.doesOreNameExist(input))
            return new Object[] { input, size, -1 };
        else
            return new Object[] { null, null, null };
    }

    public static Object[] parseItemAndMaybeMeta(String input, int size)
    {
        String name;
        Integer meta = -1;
        Item item;

        if (input.matches("\\w+:\\w+:\\d+"))
        {
            String[] segment = input.split(":");
            name = segment[0] + ":" + segment[1];
            meta = Integer.parseInt(segment[2]);
        }
        else
            name = input;

        if (GameData.getItemRegistry().containsKey(name))
            item = GameData.getItemRegistry().getObject(name);
        else if (GameData.getBlockRegistry().containsKey(name))
            item = Item.getItemFromBlock(GameData.getBlockRegistry().getObject(name));
        else
            return new Object[] { null, null, null };

        return new Object[] { item, size, meta };
    }

    public static Object[] parseBlockAndMaybeMeta(String input, int size)
    {
        String name;
        Integer meta = -1;
        Block block;

        if (input.matches("\\w+:\\w+:\\d+"))
        {
            String[] segment = input.split(":");
            name = segment[0] + ":" + segment[1];
            meta = Integer.parseInt(segment[2]);
        }
        else
            name = input;

        block = GameData.getBlockRegistry().getObject(name);

        if (block != Blocks.air)
            return new Object[] { block, size, meta };
        else
            return new Object[] { null, null, null };

    }

    @Deprecated
    public static ItemStack parseItemStack(String input)
    {
        String name;
        int meta = 0;
        int size = 1;

        String[] segment1 = input.split("@");

        if (segment1.length != 1)
            size = MathHelper.clamp_int(Integer.parseInt(segment1[1]), 1, 64);

        if (segment1[0].matches("\\w+:\\w+:\\d+"))
        {
            String[] segment2 = segment1[0].split(":");
            name = segment2[0] + ":" + segment2[1];
            meta = Integer.parseInt(segment2[2]);
        }
        else
            name = segment1[0];

        if (GameData.getItemRegistry().containsKey(name))
            return new ItemStack(GameData.getItemRegistry().getObject(name), size, meta);
        else if (GameData.getBlockRegistry().containsKey(name))
            return new ItemStack(GameData.getBlockRegistry().getObject(name), size, meta);
        else
            return null;
    }

}
