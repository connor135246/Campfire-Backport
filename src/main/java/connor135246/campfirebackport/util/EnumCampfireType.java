package connor135246.campfirebackport.util;

import java.util.HashMap;
import java.util.Map;

import connor135246.campfirebackport.common.blocks.BlockCampfire;

/**
 * Makes it easy to check if a campfire block matches variable config settings.
 */
public enum EnumCampfireType
{

    NEITHER(false, false, "neither"), REG_ONLY(true, false, "regular only"), SOUL_ONLY(false, true, "soul only"), BOTH(true, true, "both");

    // don't want to make a spelling mistake!
    public static final String REGULAR = "regular", SOUL = "soul";

    private final boolean regular;
    private final boolean soul;
    private final String stringForm;

    public static final Map<String, EnumCampfireType> campfireCheck = new HashMap<String, EnumCampfireType>(8);

    EnumCampfireType(boolean regular, boolean soul, String stringForm)
    {
        this.regular = regular;
        this.soul = soul;
        this.stringForm = stringForm;
    }

    public String toString()
    {
        return stringForm;
    }

    public static int toInt(String type)
    {
        return type.equals(SOUL) ? 1 : 0;
    }

    public boolean matches(BlockCampfire block)
    {
        return matches(block.getType());
    }

    public boolean matches(String type)
    {
        return type.equals(REGULAR) ? this.regular : (type.equals(SOUL) ? this.soul : false);
    }

    static
    {
        campfireCheck.put(NEITHER.stringForm, NEITHER);
        campfireCheck.put(REG_ONLY.stringForm, REG_ONLY);
        campfireCheck.put(SOUL_ONLY.stringForm, SOUL_ONLY);
        campfireCheck.put(BOTH.stringForm, BOTH);
    }

}
