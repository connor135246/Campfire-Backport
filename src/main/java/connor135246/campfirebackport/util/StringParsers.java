package connor135246.campfirebackport.util;

import cpw.mods.fml.common.registry.GameData;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;

public class StringParsers
{

    /**
     * Converts a string into an ItemStack.
     * 
     * @param stack
     *            - a string in the format modid:name:meta or modid:name for a normal ItemStack; or modid:name:meta@size or modid:name@size for an ItemStack with a size
     * @return an ItemStack of the item, meta (meta 0 if none is given), and size (size 1 if none is given) given; or null if the item doesn't exist
     */
    public static ItemStack parseItemStack(String stack)
    {
        String[] segment = stack.split("@");

        if (segment.length == 1)
            return parseItemStack(segment[0], 1);
        else
            return parseItemStack(segment[0], MathHelper.clamp_int(Integer.parseInt(segment[1]), 1, 64));
    }

    public static ItemStack parseItemStack(String stack, int size)
    {
        String name;
        int meta = 0;

        if (stack.matches("\\w+:\\w+:\\d+"))
        {
            String[] segment = stack.split(":");
            name = segment[0] + ":" + segment[1];
            meta = Integer.parseInt(segment[2]);
        }
        else
            name = stack;

        if (GameData.getItemRegistry().containsKey(name))
            return new ItemStack(GameData.getItemRegistry().getObject(name), size, meta);
        else if (GameData.getBlockRegistry().containsKey(name))
            return new ItemStack(GameData.getBlockRegistry().getObject(name), size, meta);
        else
            return null;
    }

    /**
     * Sometimes, you don't want an ItemStack because the metadata might not matter. Converts a string into an Object[].
     * 
     * @param input
     *            - a string in the format modid:name:meta or modid:name
     * @return an Object[] where the first element is the Item (or null if the item doesn't exist) and the second element is either the meta or -1 if meta wasn't given
     */
    public static Object[] parseItemAndMaybeMeta(String input)
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
            item = null;

        return new Object[] { item, meta };
    }

}
