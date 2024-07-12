package connor135246.campfirebackport.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import connor135246.campfirebackport.common.recipes.CampfireRecipe;
import connor135246.campfirebackport.common.recipes.CampfireStateChanger;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.config.ConfigReference;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.oredict.OreDictionary;

public class StringParsers
{
    // Patterns

    public static final String betweenZeroAndOne = "(0(\\.(\\d+))?)|(1(\\.0+)?)",
            item = "[\\w\\.\\-|]+:[\\w\\.\\-|]+",
            itemMeta = item + "(:\\d+)?",
            ore = "ore:[\\w\\.\\-]+",
            tool = "tool:[\\w\\.\\-]+",
            clazz = "class:[\\w\\.]+",
            size = "(@\\d+)?",
            NBT = "\\{[\\w\\s\"" + Pattern.quote("-+:.,[]{}") + "]+}",
            ench = "\\[ench:\\d+,\\d+]",
            fluid = "\\[Fluid:\"\\w+\",MinAmount:\\d+]",
            tinkers = "\\[Tinkers:\\[(I:\\{(\\w+:-?\\d+,)*(\\w+:-?\\d+)})?(B:\\{(\\w+:-?\\d+,)*(\\w+:-?\\d+)})?(F:\\{(\\w+:-?\\d+(\\.\\d+)?,)*(\\w+:-?\\d+(\\.\\d+)?)})?(IA:\\{(\\w+:-?\\d+,)*(\\w+:-?\\d+)})?]]",
            anyData = "((" + NBT + ")|(" + ench + ")|(" + fluid + "))",
            anyDataTinkers = "((" + NBT + ")|(" + ench + ")|(" + fluid + ")|(" + tinkers + "))",
            itemMetaAnyData = itemMeta + anyData + "?",
            oreAnyData = ore + anyData + "?",
            toolAnyData = tool + anyData + "?",
            clazzAnyData = clazz + anyData + "?",
            itemMetaOreStart = "(((?!^ore:)" + itemMeta + ")|(^" + ore + "))",
            itemMetaOreToolClassAnyDataSizeStart = "(((?!^ore:)(?!^tool:)(?!^class:)^" + itemMetaAnyData + ")|(^" + oreAnyData + ")|(^" + toolAnyData + ")|(^"
                    + clazzAnyData + ")|(^" + anyDataTinkers + "))" + size,
            itemMetaOreToolClassAnyDataSizeSlash = "(((?!\\/ore:)(?!\\/tool:)(?!\\/class:)\\/" + itemMetaAnyData + ")|(\\/" + oreAnyData + ")|(\\/"
                    + toolAnyData + ")|(\\/" + clazzAnyData + ")|(\\/" + anyDataTinkers + "))" + size,
            itemMetaOreToolClassAnyDataSizeAnd = "((((?!&ore:)(?!&tool:)(?!&class:)&" + itemMetaAnyData + ")|(&" + oreAnyData + ")|(&"
                    + toolAnyData + ")|(&" + clazzAnyData + ")|(&" + anyDataTinkers + "))" + size + ")?",
            itemMetaAnyDataSize = itemMetaAnyData + size;

    public static final Pattern recipePat = Pattern
            .compile(itemMetaOreToolClassAnyDataSizeStart + itemMetaOreToolClassAnyDataSizeAnd + itemMetaOreToolClassAnyDataSizeAnd
                    + itemMetaOreToolClassAnyDataSizeAnd + "\\/" + itemMetaAnyDataSize + "(\\/((\\d+)|(\\*)))?(\\/((" + CampfireRecipe.SIGNAL + ")|("
                    + CampfireRecipe.NOTSIGNAL + ")|(" + CampfireRecipe.ANY + ")))?" + "(\\/(" + itemMetaAnyDataSize + "))?(\\/" + betweenZeroAndOne + ")?"),
            itemMetaOrePat = Pattern.compile(itemMetaOreStart),
            itemPat = Pattern.compile(item),
            itemMetaAnySimpleDataSizeOREmptyPat = Pattern.compile("(" + itemMetaAnyDataSize + ")|()"),
            stateChangePat = Pattern
                    .compile("((^right)|(^left))(\\+dispensable)?" + itemMetaOreToolClassAnyDataSizeSlash + "\\/((" + CampfireStateChanger.STACKABLE + ")|("
                            + CampfireStateChanger.DAMAGEABLE + ")|(" + CampfireStateChanger.NONE + "))(>" + itemMetaAnyDataSize + ")?"),
            burnOutRulesPat = Pattern.compile(
                    "((regular)|(soul)|(both))\\/((biome:\\d+)|(dimension:-?\\d+)|(biome:\\d+&dimension:-?\\d+)|(dimension:-?\\d+&biome:\\d+))\\/((-1)|(\\d+))"),
            enchPattern = Pattern.compile(ench),
            fluidPattern = Pattern.compile(fluid),
            tinkersPattern = Pattern.compile(tinkers);

    // NBT Keys

    public static final String KEY_GCIDataType = "GCIDataType",
            KEY_ench = "ench",
            KEY_id = "id",
            KEY_lvl = "lvl",
            KEY_StoredEnchantments = "StoredEnchantments",
            KEY_Fluid = "Fluid",
            KEY_fluid = "fluid",
            KEY_FluidName = "FluidName",
            KEY_Amount = "Amount",
            KEY_amount = "amount",
            KEY_tank = "tank",
            KEY_InfiTool = "InfiTool",
            KEY_INT_PREFIX = "I:",
            KEY_BYTE_PREFIX = "B:",
            KEY_FLOAT_PREFIX = "F:",
            KEY_INTARRAY_PREFIX = "IA:",
            KEY_KeySet = "KeySet",
            KEY_TypeSet = "TypeSet";

    // Parsers

    /**
     * Converts a string into an Object[]. The first element is either an int representing an ore dict ID, a String representing a tool class, an Item, a Class, or null if it
     * wasn't given or wasn't found. The second element is the size (1 if none is given). The third element is the meta ({@link OreDictionary#WILDCARD_VALUE} or 0 if none was
     * given). The fourth element is the exact NBT tag or extra data, such as an enchantment or a fluid. Whenever you call these methods, you should surround them with a try/catch.
     * 
     * @param input
     *            - look at the config explanation file.
     * @param returnWildcard
     *            - if true and meta isn't specified, meta will return as {@link OreDictionary#WILDCARD_VALUE}. if false and meta isn't specified, meta will return as 0.
     */
    public static Object[] parseItemOrOreOrToolOrClassWithNBTOrDataWithSize(String input, boolean returnWildcard)
    {
        String[] segment = input.split("@");

        if (segment.length == 1)
            return parseItemOrOreOrToolOrClassWithNBTOrData(segment[0], 1, returnWildcard);
        else
            return parseItemOrOreOrToolOrClassWithNBTOrData(segment[0], Math.max(Integer.parseInt(segment[1]), 1), returnWildcard);
    }

    public static Object[] parseItemOrOreOrToolOrClassWithNBTOrData(String input, int size, boolean returnWildcard)
    {
        NBTTagCompound data = new NBTTagCompound();

        Matcher enchMatcher = enchPattern.matcher(input);
        String ench = null;
        if (enchMatcher.find())
            ench = enchMatcher.group();
        enchMatcher.reset();

        if (ench != null && !ench.isEmpty())
        {
            int id = Integer.parseInt(ench.substring(6, ench.lastIndexOf(",")));
            if (id >= 0 && id < Enchantment.enchantmentsList.length && Enchantment.enchantmentsList[id] != null)
            {
                input = enchMatcher.replaceFirst("");
                NBTTagList enchList = new NBTTagList();
                NBTTagCompound theench = new NBTTagCompound();
                theench.setInteger(KEY_lvl, Integer.parseInt(ench.substring(ench.lastIndexOf(",") + 1, ench.length() - 1)));
                theench.setInteger(KEY_id, id);
                enchList.appendTag(theench);
                data.setTag(KEY_ench, enchList);

                data.setByte(KEY_GCIDataType, (byte) 2);

                return parseItemOrOreOrToolOrClass(input, size, data, returnWildcard);
            }
            else
            {
                ConfigReference.logError("invalid_ench_id", id);
                return new Object[] { null, null, null, null };
            }
        }

        Matcher fluidMatcher = fluidPattern.matcher(input);
        String fluid = null;
        if (fluidMatcher.find())
            fluid = fluidMatcher.group();
        fluidMatcher.reset();

        if (fluid != null && !fluid.isEmpty())
        {
            input = fluidMatcher.replaceFirst("");

            String fluidName = fluid.substring(8, fluid.lastIndexOf("\""));
            if (!FluidRegistry.isFluidRegistered(fluidName))
            {
                ConfigReference.logError("invalid_fluid", fluidName);
                return new Object[] { null, null, null, null };
            }

            int fluidAmount = Integer.parseInt(fluid.substring(fluid.lastIndexOf(":") + 1, fluid.length() - 1));
            if (fluidAmount <= 0)
            {
                ConfigReference.logError("invalid_fluid_amount", fluidAmount);
                return new Object[] { null, null, null, null };
            }

            NBTTagCompound fluidCom = new NBTTagCompound();
            fluidCom.setString(KEY_FluidName, fluidName);
            fluidCom.setInteger(KEY_Amount, fluidAmount);
            data.setTag(KEY_Fluid, fluidCom);

            data.setByte(KEY_GCIDataType, (byte) 3);

            return parseItemOrOreOrToolOrClass(input, size, data, returnWildcard);
        }

        Matcher tinkersMatcher = tinkersPattern.matcher(input);
        String tinkers = null;
        if (tinkersMatcher.find())
            tinkers = tinkersMatcher.group();
        tinkersMatcher.reset();

        if (tinkers != null && !tinkers.isEmpty())
        {
            input = tinkersMatcher.replaceFirst("");

            tinkers = tinkers.substring(10, tinkers.length() - 2);
            String[] tags = tinkers.split("\\{|}");

            NBTTagList keyList = new NBTTagList();
            NBTTagList typeList = new NBTTagList();

            for (int counter = 0; counter < tags.length - 1; counter += 2)
            {
                String[] settings = tags[counter + 1].split(",");
                for (String setting : settings)
                {
                    String[] values = setting.split(":");

                    keyList.appendTag(new NBTTagString(values[0]));
                    typeList.appendTag(new NBTTagString(tags[counter]));

                    if (tags[counter].equals(KEY_INT_PREFIX))
                        data.setInteger(values[0], Integer.parseInt(values[1]));
                    else if (tags[counter].equals(KEY_BYTE_PREFIX))
                        data.setByte(values[0], Byte.parseByte(values[1]));
                    else if (tags[counter].equals(KEY_FLOAT_PREFIX))
                        data.setFloat(values[0], Float.parseFloat(values[1]));
                    else if (tags[counter].equals(KEY_INTARRAY_PREFIX))
                        data.setIntArray(values[0], new int[] { Integer.parseInt(values[1]) });
                }
            }

            data.setTag(KEY_KeySet, keyList);
            data.setTag(KEY_TypeSet, typeList);
            data.setByte(KEY_GCIDataType, (byte) 4);

            return parseItemOrOreOrToolOrClass(input, size, data, returnWildcard);
        }

        return parseItemOrOreOrToolOrClassWithNBT(input, size, returnWildcard);
    }

    public static Object[] parseItemOrOreOrToolOrClassWithNBT(String input, int size, boolean returnWildcard)
    {
        int nbtIndex = input.indexOf("{");

        if (nbtIndex == -1)
            return parseItemOrOreOrToolOrClass(input, size, new NBTTagCompound(), returnWildcard);

        NBTTagCompound nbt;
        try
        {
            nbt = (NBTTagCompound) JsonToNBT.func_150315_a(input.substring(nbtIndex));
            nbt.setByte(KEY_GCIDataType, (byte) 1);
        }
        catch (NBTException excep)
        {
            ConfigReference.logError("invalid_nbt", input);
            return new Object[] { null, null, null, null };
        }
        catch (ClassCastException excep)
        {
            ConfigReference.logError("invalid_nbt", input);
            return new Object[] { null, null, null, null };
        }

        return parseItemOrOreOrToolOrClass(input.substring(0, nbtIndex), size, nbt, returnWildcard);
    }

    public static Object[] parseItemOrOreOrToolOrClass(String input, int size, NBTTagCompound data, boolean returnWildcard)
    {
        if (!input.isEmpty())
        {
            if (input.startsWith("ore:"))
                return parseOre(input, size, data, returnWildcard);
            else if (input.startsWith("tool:"))
                return new Object[] { input, size, returnWildcard ? OreDictionary.WILDCARD_VALUE : 0, data };
            else if (input.startsWith("class:"))
                return parseClass(input.substring(6), size, data, returnWildcard);
            else
                return parseItemAndMaybeMeta(input, size, data, returnWildcard);
        }
        else
            return new Object[] { null, size, returnWildcard ? OreDictionary.WILDCARD_VALUE : 0, data };
    }

    /**
     * The same as {@link #parseItemOrOreOrToolOrClass} but instead of an Item/ore/tool/Class, it's a Block/ore.
     */
    public static Object[] parseBlockOrOre(String input, boolean returnWildcard)
    {
        if (input.startsWith("ore:"))
            return parseOre(input, 1, new NBTTagCompound(), returnWildcard);
        else
            return parseBlockAndMaybeMeta(input, 1, new NBTTagCompound(), returnWildcard);
    }

    public static Object[] parseClass(String input, int size, NBTTagCompound data, boolean returnWildcard)
    {
        try
        {
            Class inputClass = Class.forName(input);
            return new Object[] { inputClass, size, returnWildcard ? OreDictionary.WILDCARD_VALUE : 0, data };
        }
        catch (ClassNotFoundException excep)
        {
            ConfigReference.logError("invalid_class", input);
            return new Object[] { null, null, null, null };
        }
    }

    public static Object[] parseOre(String input, int size, NBTTagCompound data, boolean returnWildcard)
    {
        if (!OreDictionary.doesOreNameExist(input.substring(4)))
            CampfireBackportConfig.possiblyInvalidOres.add(input.substring(4));
        return new Object[] { input, size, returnWildcard ? OreDictionary.WILDCARD_VALUE : 0, data };
    }

    public static Object[] parseItemAndMaybeMeta(String input, int size, NBTTagCompound data, boolean returnWildcard)
    {
        Integer meta = returnWildcard ? OreDictionary.WILDCARD_VALUE : 0;

        String[] segment = input.split(":");

        if (segment.length > 2)
            meta = Integer.parseInt(segment[2]);

        ItemStack stack = GameRegistry.findItemStack(segment[0], segment[1], 1);

        if (stack != null)
            return new Object[] { stack.getItem(), size, meta, data };
        else
        {
            ConfigReference.logError("invalid_item", input);
            return new Object[] { null, null, null, null };
        }
    }

    public static Object[] parseBlockAndMaybeMeta(String input, int size, NBTTagCompound data, boolean returnWildcard)
    {
        Integer meta = returnWildcard ? OreDictionary.WILDCARD_VALUE : 0;

        String[] segment = input.split(":");

        if (segment.length > 2)
            meta = Integer.parseInt(segment[2]);

        Block block = GameRegistry.findBlock(segment[0], segment[1]);

        if (block != null && block != Blocks.air)
            return new Object[] { block, size, meta, data };
        else
        {
            ConfigReference.logError("invalid_block", input);
            return new Object[] { null, null, null, null };
        }
    }

    // Making Things Look Nice

    /**
     * Turns some number of ticks into a human-readable amount of seconds or minutes or hours.
     */
    public static String translateTimeHumanReadable(int ticks)
    {
        boolean negative = ticks < 0;
        if (negative)
            ticks = Math.abs(ticks);

        double converted;
        String unit;

        if (ticks >= 71400) // 1 hour - 0.5 minutes
        {
            converted = ticks / 72000.0;
            unit = "hours";
        }
        else if (ticks >= 1190) // 1 minute - 0.5 seconds
        {
            converted = ticks / 1200.0;
            unit = "mins";
        }
        else
        {
            if (ticks == 0) // no zero. this way the minimum is 0.1 seconds.
                ticks = 1;
            converted = ticks / 20.0;
            unit = "secs";
        }

        String formatted = negative ? "-" : "";
        if (converted < 10)
        {
            formatted += Math.round(converted * 10) / 10.0;
            if (formatted.endsWith(".0"))
                formatted = formatted.substring(0, formatted.length() - 2);
        }
        else
            formatted += Math.round(converted);

        return translateTime(unit, formatted);
    }

    /**
     * @param unit
     *            - can be "hours", "mins", "secs", or "ticks"
     * @param amount
     */
    public static String translateTime(String unit, String amount)
    {
        return translateNEI("num_" + unit, amount);
    }

    /**
     * Separates a long string into lines after each lineEnder. Tries to join short lines together while staying under lineLength. Puts prefixes in front of each line.
     * 
     * @param string
     * @param lineEnder
     * @param firstLinePrefix
     *            - the prefix that should go in front of only the first line of the string
     * @param otherLinePrefix
     *            - the prefix that should go in front of every other line of the string
     * @param lineLength
     *            - the preferred length of a line
     */
    public static List<String> lineifyString(String string, String lineEnder, String firstLinePrefix, String otherLinePrefix, int lineLength)
    {
        List<String> lines = new LinkedList<String>();

        if (firstLinePrefix.length() + string.length() <= lineLength)
            lines.add(firstLinePrefix + string);
        else
        {
            int nextLineEnder = string.indexOf(lineEnder);

            if (nextLineEnder == -1)
                lines.add(firstLinePrefix + string);
            else
            {
                StringBuilder line = new StringBuilder((int) (lineLength * 1.5));
                line.append(firstLinePrefix + string.substring(0, nextLineEnder + lineEnder.length()));

                for (int index = nextLineEnder + lineEnder.length(); index < string.length(); index = nextLineEnder + lineEnder.length())
                {
                    nextLineEnder = string.indexOf(lineEnder, index);

                    String substring;
                    if (nextLineEnder == -1)
                    {
                        substring = string.substring(index);
                        nextLineEnder = string.length();
                    }
                    else
                        substring = string.substring(index, nextLineEnder + lineEnder.length());

                    if (line.length() + substring.length() <= lineLength)
                        line.append(substring);
                    else
                    {
                        lines.add(line.toString());
                        line.delete(0, line.length());
                        line.append(otherLinePrefix + substring);
                    }
                }

                lines.add(line.toString());
            }
        }

        return lines;
    }

    /**
     * Separates a long string into lines after each lineEnder. Then sends to {@link #joinShortLinesAndPrefix}.
     */
    public static List<String> lineifyStringLazy(String string, String lineEnder, String firstLinePrefix, String otherLinePrefix, int lineLength)
    {
        if (firstLinePrefix.length() + string.length() <= lineLength)
        {
            List<String> lines = new LinkedList<String>();
            lines.add(firstLinePrefix + string);
            return lines;
        }
        else
            return joinShortLinesAndPrefix(Arrays.asList(string.split("(?<=" + lineEnder + ")")), firstLinePrefix, otherLinePrefix, lineLength);
    }

    /**
     * Tries to join short lines together while staying under lineLength. Puts prefixes in front of each line.
     * 
     * @param strings
     * @param firstLinePrefix
     *            - the prefix that should go in front of only the first line of the string
     * @param otherLinePrefix
     *            - the prefix that should go in front of every other line of the string
     * @param lineLength
     *            - the preferred length of a line
     */
    public static List<String> joinShortLinesAndPrefix(List<String> strings, String firstLinePrefix, String otherLinePrefix, int lineLength)
    {
        List<String> lines = new LinkedList<String>();

        Iterator<String> iterator = strings.iterator();
        if (iterator.hasNext())
        {
            String element = iterator.next();

            StringBuilder line = new StringBuilder((int) (lineLength * 1.5));
            line.append(firstLinePrefix + element);

            while (iterator.hasNext())
            {
                element = iterator.next();

                if (line.length() + element.length() <= lineLength)
                    line.append(element);
                else
                {
                    lines.add(line.toString());
                    line.delete(0, line.length());
                    line.append(otherLinePrefix + element);
                }
            }

            lines.add(line.toString());
        }

        return lines;
    }

    /**
     * converts an nbt key and value that represents a tinkers modifier into a String that's closer to the tooltip on an actual tinkers tool
     */
    public static String convertTinkersNBTForDisplay(String key, Object value)
    {
        StringBuilder returned = new StringBuilder();
        final String not = EnumChatFormatting.UNDERLINE + "NOT" + EnumChatFormatting.RESET + " ";

        if (key.equals("Shoddy"))
        {
            if ((Float) value > 0.0F)
                returned.append(EnumChatFormatting.GRAY + "Stonebound Modifier: " + EnumChatFormatting.AQUA
                        + (Float) value + EnumChatFormatting.GRAY + " or greater");
            else if ((Float) value < 0.0F)
                returned.append(EnumChatFormatting.GREEN + "Jagged Modifier: " + EnumChatFormatting.AQUA
                        + Math.abs((Float) value) + EnumChatFormatting.GRAY + " or greater");
            else
                returned.append(not + EnumChatFormatting.GRAY + "Stonebound" + EnumChatFormatting.RESET + " or " + EnumChatFormatting.GREEN + "Jagged");
        }
        else if (key.equals("Knockback"))
        {
            returned.append(EnumChatFormatting.DARK_GRAY + "Knockback");
            if ((Float) value > 0.0F)
                returned.append(": " + EnumChatFormatting.GREEN + (Float) value + EnumChatFormatting.GRAY + " or greater");
            else if ((Float) value < 0.0F)
                returned.append(": " + EnumChatFormatting.GREEN + (Float) value + EnumChatFormatting.GRAY + " or smaller");
            else
                returned.insert(0, not);
        }
        else if (key.equals("HarvestLevel"))
        {
            if ((Integer) value > 0)
                returned.append(EnumChatFormatting.GRAY + "Harvest Level: " + EnumChatFormatting.GREEN
                        + (Integer) value + EnumChatFormatting.GRAY + " or greater");
        }
        else if (key.equals("MiningSpeed"))
        {
            if ((Integer) value > 0)
                returned.append(EnumChatFormatting.GRAY + "Mining Speed: " + EnumChatFormatting.GREEN
                        + (Integer) value + EnumChatFormatting.GRAY + " or greater");
        }
        else if (key.equals("Moss"))
        {
            if ((Integer) value > 0)
                returned.append(EnumChatFormatting.GREEN + "Auto-Repair");
        }
        else if (key.equals("Unbreaking"))
        {
            if ((Integer) value > 0)
                returned.append(EnumChatFormatting.GRAY + "Reinforced");
            if ((Integer) value > 1)
                returned.append(" " + StatCollector.translateToLocal("enchantment.level." + (Integer) value) + EnumChatFormatting.GRAY + " or greater");
        }
        else if (key.equals("Necrotic"))
        {
            if ((Integer) value > 0)
                returned.append(EnumChatFormatting.DARK_GRAY + "Life Steal");
            if ((Integer) value > 1)
                returned.append(" " + StatCollector.translateToLocal("enchantment.level." + (Integer) value) + EnumChatFormatting.GRAY + " or greater");
        }
        else if (key.equals("Beheading"))
        {
            if ((Integer) value > 0)
                returned.append(EnumChatFormatting.LIGHT_PURPLE + "Beheading");
            if ((Integer) value > 1)
                returned.append(" " + StatCollector.translateToLocal("enchantment.level." + (Integer) value) + EnumChatFormatting.GRAY + " or greater");
        }
        else if (key.equals("SilkTouch"))
        {
            returned.append(EnumChatFormatting.YELLOW + "Silky");
            if ((Byte) value == 0)
                returned.insert(0, not);
        }
        else if (key.equals("Emerald"))
        {
            returned.append(EnumChatFormatting.GREEN + "Durability +50%");
            if ((Byte) value == 0)
                returned.insert(0, not);
        }
        else if (key.equals("Diamond"))
        {
            returned.append(EnumChatFormatting.AQUA + "Durability +500");
            if ((Byte) value == 0)
                returned.insert(0, not);
        }
        else if (key.equals("Lava"))
        {
            returned.append(EnumChatFormatting.DARK_RED + "Auto-Smelt");
            if ((Byte) value == 0)
                returned.insert(0, not);
        }
        else if (key.equals("Broken"))
        {
            returned.append(EnumChatFormatting.GRAY + "" + EnumChatFormatting.ITALIC + "Broken");
            if ((Byte) value == 0)
                returned.insert(0, not);
        }
        else if (key.equals("Flux"))
        {
            returned.append(EnumChatFormatting.GRAY + "Uses RF");
            if ((Byte) value == 0)
                returned.insert(0, not);
        }
        else if (key.equals("Fiery"))
        {
            if ((Integer) value > 0)
                returned.append(EnumChatFormatting.GOLD + "Fiery");
            if ((Integer) value > 1)
                returned.append(" (" + (Integer) value + " or greater / " + ((((Integer) value) / 25) + 1) * 25 + ")");
        }
        else if (key.equals("ModAntiSpider"))
        {
            if ((Integer) value > 0)
                returned.append(EnumChatFormatting.GREEN + "Bane of Arthropods");
            if ((Integer) value > 1)
                returned.append(" (" + (Integer) value + " or greater / " + ((((Integer) value) / 4) + 1) * 4 + ")");
        }
        else if (key.equals("Redstone"))
        {
            if ((Integer) value > 0)
                returned.append(EnumChatFormatting.RED + "Haste");
            if ((Integer) value > 1)
                returned.append(" (" + (Integer) value + " or greater / " + ((((Integer) value) / 50) + 1) * 50 + ")");
        }
        else if (key.equals("ModSmite"))
        {
            if ((Integer) value > 0)
                returned.append(EnumChatFormatting.YELLOW + "Smite");
            if ((Integer) value > 1)
                returned.append(" (" + (Integer) value + " or greater / " + ((((Integer) value) / 36) + 1) * 36 + ")");
        }
        else if (key.equals("ModAttack"))
        {
            if ((Integer) value > 0)
                returned.append(EnumChatFormatting.WHITE + "Sharpness");
            if ((Integer) value > 1)
                returned.append(" (" + (Integer) value + " or greater / " + ((((Integer) value) / 72) + 1) * 72 + ")");
        }
        else if (key.equals("Lapis"))
        {
            if ((Integer) value > 0)
                returned.append(EnumChatFormatting.BLUE + "Luck");
            if ((Integer) value > 1)
                returned.append(" (" + (Integer) value + " or greater / 450)");
        }
        else if (value instanceof Integer)
        {
            if ((Integer) value > 0)
                returned.append(key);
            if ((Integer) value > 1)
                returned.append(" (" + (Integer) value + " or greater)");
        }
        else if (value instanceof Byte)
        {
            returned.append(key);
            if ((Byte) value == 0)
                returned.insert(0, not);
        }
        else if (value instanceof Float)
        {
            if ((Float) value > 0.0F)
                returned.append(key + ": " + (Float) value + " or greater");
            else if ((Float) value < 0.0F)
                returned.append(key + ": " + (Float) value + " or smaller");
            else
                returned.append(not + key);
        }
        else
            returned.append(key);

        return returned.toString();
    }

    // Translators

    /**
     * @return the key translated using the "config.inputerror" prefix
     */
    public static String translateInputError(String key)
    {
        return StatCollector.translateToLocal(Reference.MODID + ".config.inputerror." + key);
    }

    /**
     * @return the key/args translated using the "config.inputerror" prefix
     */
    public static String translateInputError(String key, Object... args)
    {
        return StatCollector.translateToLocalFormatted(Reference.MODID + ".config.inputerror." + key, args);
    }

    /**
     * @return the key translated using the "config.info" prefix
     */
    public static String translateInfo(String key)
    {
        return StatCollector.translateToLocal(Reference.MODID + ".config.info." + key);
    }

    /**
     * @return the key/args translated using the "config.info" prefix
     */
    public static String translateInfo(String key, Object... args)
    {
        return StatCollector.translateToLocalFormatted(Reference.MODID + ".config.info." + key, args);
    }

    /**
     * @return the key translated using "config.{key}.tooltip"
     */
    public static String translateTooltip(String key)
    {
        return StatCollector.translateToLocal(Reference.MODID + ".config." + key + ".tooltip");
    }

    /**
     * @return the key translated using the "config.packets" prefix
     */
    public static String translatePacket(String key)
    {
        return StatCollector.translateToLocal(Reference.MODID + ".config.packets." + key);
    }

    /**
     * @return the key/args translated using the "config.packets" prefix
     */
    public static String translatePacket(String key, Object... args)
    {
        return StatCollector.translateToLocalFormatted(Reference.MODID + ".config.packets." + key, args);
    }

    /**
     * @return the key/args translated using the "mixin_config.packets" prefix
     */
    public static String translatePacketMixin(String key, Object... args)
    {
        return StatCollector.translateToLocalFormatted(Reference.MODID + ".mixin_config.packets." + key, args);
    }

    /**
     * @return the key translated using the "nei" prefix
     */
    public static String translateNEI(String key)
    {
        return StatCollector.translateToLocal(Reference.MODID + ".nei." + key);
    }

    /**
     * @return the key/args translated using the "nei" prefix
     */
    public static String translateNEI(String key, Object... args)
    {
        return StatCollector.translateToLocalFormatted(Reference.MODID + ".nei." + key, args);
    }

    /**
     * @return the key translated using the "crafttweaker" prefix
     */
    public static String translateCT(String key)
    {
        return StatCollector.translateToLocal(Reference.MODID + ".crafttweaker." + key);
    }

    /**
     * @return the key/args translated using the "crafttweaker" prefix
     */
    public static String translateCT(String key, Object... args)
    {
        return StatCollector.translateToLocalFormatted(Reference.MODID + ".crafttweaker." + key, args);
    }

    /**
     * @return the key translated using the "waila" prefix
     */
    public static String translateWAILA(String key)
    {
        return StatCollector.translateToLocal(Reference.MODID + ".waila." + key);
    }

    /**
     * @return the key/args translated using the "waila" prefix
     */
    public static String translateWAILA(String key, Object... args)
    {
        return StatCollector.translateToLocalFormatted(Reference.MODID + ".waila." + key, args);
    }

}
