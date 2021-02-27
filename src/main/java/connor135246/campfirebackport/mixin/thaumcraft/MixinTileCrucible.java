package connor135246.campfirebackport.mixin.thaumcraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraftforge.fluids.IFluidHandler;
import thaumcraft.api.TileThaumcraft;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.wands.IWandable;
import thaumcraft.common.tiles.TileCrucible;

/**
 * This mixin allows lit campfires to act as a heat source for Thaumcraft crucibles.
 */
@Mixin(TileCrucible.class)
public abstract class MixinTileCrucible extends TileThaumcraft implements IFluidHandler, IWandable, IAspectContainer
{

    // func_145845_h (in production environment) / updateEntity (in development environment)
    @Redirect(method = "func_145845_h", at = @At(value = "INVOKE", ordinal = 3), remap = false)
    public Material getMaterialProxy(Block block)
    {
        if (block instanceof BlockCampfire && ((BlockCampfire) block).isLit())
            return Material.fire;
        else
            return block.getMaterial();
    }

}
