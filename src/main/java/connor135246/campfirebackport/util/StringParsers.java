package connor135246.campfirebackport.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import connor135246.campfirebackport.CampfireBackport;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.oredict.OreDictionary;

public class StringParsers
{
    // Patterns
    public static final String item = "\\w+:\\w+",
            itemMeta = item + "(:\\d+)?",
            ore = "ore:\\w+",
            tool = "tool:\\w+",
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
            itemMetaAnyDataSize = itemMetaAnyData + size;

    public static final Pattern recipePat = Pattern
            .compile(itemMetaOreToolClassAnyDataSizeStart + "\\/" + itemMetaAnyDataSize + "(\\/\\d+)?(\\/((signal)|(notsignal)))?"),
            itemMetaOrePat = Pattern.compile(itemMetaOreStart),
            itemPat = Pattern.compile(item),
            itemMetaAnySimpleDataSizeOREmptyPat = Pattern.compile("(" + itemMetaAnyDataSize + ")|()"),
            stateChangePat = Pattern.compile("((^right)|(^left))(\\+dispensable)?" + itemMetaOreToolClassAnyDataSizeSlash
                    + "\\/((none)|(damageable)|(stackable))(>" + itemMetaAnyDataSize + ")?"),
            enchPattern = Pattern.compile(ench),
            fluidPattern = Pattern.compile(fluid),
            tinkersPattern = Pattern.compile(tinkers);

    // NBT Keys
    public static final String GCI_DATATYPE = "GCIDataType",
            ENCH = "ench",
            ID = "id",
            LVL = "lvl",
            FLUID_CAPS = "Fluid",
            FLUID_LOWER = "fluid",
            FLUIDNAME = "FluidName",
            AMOUNT_CAPS = "Amount",
            AMOUNT_LOWER = "amount",
            TANK = "tank",
            INFITOOL = "InfiTool",
            INT_PREFIX = "I:",
            BYTE_PREFIX = "B:",
            FLOAT_PREFIX = "F:",
            INTARRAY_PREFIX = "IA:",
            KEYSET = "KeySet",
            TYPESET = "TypeSet";

    /**
     * Converts a string into an <code>Object[]</code>. The first element is either a <code>String</code> representing an ore dictionary name, an <code>Item</code>, a
     * <code>Class</code>, or <code>null</code> if it wasn't given or wasn't found. The second element is the size (1 if none is given). The third element is the meta
     * ({@link OreDictionary#WILDCARD_VALUE} or 0 if none was given). The fourth element is the exact NBT tag or extra data, such as an enchantment or a fluid. Whenever you call
     * these methods, you should surround them with a <code>try</code>/<code>catch</code>.
     * 
     * @param input
     *            - a String in the format modid:name(:meta)({nbt}|[ench:id,minlvl]|[Fluid:"fluidname",Amount:minamount])(@size) or
     *            ore:oreName({nbt}|[ench:id,minlvl]|[Fluid:"fluidname",MinAmount:minamount])(@size) or
     *            tool:toolClass({nbt}|[ench:id,minlvl]|[Fluid:"fluidname",MinAmount:minamount])(@size)
     *            class:package.class({nbt}|[ench:id,minlvl]|[Fluid:"fluidname",MinAmount:minamount])(@size) or
     *            {nbt}|[ench:id,minlvl]|[Fluid:"fluidname",MinAmount:minamount]|[Tinkers:[I:{integer modifiers}B:{byte modifiers}F:{float modifiers}IA:{integer array
     *            modifiers}](@size). the parts surrounded by regular brackets are optional. for output stacks, it must be the first format.
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
            int id = Integer.valueOf(ench.substring(6, ench.lastIndexOf(",")));
            if (Enchantment.enchantmentsList[id] != null)
            {
                input = enchMatcher.replaceFirst("");
                NBTTagList enchList = new NBTTagList();
                NBTTagCompound theench = new NBTTagCompound();
                theench.setInteger(LVL, Integer.valueOf(ench.substring(ench.lastIndexOf(",") + 1, ench.length() - 1)));
                theench.setInteger(ID, id);
                enchList.appendTag(theench);
                data.setTag(ENCH, enchList);

                data.setByte(GCI_DATATYPE, (byte) 1);

                return parseItemOrOreOrToolOrClass(input, size, data, returnWildcard);
            }
            else
            {
                if (!CampfireBackportConfig.suppressInputErrors)
                    CampfireBackport.proxy.modlog.warn(StatCollector.translateToLocalFormatted(Reference.MODID + ".inputerror.invalid_ench_id", input));
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

            if (FluidRegistry.isFluidRegistered(fluidName))
            {
                NBTTagCompound fluidCom = new NBTTagCompound();
                fluidCom.setString(FLUIDNAME, fluidName);
                fluidCom.setInteger(AMOUNT_CAPS, Integer.valueOf(fluid.substring(fluid.lastIndexOf(":") + 1, fluid.length() - 1)));
                data.setTag(FLUID_CAPS, fluidCom);

                data.setByte(GCI_DATATYPE, (byte) 2);

                return parseItemOrOreOrToolOrClass(input, size, data, returnWildcard);
            }
            else
            {
                if (!CampfireBackportConfig.suppressInputErrors)
                    CampfireBackport.proxy.modlog.warn(StatCollector.translateToLocalFormatted(Reference.MODID + ".inputerror.invalid_fluid", input));
                return new Object[] { null, null, null, null };
            }
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

                    if (tags[counter].equals(INT_PREFIX))
                    {
                        data.setInteger(values[0], Integer.parseInt(values[1]));
                    }
                    else if (tags[counter].equals(BYTE_PREFIX))
                    {
                        data.setByte(values[0], Byte.parseByte(values[1]));
                    }
                    else if (tags[counter].equals(FLOAT_PREFIX))
                    {
                        data.setFloat(values[0], Float.parseFloat(values[1]));
                    }
                    else if (tags[counter].equals(INTARRAY_PREFIX))
                    {
                        data.setIntArray(values[0], new int[] { Integer.parseInt(values[1]) });
                    }
                }
            }

            data.setTag(KEYSET, keyList);
            data.setTag(TYPESET, typeList);
            data.setByte(GCI_DATATYPE, (byte) 3);

            return parseItemOrOreOrToolOrClass(input, size, data, returnWildcard);
        }

        return parseItemOrOreOrToolOrClassWithNBT(input, size, returnWildcard);
    }

    public static Object[] parseItemOrOreOrToolOrClassWithNBT(String input, int size, boolean returnWildcard)
    {
        int nbtIndex = input.indexOf("{");

        if (nbtIndex == -1)
            return parseItemOrOreOrToolOrClass(input, size, new NBTTagCompound(), returnWildcard);

        NBTBase nbt;
        try
        {
            nbt = JsonToNBT.func_150315_a(input.substring(nbtIndex));
        }
        catch (NBTException e)
        {
            if (!CampfireBackportConfig.suppressInputErrors)
                CampfireBackport.proxy.modlog.warn(StatCollector.translateToLocalFormatted(Reference.MODID + ".inputerror.invalid_nbt", input));
            return new Object[] { null, null, null, null };
        }

        if (!(nbt instanceof NBTTagCompound))
        {
            if (!CampfireBackportConfig.suppressInputErrors)
                CampfireBackport.proxy.modlog.warn(StatCollector.translateToLocalFormatted(Reference.MODID + ".inputerror.invalid_nbt", input));
            return new Object[] { null, null, null, null };
        }

        return parseItemOrOreOrToolOrClass(input.substring(0, nbtIndex), size, (NBTTagCompound) nbt, returnWildcard);
    }

    public static Object[] parseItemOrOreOrToolOrClass(String input, int size, NBTTagCompound data, boolean returnWildcard)
    {
        if (!input.isEmpty())
        {
            if (input.startsWith("ore:"))
                return parseOre(input.substring(4), size, data, returnWildcard);
            else if (input.startsWith("tool:"))
                return new Object[] { input.substring(5), size, returnWildcard ? OreDictionary.WILDCARD_VALUE : 0, data };
            else if (input.startsWith("class:"))
                return parseClass(input.substring(6), size, data, returnWildcard);
            else
                return parseItemAndMaybeMeta(input, size, data, returnWildcard);
        }
        else
            return new Object[] { null, size, returnWildcard ? OreDictionary.WILDCARD_VALUE : 0, data };
    }

    /**
     * The same as {@link #parseItemStackOrOreOrClass(String, int, NBTTagCompound)} but instead of an <code>Item</code>/ore name/<code>Class</code>, it's a <code>Block</code>.
     */
    public static Object[] parseBlockOrOre(String input, boolean returnWildcard)
    {
        if (input.startsWith("ore:"))
            return parseOre(input.substring(4), 1, new NBTTagCompound(), returnWildcard);
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
        catch (ClassNotFoundException e)
        {
            if (!CampfireBackportConfig.suppressInputErrors)
                CampfireBackport.proxy.modlog.warn(StatCollector.translateToLocalFormatted(Reference.MODID + ".inputerror.invalid_class", input));
            return new Object[] { null, null, null, null };
        }
    }

    public static Object[] parseOre(String input, int size, NBTTagCompound data, boolean returnWildcard)
    {
        if (OreDictionary.doesOreNameExist(input))
            return new Object[] { OreDictionary.getOreID(input), size, returnWildcard ? OreDictionary.WILDCARD_VALUE : 0, data };
        else
        {
            if (!CampfireBackportConfig.suppressInputErrors)
                CampfireBackport.proxy.modlog.warn(StatCollector.translateToLocalFormatted(Reference.MODID + ".inputerror.invalid_ore", input));
            return new Object[] { null, null, null, null };
        }
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
            return new Object[] { null, null, null, null };
    }

    public static Object[] parseBlockAndMaybeMeta(String input, int size, NBTTagCompound data, boolean returnWildcard)
    {
        Integer meta = returnWildcard ? OreDictionary.WILDCARD_VALUE : 0;

        String[] segment = input.split(":");

        if (segment.length > 2)
            meta = Integer.parseInt(segment[2]);

        Block block = GameRegistry.findBlock(segment[0], segment[1]);

        if (block != null)
            return new Object[] { block, size, meta, data };
        else
            return new Object[] { null, null, null, null };
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

        String[] segment2 = segment1[0].split(":");

        if (segment2.length > 2)
            meta = Integer.parseInt(segment2[2]);

        return GameRegistry.findItemStack(segment2[0], segment2[1], size);
    }

}
