package connor135246.campfirebackport.util;

import java.util.HashMap;
import java.util.Map;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.items.ItemBlockCampfire;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;

/**
 * Makes it easy to check if a campfire block matches variable config settings.
 */
public enum EnumCampfireType
{

    BOTH(true, true, "both"), REG_ONLY(true, false, "regular only"), SOUL_ONLY(false, true, "soul only"), NEITHER(false, false, "neither");

    // don't want to make a spelling mistake!
    public static final String regular = "regular", soul = "soul";
    public static final int regIndex = 0, soulIndex = 1;

    private final boolean acceptsRegular;
    private final boolean acceptsSoul;
    private final String stringForm;

    public static final Map<String, EnumCampfireType> FROM_NAME = new HashMap<String, EnumCampfireType>(10);

    static
    {
        FROM_NAME.put(BOTH.stringForm, BOTH);
        FROM_NAME.put(REG_ONLY.stringForm, REG_ONLY);
        FROM_NAME.put(SOUL_ONLY.stringForm, SOUL_ONLY);
        FROM_NAME.put(NEITHER.stringForm, NEITHER);
        // alternate names
        FROM_NAME.put(regular, REG_ONLY);
        FROM_NAME.put(soul, SOUL_ONLY);
    }

    EnumCampfireType(boolean acceptsRegular, boolean acceptsSoul, String stringForm)
    {
        this.acceptsRegular = acceptsRegular;
        this.acceptsSoul = acceptsSoul;
        this.stringForm = stringForm;
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
     * @return true if this accepts the types that other accepts.
     */
    public boolean accepts(EnumCampfireType other)
    {
        if (other == EnumCampfireType.REG_ONLY)
            return acceptsRegular;
        else if (other == EnumCampfireType.SOUL_ONLY)
            return acceptsSoul;
        else
            return this == other;
    }

    /**
     * @return true if this is BOTH or NEITHER, false if this is REG_ONLY or SOUL_ONLY.
     */
    public boolean sameForBoth()
    {
        return acceptsRegular == acceptsSoul;
    }

    public boolean matches(BlockCampfire cblock)
    {
        return cblock != null && matches(cblock.getTypeIndex());
    }

    public boolean matches(ItemBlockCampfire citem)
    {
        return citem != null && matches(citem.getTypeIndex());
    }

    public boolean matches(TileEntityCampfire ctile)
    {
        return ctile != null && matches(ctile.getTypeIndex());
    }

    public boolean matches(int typeIndex)
    {
        return isRegular(typeIndex) ? acceptsRegular : (isSoul(typeIndex) ? acceptsSoul : false);
    }

    public boolean matches(String type)
    {
        return isRegular(type) ? acceptsRegular : (isSoul(type) ? acceptsSoul : false);
    }

    // Static Methods

    public static boolean isRegular(String type)
    {
        return regular.equals(type);
    }

    public static boolean isRegular(int typeIndex)
    {
        return typeIndex == regIndex;
    }

    public static boolean isSoul(String type)
    {
        return soul.equals(type);
    }

    public static boolean isSoul(int typeIndex)
    {
        return typeIndex == soulIndex;
    }

    public static String fromIndex(int typeIndex)
    {
        return isSoul(typeIndex) ? soul : regular;
    }

    public static int index(String type)
    {
        return isSoul(type) ? soulIndex : regIndex;
    }

    public static int index(int typeIndex)
    {
        return isSoul(typeIndex) ? soulIndex : regIndex;
    }

    public static int index(EnumCampfireType type)
    {
        return type == SOUL_ONLY ? soulIndex : regIndex;
    }

}
