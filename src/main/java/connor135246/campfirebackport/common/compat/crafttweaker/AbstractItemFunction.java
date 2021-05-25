package connor135246.campfirebackport.common.compat.crafttweaker;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;

import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemCondition;
import minetweaker.api.item.IItemStack;
import minetweaker.api.item.IItemTransformer;
import minetweaker.api.item.IngredientAny;
import minetweaker.api.item.IngredientOr;
import minetweaker.api.item.IngredientStack;
import minetweaker.api.item.IngredientUnknown;
import minetweaker.api.minecraft.MineTweakerMC;
import minetweaker.mc1710.item.MCItemStack;
import minetweaker.mc1710.liquid.MCLiquidStack;
import minetweaker.mc1710.oredict.MCOreDictEntry;
import net.minecraft.item.ItemStack;

/**
 * Abstract class for IItemConditions and/or IItemTransformers. <br>
 * Holds information about the conditions and transformers that were applied to IIngredients so that we can know their features after the fact. Mainly used for putting nice stuff
 * in NEI.
 */
public abstract class AbstractItemFunction implements IItemCondition, IItemTransformer
{

    /**
     * Returns whether this AbstractItemFunction has a condition.
     */
    public abstract boolean hasConditions();

    /**
     * Returns whether this AbstractItemFunction applies a transform.
     */
    public abstract boolean hasTransforms();

    /**
     * The value {@link #appliesReuse()} returns. Defaults to false.
     */
    protected boolean reuse = false;

    /**
     * Sets {@link #reuse} to the value.
     */
    protected void setAppliesReuse(boolean reuse)
    {
        this.reuse = reuse;
    }

    /**
     * Returns {@link #reuse}, whether this AbstractItemFunction applied a .reuse() to its IIngredient. <br>
     * If this returns true, {@link #hasTransforms()} should also return true.
     */
    public boolean appliesReuse()
    {
        return reuse;
    }

    /**
     * Returns a short line of text explaining what the AbstractItemFunction does, or an empty string if it shouldn't be represented by a line of text. <br>
     * You probably only need to override one of {@link #toInfoString()} / {@link #toNBTString()}, not both.
     */
    public String toInfoString()
    {
        return "";
    }

    /**
     * Returns the AbstractItemFunction's NBT without "tag:{ }" wrapped around it, or an empty string if it shouldn't be represented by an NBT string. <br>
     * You probably only need to override one of {@link #toInfoString()} / {@link #toNBTString()}, not both.
     */
    public String toNBTString()
    {
        return "";
    }

    /**
     * Modifies a stack to match this AbstractItemFunction, usually for displaying in NEI.
     */
    public ItemStack modifyStackForDisplay(ItemStack stack)
    {
        return stack;
    }

    /**
     * Sends to {@link #modifyStackForDisplay(ItemStack)}.
     */
    public IItemStack modifyIStackForDisplay(IItemStack istack)
    {
        return MineTweakerMC.getIItemStack(modifyStackForDisplay(MineTweakerMC.getItemStack(istack)));
    }

    // Static Methods

    /**
     * Returns true if any of the AbstractItemFunctions have {@link #appliesReuse()} return true.
     */
    public static boolean anyAppliedReuse(AbstractItemFunction[] functions)
    {
        if (functions != null)
            for (AbstractItemFunction function : functions)
                if (function.appliesReuse())
                    return true;
        return false;
    }

    /**
     * Returns a list of all the AbstractItemFunction info strings, or an empty list if none of them should be represented as info strings.
     */
    public static List<String> getInfoStrings(AbstractItemFunction[] functions)
    {
        List<String> infos = new LinkedList<String>();
        if (functions != null)
        {
            for (AbstractItemFunction function : functions)
            {
                String info = function.toInfoString();
                if (!info.isEmpty())
                    infos.add(info);
            }
        }
        return infos;
    }

    /**
     * Returns all the AbstractItemFunction NBT strings combined together, or an empty string if none of them should be represented as NBT strings.
     */
    public static String getCombinedNBTString(AbstractItemFunction[] functions)
    {
        StringBuilder combined = new StringBuilder();
        if (functions != null)
        {
            for (int i = 0; i < functions.length; ++i)
            {
                String nbtString = functions[i].toNBTString();
                if (!nbtString.isEmpty())
                {
                    if (combined.length() == 0)
                        combined.append('{');

                    if (combined.length() > 1)
                        combined.append(", ");

                    combined.append(nbtString);
                }

                if (i == functions.length - 1 && combined.length() > 1)
                    combined.append('}');
            }
        }
        return combined.toString();
    }

    /** Map of IIngredients to their AbstractItemFunctions. */
    static Map<IIngredient, AbstractItemFunction[]> iingredientsToFunctions = new HashMap<IIngredient, AbstractItemFunction[]>();

    /**
     * Adds the AbstractItemFunction to the AbstractItemFunction[] for the IIngredient. Returns the previous AbstractItemFunction[] it was mapped to, or null if there wasn't one.
     */
    public static AbstractItemFunction[] rememberFunction(IIngredient iingredient, AbstractItemFunction function)
    {
        if (function != null)
            return iingredientsToFunctions.put(iingredient, ArrayUtils.add(iingredientsToFunctions.get(iingredient), function));
        else
            return null;
    }

    /**
     * Adds all the AbstractItemFunctions to the AbstractItemFunction[] for the IIngredient. Returns the previous AbstractItemFunction[] it was mapped to, or null if there wasn't
     * one.
     */
    public static AbstractItemFunction[] rememberFunctions(IIngredient iingredient, AbstractItemFunction[] functions)
    {
        if (functions != null)
            return iingredientsToFunctions.put(iingredient, ArrayUtils.addAll(iingredientsToFunctions.get(iingredient), functions));
        else
            return null;
    }

    /**
     * Removes the mapping for the IIngredient. Returns the AbstractItemFunction[] it was mapped to, or null if there wasn't one.
     */
    public static AbstractItemFunction[] forgetFunctions(IIngredient iingredient)
    {
        return iingredientsToFunctions.remove(iingredient);
    }

    /**
     * Clears all mappings of IIngredient to AbstractItemFunction[].
     */
    public static void clearFunctions()
    {
        iingredientsToFunctions.clear();
    }

    private static final AbstractItemFunction[] EMPTY_ARRAY = new AbstractItemFunction[0];

    /**
     * Checks the conditions/transforms of the IIngredient in order to find the AbstractItemFunctions that have been applied to it, saves the results in the map
     * {@link #iingredientsToFunctions}, and returns them. <br>
     * If the IIngredient didn't have any AbstractItemFunctions that have been applied to it, returns an empty array. <br>
     * Uses reflection since you can't normally access anything about the conditions/transforms that have been applied to an IIngredient.
     */
    public static AbstractItemFunction[] getFunctions(IIngredient iingredient)
    {
        if (hasIFunctions(iingredient))
        {
            AbstractItemFunction[] functions = iingredientsToFunctions.get(iingredient);
            if (functions != null)
                return functions;

            Set<AbstractItemFunction> functionSet = new HashSet<AbstractItemFunction>();

            for (IItemCondition condition : getIConditions(iingredient))
                if (condition instanceof AbstractItemFunction)
                    functionSet.add((AbstractItemFunction) condition);

            for (IItemTransformer transformer : getITransformers(iingredient))
                if (transformer instanceof AbstractItemFunction)
                    functionSet.add((AbstractItemFunction) transformer);

            if (functionSet.size() > 0)
            {
                functions = functionSet.toArray(new AbstractItemFunction[functionSet.size()]);
                rememberFunctions(iingredient, functions);
                return functions;
            }
        }
        return EMPTY_ARRAY;
    }

    /**
     * @return false if the IIngredient is one of the base classes that doesn't have IItemConditions/IItemTransformers, true otherwise
     */
    public static boolean hasIFunctions(IIngredient iingredient)
    {
        Class clazz = iingredient.getClass();

        return !(clazz == MCItemStack.class || clazz == MCOreDictEntry.class || clazz == MCLiquidStack.class || clazz == IngredientAny.class
                || clazz == IngredientUnknown.class);
    }

    /**
     * @return the IIngredient's IItemCondition array, or an empty array if it doesn't have one or a reflection error occurs
     */
    public static IItemCondition[] getIConditions(IIngredient iingredient)
    {
        Object iconditions = reflectIngredientField(iingredient, "conditions");
        if (iconditions instanceof IItemCondition[])
            return (IItemCondition[]) iconditions;
        else
            return new IItemCondition[0];
    }

    /**
     * @return the IIngredient's IItemTransformer array, or an empty array if it doesn't have one or a reflection error occurs
     */
    public static IItemTransformer[] getITransformers(IIngredient iingredient)
    {
        Object itransformers = reflectIngredientField(iingredient, "transformers");
        if (itransformers instanceof IItemTransformer[])
            return (IItemTransformer[]) itransformers;
        else
            return new IItemTransformer[0];
    }

    /**
     * Reflects the IIngredient to get the value of the field. <br>
     * Returns null if a reflection error occurs.
     */
    public static Object reflectIngredientField(IIngredient iingredient, String fieldName)
    {
        try
        {
            iingredient = reflectIngredientStackInternal(iingredient);

            Class clazz = iingredient.getClass();
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(iingredient);
        }
        catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException excep)
        {
            ;
        }

        return null;
    }

    /**
     * If the IIngredient is an IngredientStack, returns its internal IIngredient. <br>
     * Otherwise, just returns the IIngredient.
     */
    public static IIngredient reflectIngredientStackInternal(IIngredient iingredient)
    {
        try
        {
            IIngredient internal = iingredient;

            Class clazz = internal.getClass();

            if (clazz == IngredientStack.class)
            {
                Field ingStackIngredientField = IngredientStack.class.getDeclaredField("ingredient");
                ingStackIngredientField.setAccessible(true);

                while (clazz == IngredientStack.class)
                {
                    internal = (IIngredient) ingStackIngredientField.get(internal);
                    clazz = internal.getClass();
                }

                return internal;
            }
        }
        catch (ClassCastException | NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e)
        {
            ;
        }

        return iingredient;
    }

    // Unused - IngredientOrs are just weird to work with, the simple answer is not to support them
    /**
     * If the IIngredient is an IngredientOr, returns its internal IIngredients. <br>
     * Otherwise, just returns the IIngredient in an array.
     */
    public static IIngredient[] reflectIngredientOrInternals(IIngredient iingredient)
    {
        try
        {
            Class clazz = iingredient.getClass();

            if (clazz == IngredientOr.class)
            {
                Field field = clazz.getDeclaredField("elements");
                field.setAccessible(true);
                return (IIngredient[]) field.get(iingredient);
            }
        }
        catch (ClassCastException | NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException excep)
        {
            ;
        }

        return new IIngredient[] { iingredient };
    }

}