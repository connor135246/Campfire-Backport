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
 * Class for functions made using {@link IngredientFunctions#transformFluid}.
 */
public class FluidFunction extends AbstractItemFunction
{

    protected final String fluidName;
    protected final int minAmount;
    protected final FluidStack asFluidStack;

    protected FluidFunction(String fluidName, int minAmount) throws Exception
    {
        if (!FluidRegistry.isFluidRegistered(fluidName))
            throw new Exception(StringParsers.translateCT("fluid.unregistered", fluidName));
        if (minAmount <= 0)
            throw new Exception(StringParsers.translateCT("fluid.invalid_amount", minAmount));

        this.fluidName = fluidName;
        this.minAmount = minAmount;

        this.asFluidStack = new FluidStack(FluidRegistry.getFluid(fluidName), minAmount);
    }

    @Override
    public boolean matches(IItemStack istack)
    {
        return MiscUtil.containsFluid(MineTweakerMC.getItemStack(istack), asFluidStack);
    }

    @Override
    public IItemStack transform(IItemStack istack, IPlayer iplayer)
    {
        return MineTweakerMC.getIItemStack(CustomInput.doFluidEmptying(MineTweakerMC.getItemStack(istack), minAmount, MineTweakerMC.getPlayer(iplayer)));
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
        return StringParsers.translateNEI("fluid_data", minAmount, asFluidStack.getLocalizedName());
    }

    @Override
    public ItemStack modifyStackForDisplay(ItemStack stack)
    {
        return MiscUtil.fillContainerWithFluid(stack, asFluidStack.copy());
    }

    @Override
    public String toString()
    {
        return "(Fluid Function) [Fluid:\"" + fluidName + "\",MinAmount:" + minAmount + "]";
    }

}
