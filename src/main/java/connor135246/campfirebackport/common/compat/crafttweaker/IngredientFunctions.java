package connor135246.campfirebackport.common.compat.crafttweaker;

import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IngredientTransform;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenExpansion("minetweaker.item.IIngredient")
public class IngredientFunctions
{

    /**
     * ZenScript method that can be used on an IIngredient. <br>
     * Puts a condition on the IIngredient that works similarly to .onlyWithTag(tag) but with extra options. <br>
     * May put a transform on the IIngredient that consumes NBT. Then, if reuse is true (default), applies a .reuse() to the IIngredient, unless there's already an
     * AbstractItemFunction that applied a .reuse() to it earlier.
     */
    @ZenMethod
    public static IIngredient onlyWithTagAdvanced(IIngredient iingredient, String input, @Optional Boolean reuse) throws Exception
    {
        return addFunction(iingredient, new TagFunction(input), reuse == null || reuse);
    }

    /**
     * ZenScript method that can be used on an IIngredient. <br>
     * Puts a condition on the IIngredient that checks for a fluid. <br>
     * Puts a transform on the IIngredient that consumes the fluid. Then, if reuse is true (default), applies a .reuse() to the IIngredient, unless there's already an
     * AbstractItemFunction that applied a .reuse() to it earlier.
     */
    @ZenMethod
    public static IIngredient transformFluid(IIngredient iingredient, String fluidName, int minAmount, @Optional Boolean reuse)
            throws Exception
    {
        return addFunction(iingredient, new FluidFunction(fluidName, minAmount), reuse == null || reuse);
    }

    /**
     * Actually puts the conditions/transforms on the IIngredient, and saves it to the AbstractItemFunction map. <br>
     * If reuse is true, applies a .reuse() to the IIngredient, unless there's already an AbstractItemFunction that applied a .reuse() to it earlier.
     */
    private static IIngredient addFunction(IIngredient iingredient, AbstractItemFunction function, boolean reuse)
    {
        if (function.hasConditions() || function.hasTransforms())
        {
            if (reuse && AbstractItemFunction.anyAppliedReuse(AbstractItemFunction.getFunctions(iingredient)))
                reuse = false;

            AbstractItemFunction.forgetFunctions(iingredient);

            if (function.hasConditions())
                iingredient = iingredient.only(function);

            if (function.hasTransforms())
            {
                iingredient = iingredient.transform(function);

                function.setAppliesReuse(reuse);

                if (function.appliesReuse())
                    iingredient = IngredientTransform.reuse(iingredient);
            }

            AbstractItemFunction.rememberFunction(iingredient, function);
        }

        return iingredient;
    }

}