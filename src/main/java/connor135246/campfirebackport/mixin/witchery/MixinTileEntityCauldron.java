package connor135246.campfirebackport.mixin.witchery;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.emoniph.witchery.brewing.TileEntityCauldron;

import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

/**
 * This mixin allows lit campfires to act as a heat source for Witchery cauldrons.
 */
@Mixin(TileEntityCauldron.class)
public abstract class MixinTileEntityCauldron
{

    // func_145845_h (in production environment) / updateEntity (in development environment)
    // func_147439_a is getBlock
    @Redirect(method = "func_145845_h",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;func_147439_a(III)Lnet/minecraft/block/Block;", ordinal = 0),
            remap = false)
    public Block getBlockProxy(World world, int x, int y, int z)
    {
        Block block = world.getBlock(x, y, z);
        if (CampfireBackportBlocks.isLitCampfire(block))
            return Blocks.fire;
        else
            return block;
    }

}
