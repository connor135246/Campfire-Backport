package connor135246.campfirebackport.mixin.thaumcraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import thaumcraft.common.tiles.TileCrucible;

/**
 * This mixin allows lit campfires to act as a heat source for Thaumcraft crucibles.
 */
@Mixin(TileCrucible.class)
public abstract class MixinTileCrucible
{

    // func_145845_h (in production environment) / updateEntity (in development environment)
    // func_149688_o is getMaterial
    @Redirect(method = "func_145845_h",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;func_149688_o()Lnet/minecraft/block/material/Material;", ordinal = 0),
            remap = false)
    public Material getMaterialProxy(Block block)
    {
        if (CampfireBackportBlocks.isLitCampfire(block))
            return Material.fire;
        else
            return block.getMaterial();
    }

}
