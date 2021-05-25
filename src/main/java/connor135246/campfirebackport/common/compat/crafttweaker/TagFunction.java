package connor135246.campfirebackport.common.compat.crafttweaker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.lang3.StringUtils;

import connor135246.campfirebackport.util.MiscUtil;
import connor135246.campfirebackport.util.StringParsers;
import minetweaker.MineTweakerAPI;
import minetweaker.api.data.DataByte;
import minetweaker.api.data.DataByteArray;
import minetweaker.api.data.DataDouble;
import minetweaker.api.data.DataFloat;
import minetweaker.api.data.DataInt;
import minetweaker.api.data.DataIntArray;
import minetweaker.api.data.DataList;
import minetweaker.api.data.DataLong;
import minetweaker.api.data.DataMap;
import minetweaker.api.data.DataShort;
import minetweaker.api.data.DataString;
import minetweaker.api.data.IData;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import minetweaker.api.player.IPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * Class for functions made using {@link IngredientFunctions#onlyWithTagAdvanced}.
 */
public class TagFunction extends AbstractItemFunction
{

    protected final CompareDataBase cdata;
    protected final boolean hasTransforms;
    protected final NBTTagCompound asNBT;

    protected TagFunction(String input) throws Exception
    {
        try
        {
            this.cdata = constructComparison(input);
            this.hasTransforms = cdata.hasTransforms();

            NBTBase nbt = cdata.asNBT();
            if (nbt instanceof NBTTagCompound)
                this.asNBT = (NBTTagCompound) nbt;
            else
                throw new NBTException(StringParsers.translateCT("nbt.not_compound"));
        }
        catch (NBTException excep)
        {
            MineTweakerAPI.logError(StringParsers.translateCT("nbt.caught_error", input), excep);
            throw new Exception(StringParsers.translateCT("nbt.error"));
        }
    }

    @Override
    public boolean matches(IItemStack istack)
    {
        return istack != null && istack.getTag() != null && cdata.matches(istack.getTag());
    }

    @Override
    public IItemStack transform(IItemStack istack, IPlayer iplayer)
    {
        if (istack != null)
        {
            IItemStack transformedIStack = istack.updateTag(cdata.transform(istack.getTag()));

            if (transformedIStack.getTag() instanceof DataMap && transformedIStack.getTag().length() == 0)
                transformedIStack = transformedIStack.withTag(null);

            return transformedIStack;
        }
        else
            return istack;
    }

    @Override
    public boolean hasConditions()
    {
        return true;
    }

    @Override
    public boolean hasTransforms()
    {
        return hasTransforms;
    }

    @Override
    public String toNBTString()
    {
        String thisString = cdata.toString();
        return thisString.substring(6, thisString.length() - 1);
    }

    @Override
    public ItemStack modifyStackForDisplay(ItemStack stack)
    {
        if (stack != null)
            stack.setTagCompound(MiscUtil.mergeNBT(stack.getTagCompound(), asNBT));

        return stack;
    }

    @Override
    public String toString()
    {
        return "(Tag Function) " + cdata.toString();
    }

    // --- the rest is mostly copy-pasted from {@link net.minecraft.nbt.JsonToNBT} ---

    /**
     * Constructs a CompareData from the input.<br>
     * The input should have only one top tag, which should be an unnamed compound.
     */
    public static CompareDataBase constructComparison(String input) throws NBTException
    {
        input = input.trim();

        if (!input.startsWith("{"))
            throw new NBTException(StringParsers.translateCT("nbt.not_compound"));
        else if (verifyNBTAndCountTopTags(input) > 1)
            throw new NBTException(StringParsers.translateCT("nbt.multiple_tops"));
        else
            return parseNBT("tag", input);
    }

    /**
     * Verifies the NBT doesn't have unbalanced brackets or quotations. Returns the number of top tags.
     */
    private static int verifyNBTAndCountTopTags(String input) throws NBTException
    {
        int counter = 0;
        boolean withinString = false;
        Stack stack = new Stack();

        for (int index = 0; index < input.length(); ++index)
        {
            char ch = input.charAt(index);

            if (ch == 34) // 34 = "
            {
                if (index > 0 && input.charAt(index - 1) == 92) // 92 = \
                {
                    if (!withinString)
                        throw new NBTException(StringParsers.translateCT("nbt.illegal_escaped_quote", input));
                }
                else
                    withinString = !withinString;
            }
            else if (!withinString)
            {
                if (ch != 123 && ch != 91) // 123 = { . 91 = [
                {
                    if (ch == 125 && (stack.isEmpty() || ((Character) stack.pop()).charValue() != 123)) // 125 = }
                        throw new NBTException(StringParsers.translateCT("nbt.unbalanced_brackets", "{}", input));

                    if (ch == 93 && (stack.isEmpty() || ((Character) stack.pop()).charValue() != 91)) // 93 = ]
                        throw new NBTException(StringParsers.translateCT("nbt.unbalanced_brackets", "[]", input));
                }
                else
                {
                    if (stack.isEmpty())
                        ++counter;

                    stack.push(Character.valueOf(ch));
                }
            }
        }

        if (withinString)
            throw new NBTException(StringParsers.translateCT("nbt.unbalanced_quotation", input));
        else if (!stack.isEmpty())
            throw new NBTException(StringParsers.translateCT("nbt.unbalanced_brackets", "", input));
        else if (counter == 0 && !input.isEmpty())
            return 1;
        else
            return counter;
    }

    /**
     * Recursively parses NBT.
     */
    private static CompareDataBase parseNBT(String name, String value) throws NBTException
    {
        value = value.trim();
        verifyNBTAndCountTopTags(value);
        String nbtPair;
        String nbtName;
        String nbtValue;
        char ch;

        if (value.startsWith("{"))
        {
            if (!value.endsWith("}"))
                throw new NBTException(StringParsers.translateCT("nbt.missing_bracket", value));
            else
            {
                CompareDataMap compound = new CompareDataMap(name);

                if (value.length() > 2)
                {
                    value = value.substring(1, value.length() - 1);

                    while (value.length() > 0)
                    {
                        nbtPair = getFirstNBTPair(value, false);

                        if (nbtPair.length() > 0)
                        {
                            nbtName = getNBTName(nbtPair, false);
                            nbtValue = getNBTValue(nbtPair, false);
                            compound.cdatas.add(parseNBT(nbtName, nbtValue));

                            if (value.length() < nbtPair.length() + 1)
                                break;

                            ch = value.charAt(nbtPair.length());

                            if (ch != 44 && ch != 123 && ch != 125 && ch != 91 && ch != 93) // 44 = , . 123 = { . 125 = } . 91 = [ . 93 = ]
                                throw new NBTException(StringParsers.translateCT("nbt.unexpected_char", ch, value.substring(nbtPair.length())));

                            value = value.substring(nbtPair.length() + 1);
                        }
                    }
                }

                return compound;
            }
        }
        else if (value.startsWith("["))
        {
            if (!value.endsWith("]"))
                throw new NBTException(StringParsers.translateCT("nbt.missing_bracket", value));
            else if (value.matches("\\[[-\\db,\\s(mincostmaxany)]+\\]"))
            {
                try
                {
                    value = value.substring(1, value.length() - 1);

                    String[] values = value.split(",");
                    IData array = null;
                    int[] comparisonModes = new int[values.length];
                    boolean isByteArray = false;

                    for (int i = 0; i < values.length; ++i)
                    {
                        String element = values[i].trim();
                        comparisonModes[i] = getComparisonMode(element);
                        element = comparisonModes[i] != 0 ? element.substring(0, element.length() - (comparisonModes[i] == 2 ? 6 : 5)).trim() : element;

                        if (i == 0)
                        {
                            isByteArray = element.toLowerCase().endsWith("b");
                            array = isByteArray ? new DataByteArray(new byte[values.length], false) : new DataIntArray(new int[values.length], false);
                        }
                        else if (isByteArray != element.toLowerCase().endsWith("b"))
                            throw new NBTException(StringParsers.translateCT("nbt.inconsistent_array_type", value));

                        array.setAt(i, isByteArray ? new DataByte(Byte.parseByte(element.substring(0, element.length() - 1)))
                                : new DataInt(Integer.parseInt(element)));
                    }

                    return new CompareDataArray(name, array, comparisonModes);
                }
                catch (NumberFormatException excep)
                {
                    throw new NBTException("NumberFormatException: " + excep.getLocalizedMessage());
                }
            }
            else
            {
                CompareDataList list = new CompareDataList(name);

                if (value.length() > 2)
                {
                    value = value.substring(1, value.length() - 1);

                    byte listType = 0;

                    while (value.length() > 0)
                    {
                        nbtPair = getFirstNBTPair(value, true);

                        if (nbtPair.length() > 0)
                        {
                            nbtName = getNBTName(nbtPair, true);
                            nbtValue = getNBTValue(nbtPair, true);

                            CompareDataBase cdata = parseNBT(nbtName, nbtValue);

                            byte cdataType = cdata.asNBT().getId();
                            if (listType == 0)
                                listType = cdataType;
                            else if (cdataType != listType)
                            {
                                throw new NBTException(StringParsers.translateCT("nbt.list_type_mismatch",
                                        value, NBTBase.NBTTypes[cdataType], NBTBase.NBTTypes[listType]));
                            }

                            list.cdatas.add(cdata);

                            if (value.length() < nbtPair.length() + 1)
                                break;

                            ch = value.charAt(nbtPair.length());

                            if (ch != 44 && ch != 123 && ch != 125 && ch != 91 && ch != 93) // 44 = , . 123 = { . 125 = } . 91 = [ . 93 = ]
                                throw new NBTException(StringParsers.translateCT("nbt.unexpected_char", ch, value.substring(nbtPair.length())));

                            value = value.substring(nbtPair.length() + 1);
                        }
                        else
                            MineTweakerAPI.logError(value);
                    }
                }

                return list;
            }
        }
        else
        {
            int comparisonMode = getComparisonMode(value);
            value = comparisonMode != 0 ? value.substring(0, value.length() - (comparisonMode == 2 ? 6 : 5)).trim() : value;

            IData data;
            try
            {
                if (value.matches("[-+]?[0-9]*\\.?[0-9]+[dD]"))
                    data = new DataDouble(Double.parseDouble(value.substring(0, value.length() - 1)));
                else if (value.matches("[-+]?[0-9]*\\.?[0-9]+[fF]"))
                    data = new DataFloat(Float.parseFloat(value.substring(0, value.length() - 1)));
                else if (value.matches("[-+]?[0-9]+[bB]"))
                    data = new DataByte(Byte.parseByte(value.substring(0, value.length() - 1)));
                else if (value.matches("[-+]?[0-9]+[lL]"))
                    data = new DataLong(Long.parseLong(value.substring(0, value.length() - 1)));
                else if (value.matches("[-+]?[0-9]+[sS]"))
                    data = new DataShort(Short.parseShort(value.substring(0, value.length() - 1)));
                else if (value.matches("[-+]?[0-9]+"))
                    data = new DataInt(Integer.parseInt(value.substring(0, value.length())));
                else if (value.matches("[-+]?[0-9]*\\.?[0-9]+"))
                    data = new DataDouble(Double.parseDouble(value.substring(0, value.length())));
                else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false"))
                    data = new DataByte((byte) (Boolean.parseBoolean(value) ? 1 : 0));
                else
                {
                    if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 2)
                        value = value.substring(1, value.length() - 1);

                    value = value.replaceAll("\\\\\"", "\"");
                    data = new DataString(value);
                }
            }
            catch (NumberFormatException excep)
            {
                value = value.replaceAll("\\\\\"", "\"");
                data = new DataString(value);
            }

            return new CompareDataValue(name, data, comparisonMode);
        }
    }

    /**
     * Gets the first whole NBT.<br>
     * If listMode is true, the NBT doesn't have to be strictly a name-value pair.
     */
    private static String getFirstNBTPair(String input, boolean listMode) throws NBTException
    {
        int colonIndex = findChar(input, ':');

        if (colonIndex < 0 && !listMode)
            throw new NBTException(StringParsers.translateCT("nbt.missing_name_value_sep", input));
        else
        {
            int commaIndex = findChar(input, ',');

            if (commaIndex >= 0 && commaIndex < colonIndex && !listMode)
                throw new NBTException(StringParsers.translateCT("nbt.name_error", input));
            else
            {
                if (listMode && (colonIndex < 0 || commaIndex < colonIndex))
                    colonIndex = -1;

                Stack stack = new Stack();
                int index = colonIndex + 1;
                int previousIndexOfEndingQuotation = 0;
                boolean withinString = false;
                boolean startedWith34 = false;
                boolean firstLoop = true;

                for (; index < input.length(); ++index)
                {
                    char ch = input.charAt(index);

                    if (ch == 34) // 34 = "
                    {
                        if (index > 0 && input.charAt(index - 1) == 92) // 92 = \
                        {
                            if (!withinString)
                                throw new NBTException(StringParsers.translateCT("nbt.illegal_escaped_quote", input));
                        }
                        else
                        {
                            withinString = !withinString;

                            if (withinString && firstLoop)
                                startedWith34 = true;

                            if (!withinString)
                                previousIndexOfEndingQuotation = index;
                        }
                    }
                    else if (!withinString)
                    {
                        if (ch != 123 && ch != 91) // 123 = {, 91 = [
                        {
                            if (ch == 125 && (stack.isEmpty() || ((Character) stack.pop()).charValue() != 123)) // 125 = }
                                throw new NBTException(StringParsers.translateCT("nbt.unbalanced_brackets", "{}", input));

                            if (ch == 93 && (stack.isEmpty() || ((Character) stack.pop()).charValue() != 91)) // 93 = ]
                                throw new NBTException(StringParsers.translateCT("nbt.unbalanced_brackets", "[]", input));

                            if (ch == 44 && stack.isEmpty()) // 44 = ,
                                return input.substring(0, index);
                        }
                        else
                            stack.push(Character.valueOf(ch));
                    }

                    if (!Character.isWhitespace(ch))
                    {
                        if (!withinString && startedWith34 && previousIndexOfEndingQuotation != index)
                            return input.substring(0, previousIndexOfEndingQuotation + 1);

                        firstLoop = false;
                    }
                }

                return input.substring(0, index);
            }
        }
    }

    /**
     * Gets the name of the given NBT.<br>
     * If listMode is true, the NBT doesn't have to be strictly a name-value pair (and if that's the case, returns an empty string).
     */
    private static String getNBTName(String input, boolean listMode) throws NBTException
    {
        if (listMode)
        {
            input = input.trim();

            if (input.startsWith("{") || input.startsWith("["))
                return "";
        }

        int i = input.indexOf(58); // 58 = :

        if (i < 0)
        {
            if (listMode)
                return "";
            else
                throw new NBTException(StringParsers.translateCT("nbt.missing_name_value_sep", input));
        }
        else
            return input.substring(0, i).trim();
    }

    /**
     * Gets the value of the given NBT.<br>
     * If listMode is true, the NBT doesn't have to be strictly a name-value pair (and if that's the case, returns input).
     */
    private static String getNBTValue(String input, boolean listMode) throws NBTException
    {
        if (listMode)
        {
            input = input.trim();

            if (input.startsWith("{") || input.startsWith("["))
                return input;
        }

        int i = input.indexOf(58); // 58 = :

        if (i < 0)
        {
            if (listMode)
                return input;
            else
                throw new NBTException(StringParsers.translateCT("nbt.missing_name_value_sep", input));
        }
        else
            return input.substring(i + 1).trim();
    }

    /**
     * Finds the index of the first occurrence of targetChar.<br>
     * Won't search in quotations, and will stop if it encounters a sub-compound or sub-list.
     */
    private static int findChar(String input, char targetChar)
    {
        int index = 0;

        for (boolean withinString = false; index < input.length(); ++index)
        {
            char ch = input.charAt(index);

            if (ch == 34) // 34 = "
            {
                if (index <= 0 || input.charAt(index - 1) != 92) // 92 = \
                    withinString = !withinString;
            }
            else if (!withinString)
            {
                if (ch == targetChar)
                    return index;

                if (ch == 123 || ch == 91) // 123 = { . 91 = [
                    return -1;
            }
        }

        return -1;
    }

    /**
     * Gets the comparison mode of the input. <br>
     * Modes <br>
     * 0 : Default. The compared must exactly match. <br>
     * 1 : Minimum. The compared must be greater than or equal to. <br>
     * 2 : Cost. The compared must be greater than or equal to. Then, the cost is consumed. <br>
     * 3 : Maximum. The compared must be less than or equal to. <br>
     * 4 : Any. The compared can be anything. <br>
     */
    private static int getComparisonMode(String input) throws NBTException
    {
        if (input.startsWith("{") || input.startsWith("["))
            throw new NBTException(StringParsers.translateCT("nbt.cannot_have_comparison", input));

        if (input.endsWith(")"))
        {
            if (input.endsWith("(min)"))
                return 1;
            else if (input.endsWith("(cost)"))
                return 2;
            else if (input.endsWith("(max)"))
                return 3;
            else if (input.endsWith("(any)"))
                return 4;
        }

        return 0;
    }

    //

    abstract static class CompareDataBase
    {
        final String key;

        public CompareDataBase(String key)
        {
            this.key = key;
        }

        abstract boolean matches(IData data);

        abstract boolean hasTransforms();

        abstract IData transform(IData data);

        abstract IData asIData();

        abstract NBTBase asNBT();

        public abstract String toString();

    }

    static class CompareDataMap extends CompareDataBase
    {
        List<CompareDataBase> cdatas = new ArrayList<CompareDataBase>();

        public CompareDataMap(String key)
        {
            super(key);
        }

        @Override
        boolean matches(IData data)
        {
            Map<String, IData> map = data.asMap();
            if (map == null)
                return false;

            for (CompareDataBase cdata : cdatas)
            {
                if (!map.containsKey(cdata.key))
                    return false;
                else if (!cdata.matches(map.get(cdata.key)))
                    return false;
            }
            return true;
        }

        @Override
        boolean hasTransforms()
        {
            for (CompareDataBase cdata : cdatas)
                if (cdata.hasTransforms())
                    return true;
            return false;
        }

        @Override
        IData transform(IData data)
        {
            Map<String, IData> tempmap = data.asMap();
            if (tempmap == null)
                return data;

            Map<String, IData> map = new HashMap<>(tempmap);

            for (CompareDataBase cdata : cdatas)
                if (map.containsKey(cdata.key) && cdata.matches(map.get(cdata.key)))
                    map.put(cdata.key, cdata.transform(map.get(cdata.key)));

            return new DataMap(map, false);
        }

        @Override
        IData asIData()
        {
            Map<String, IData> map = new HashMap<String, IData>();

            for (CompareDataBase cdata : cdatas)
                map.put(cdata.key, cdata.asIData());

            return new DataMap(map, false);
        }

        @Override
        NBTBase asNBT()
        {
            NBTTagCompound compound = new NBTTagCompound();

            for (CompareDataBase cdata : cdatas)
                compound.setTag(cdata.key, cdata.asNBT());

            return compound;
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();

            if (!StringUtils.isEmpty(key))
            {
                builder.append(key);
                builder.append(": ");
            }

            builder.append('{');

            for (int i = 0; i < cdatas.size(); ++i)
            {
                if (i != 0)
                    builder.append(", ");

                builder.append(cdatas.get(i).toString());
            }

            builder.append('}');

            return builder.toString();
        }

    }

    static class CompareDataList extends CompareDataBase
    {
        List<CompareDataBase> cdatas = new ArrayList<CompareDataBase>();

        public CompareDataList(String key)
        {
            super(key);
        }

        @Override
        boolean matches(IData data)
        {
            List<IData> list = data.asList();
            if (list == null)
                return false;

            boolean[] matched = new boolean[cdatas.size()];
            int matchedCount = 0;

            for (IData dataElement : list)
            {
                for (int i = 0; i < cdatas.size(); ++i)
                    if (matched[i] == false && cdatas.get(i).matches(dataElement))
                    {
                        matched[i] = true;
                        matchedCount++;
                        break;
                    }

                if (matchedCount == cdatas.size())
                    return true;
            }

            return false;
        }

        @Override
        boolean hasTransforms()
        {
            for (CompareDataBase cdata : cdatas)
                if (cdata.hasTransforms())
                    return true;
            return false;
        }

        @Override
        IData transform(IData data)
        {
            List<IData> templist = data.asList();
            if (templist == null)
                return data;

            List<IData> list = new ArrayList<>(templist);

            boolean[] matched = new boolean[cdatas.size()];
            int matchedCount = 0;

            for (int i = 0; i < list.size(); ++i)
            {
                for (int j = 0; j < cdatas.size(); ++j)
                {
                    if (matched[j] == false && cdatas.get(j).matches(list.get(i)))
                    {
                        list.set(i, cdatas.get(j).transform(list.get(i)));
                        matched[j] = true;
                        matchedCount++;
                        break;
                    }
                }

                if (matchedCount == cdatas.size())
                    break;
            }

            return new DataList(list, false);
        }

        @Override
        IData asIData()
        {
            List<IData> list = new ArrayList<IData>();

            for (CompareDataBase cdata : cdatas)
                list.add(cdata.asIData());

            return new DataList(list, false);
        }

        @Override
        NBTBase asNBT()
        {
            NBTTagList list = new NBTTagList();

            for (CompareDataBase cdata : cdatas)
                list.appendTag(cdata.asNBT());

            return list;
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();

            if (!StringUtils.isEmpty(key))
            {
                builder.append(key);
                builder.append(": ");
            }

            builder.append('[');

            for (int i = 0; i < cdatas.size(); ++i)
            {
                if (i != 0)
                    builder.append(", ");

                builder.append(cdatas.get(i).toString());
            }

            builder.append(']');

            return builder.toString();
        }

    }

    static class CompareDataValue extends CompareDataBase
    {
        final IData value;
        final int comparisonMode;

        public CompareDataValue(String name, IData value, int comparisonMode)
        {
            super(name);
            this.value = value;
            this.comparisonMode = comparisonMode;
        }

        @Override
        boolean matches(IData data)
        {
            if (data instanceof DataString)
                return data.equals(value);

            int compared = data.compareTo(value);

            switch (comparisonMode)
            {
            case 0:
                return compared == 0;
            case 1:
            case 2:
                return compared >= 0;
            case 3:
                return compared <= 0;
            case 4:
                return true;
            default:
                return false;
            }
        }

        @Override
        boolean hasTransforms()
        {
            return comparisonMode == 2;
        }

        @Override
        IData transform(IData data)
        {
            if (data instanceof DataString || comparisonMode != 2)
                return data;
            else
                return data.compareTo(value) >= 0 ? data.sub(value) : data.mul(new DataInt(0));
        }

        @Override
        IData asIData()
        {
            return value;
        }

        @Override
        NBTBase asNBT()
        {
            return MineTweakerMC.getNBT(value);
        }

        @Override
        public String toString()
        {
            String str = (StringUtils.isEmpty(key) ? "" : key + ": ");
            switch (comparisonMode)
            {
            default:
                return str + value;
            case 1:
                return str + value + " (min)";
            case 2:
                return str + value + " (cost)";
            case 3:
                return str + value + " (max)";
            case 4:
                return str + "*";
            }
        }

    }

    static class CompareDataArray extends CompareDataBase
    {
        final IData array;
        final int[] comparisonModes;

        public CompareDataArray(String name, IData array, int[] comparisonModes)
        {
            super(name);
            this.array = array;
            this.comparisonModes = comparisonModes;
        }

        @Override
        boolean matches(IData data)
        {
            if (data.length() < array.length())
                return false;

            for (int i = 0; i < array.length(); ++i)
            {
                int compared = data.getAt(i).compareTo(array.getAt(i));

                switch (comparisonModes[i])
                {
                case 0:
                {
                    if (!(compared == 0))
                        return false;
                    break;
                }
                case 1:
                case 2:
                {
                    if (!(compared >= 0))
                        return false;
                    break;
                }
                case 3:
                {
                    if (!(compared <= 0))
                        return false;
                    break;
                }
                case 4:
                    break;
                default:
                    return false;
                }
            }

            return true;
        }

        @Override
        boolean hasTransforms()
        {
            for (int comparisonMode : comparisonModes)
                if (comparisonMode == 2)
                    return true;

            return false;
        }

        @Override
        IData transform(IData data)
        {
            if (data.length() < array.length())
                return data;

            for (int i = 0; i < array.length(); ++i)
            {
                if (comparisonModes[i] == 2)
                {
                    if (data.getAt(i).compareTo(array.getAt(i)) >= 0)
                        data.setAt(i, data.getAt(i).sub(array.getAt(i)));
                    else
                        data.setAt(i, data.getAt(i).mul(new DataInt(0)));
                }
            }

            return data;
        }

        @Override
        IData asIData()
        {
            return array;
        }

        @Override
        NBTBase asNBT()
        {
            return MineTweakerMC.getNBT(array);
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();

            if (!StringUtils.isEmpty(key))
            {
                builder.append(key);
                builder.append(": ");
            }

            builder.append('[');

            boolean isByteArray = array instanceof DataByteArray;

            for (int i = 0; i < array.length(); ++i)
            {
                if (i != 0)
                    builder.append(", ");

                String value = isByteArray ? array.getAt(i).asByte() + "b" : array.getAt(i).asInt() + "";

                switch (comparisonModes[i])
                {
                default:
                    builder.append(value);
                    break;
                case 1:
                    builder.append(value + " (min)");
                    break;
                case 2:
                    builder.append(value + " (cost)");
                    break;
                case 3:
                    builder.append(value + " (max)");
                    break;
                case 4:
                    builder.append("*");
                    break;
                }
            }

            builder.append(']');

            return builder.toString();

        }

    }

}
