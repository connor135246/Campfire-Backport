package connor135246.campfirebackport.common.compat.crafttweaker;

import connor135246.campfirebackport.common.recipes.CustomInput;
import connor135246.campfirebackport.util.MiscUtil;
import connor135246.campfirebackport.util.StringParsers;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import minetweaker.api.player.IPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * Class for functions made using {@link IngredientFunctions#drainFluid} and {@link IngredientFunctions#fillFluid}.
 */
public class FluidFunction extends AbstractItemFunction
{

    protected final String fluidName;
    protected final int amount;
    protected final boolean drain;
    protected final FluidStack asFluidStack;

    protected FluidFunction(String fluidName, int amount, boolean drain) throws Exception
    {
        if (!FluidRegistry.isFluidRegistered(fluidName))
            throw new Exception(StringParsers.translateCT("fluid.unregistered", fluidName));
        if (amount <= 0)
            throw new Exception(StringParsers.translateCT("fluid.invalid_amount", amount));

        this.fluidName = fluidName;
        this.amount = amount;
        this.drain = drain;

        this.asFluidStack = new FluidStack(FluidRegistry.getFluid(fluidName), amount);
    }

    @Override
    public boolean matches(IItemStack istack)
    {
        if (drain)
            return MiscUtil.containsFluid(MineTweakerMC.getItemStack(istack), asFluidStack);
        else
            return MiscUtil.couldContainFluid(MineTweakerMC.getItemStack(istack), asFluidStack);
    }

    @Override
    public IItemStack transform(IItemStack istack, IPlayer iplayer)
    {
        if (drain)
            return MineTweakerMC.getIItemStack(CustomInput.doFluidDraining(MineTweakerMC.getItemStack(istack), amount, MineTweakerMC.getPlayer(iplayer)));
        else
            return MineTweakerMC.getIItemStack(CustomInput.doFluidFilling(MineTweakerMC.getItemStack(istack), asFluidStack.copy(), MineTweakerMC.getPlayer(iplayer)));
    }

    @Override
    public boolean hasConditions()
    {
        return true;
    }

    @Override
    public boolean hasTransforms()
    {
        return true;
    }

    @Override
    public String toInfoString()
    {
        if (drain)
            return StringParsers.translateNEI("fluid_data", amount, asFluidStack.getLocalizedName());
        else
            return StringParsers.translateNEI("fill_fluid_data", amount, asFluidStack.getLocalizedName());
    }

    @Override
    public ItemStack modifyStackForDisplay(ItemStack stack)
    {
        if (drain)
            return MiscUtil.fillContainerWithFluid(stack, asFluidStack.copy());
        else
            return ItemStack.copyItemStack(stack);
    }

    @Override
    public String toString()
    {
        return "(Fluid Function) [Fluid:\"" + fluidName + "\",Amount:" + amount + ",Drains:" + drain + "]";
    }

}
