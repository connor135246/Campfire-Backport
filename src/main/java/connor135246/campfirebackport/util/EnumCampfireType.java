package connor135246.campfirebackport.util;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.EnumDifficulty;

/**
 * Makes it easy to check if a campfire block matches variable config settings.
 */
public enum EnumCampfireType
{

    BOTH(true, true, "both"), REG_ONLY(true, false, "regular only"), SOUL_ONLY(false, true, "soul only"), NEITHER(false, false, "neither");

    // don't want to make a spelling mistake!
    public static final String regular = "regular", soul = "soul", foxfire = "foxfire", shadow = "shadow";
    public static final int regIndex = 0, soulIndex = 1, foxfireIndex = 2, shadowIndex = 3;
    public static final String[] iconPrefixes = { "", soul + "_", foxfire + "_", shadow + "_" };

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
        // TODO soul-like names
        FROM_NAME.put(foxfire, SOUL_ONLY);
        FROM_NAME.put(shadow, SOUL_ONLY);
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

    public boolean matches(ICampfire campfire)
    {
        return campfire != null && matches(campfire.getTypeIndex());
    }

    public boolean matches(int typeIndex)
    {
        return isRegular(typeIndex) ? acceptsRegular : (isSoulLike(typeIndex) ? acceptsSoul : false);
    }

    // Static Methods

    public static boolean isRegular(int typeIndex)
    {
        return typeIndex == regIndex;
    }

    /**
     * Foxfire and shadow campfires are "soul-like" and inherit properties from soul campfires. <br>
     * TODO I may change this in the future to allow properties to be determined separately.
     */
    public static boolean isSoulLike(int typeIndex)
    {
        return typeIndex == soulIndex || typeIndex == foxfireIndex || typeIndex == shadowIndex;
    }

    public static boolean isValidIndex(int typeIndex)
    {
        return isRegular(typeIndex) || isSoulLike(typeIndex);
    }

    public static String fromIndex(int typeIndex)
    {
        return isSoulLike(typeIndex) ? soul : regular;
    }

    public static int index(int typeIndex)
    {
        return isSoulLike(typeIndex) ? soulIndex : regIndex;
    }

    public static int index(EnumCampfireType type)
    {
        return type == SOUL_ONLY ? soulIndex : regIndex;
    }

    // Foxfire & shadow functionality

    /**
     * @return prefix for IIcons
     */
    public static String iconPrefix(int typeIndex)
    {
        if (typeIndex < 0 || typeIndex >= iconPrefixes.length)
            return iconPrefixes[0];
        else
            return iconPrefixes[typeIndex];
    }

    /**
     * @return if this entity can be damaged by this campfire type. doesn't check CampfireBackportConfig.damaging!
     */
    public static boolean canDamage(int typeIndex, Entity entity)
    {
        if (entity instanceof EntityLivingBase)
        {
            switch (typeIndex)
            {
            default:
                return !entity.isImmuneToFire();
            case foxfireIndex:
                return entity.isImmuneToFire();
            case shadowIndex:
                return !((EntityLivingBase) entity).isEntityUndead();
            }
        }
        return false;
    }

    /**
     * tries to do damage effects to the entity with this campfire type. returns true if damage was dealt.
     */
    public static boolean doDamage(int typeIndex, Entity entity, EnumDifficulty difficulty)
    {
        if (entity instanceof EntityLivingBase && canDamage(typeIndex, entity))
        {
            switch (typeIndex)
            {
            default:
                return entity.attackEntityFrom(DamageSource.inFire, EnumCampfireType.isSoulLike(typeIndex) ? 2.0F : 1.0F);
            case foxfireIndex:
                return entity.attackEntityFrom(DamageSource.inFire, 1.0F);
            case shadowIndex:
            {
                int blindnessTimer = difficulty == EnumDifficulty.HARD ? 10 : (difficulty == EnumDifficulty.NORMAL ? 5 : 3);
                ((EntityLivingBase) entity).addPotionEffect(new PotionEffect(Potion.blindness.id, blindnessTimer * 20));
                return entity.attackEntityFrom(DamageSource.wither, 3.0F);
            }
            }
        }
        return false;
    }

}
