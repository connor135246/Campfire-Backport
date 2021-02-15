package connor135246.campfirebackport.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.items.ItemBlockCampfire;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;

/**
 * Makes it easy to check if a campfire block matches variable config settings.
 */
public enum EnumCampfireType
{

    NEITHER(false, false, "neither"), REG_ONLY(true, false, "regular only"), SOUL_ONLY(false, true, "soul only"), BOTH(true, true, "both");

    // don't want to make a spelling mistake!
    public static final String regular = "regular", soul = "soul";

    private final boolean acceptsRegular;
    private final boolean acceptsSoul;
    private final String stringForm;
    private final List<String> asList;

    public static final Map<String, EnumCampfireType> FROM_NAME = new HashMap<String, EnumCampfireType>(10);

    static
    {
        FROM_NAME.put(NEITHER.stringForm, NEITHER);
        FROM_NAME.put(REG_ONLY.stringForm, REG_ONLY);
        FROM_NAME.put(SOUL_ONLY.stringForm, SOUL_ONLY);
        FROM_NAME.put(BOTH.stringForm, BOTH);
        // alternate names
        FROM_NAME.put(regular, REG_ONLY);
        FROM_NAME.put(soul, SOUL_ONLY);
    }

    EnumCampfireType(boolean acceptsRegular, boolean acceptsSoul, String stringForm)
    {
        this.acceptsRegular = acceptsRegular;
        this.acceptsSoul = acceptsSoul;
        this.stringForm = stringForm;

        if (acceptsRegular && acceptsSoul)
            asList = ImmutableList.of(regular, soul);
        else if (acceptsRegular)
            asList = ImmutableList.of(regular);
        else if (acceptsSoul)
            asList = ImmutableList.of(soul);
        else
            asList = ImmutableList.of();
    }

    @Override
    public String toString()
    {
        return stringForm;
    }

    public boolean acceptsRegular()
    {
        return acceptsRegular;
    }

    public boolean acceptsSoul()
    {
        return acceptsSoul;
    }

    /**
     * @return a list containing the names of the types of campfires this enum accepts.
     */
    public List<String> asList()
    {
        return asList;
    }

    /**
     * @return an array containing the names of the types of campfires this enum accepts.
     */
    public String[] asArray()
    {
        return (String[]) asList.toArray();
    }

    public boolean matches(BlockCampfire cblock)
    {
        return cblock != null && matches(cblock.getType());
    }

    public boolean matches(ItemBlockCampfire citem)
    {
        return citem != null && matches(citem.getType());
    }

    public boolean matches(TileEntityCampfire ctile)
    {
        return ctile != null && matches(ctile.getType());
    }

    public boolean matches(String type)
    {
        return isRegular(type) ? acceptsRegular : (isSoul(type) ? acceptsSoul : false);
    }

    // Static Methods

    public static boolean isRegular(String type)
    {
        return type != null && type.equals(regular);
    }

    public static boolean isSoul(String type)
    {
        return type != null && type.equals(soul);
    }

    /**
     * sometimes we use arrays where the first element is for regular campfires and the second is for soul campfires.
     */
    public static int index(String type)
    {
        return isSoul(type) ? 1 : 0;
    }

    /**
     * @return regularOption if type is "regular", soulOption if type is "soul". if type is neither, regularOption.
     */
    public static <E> E option(String type, E regularOption, E soulOption)
    {
        return isSoul(type) ? soulOption : regularOption;
    }

}
