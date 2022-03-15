package connor135246.campfirebackport.mixin.witchery;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.emoniph.witchery.blocks.TileEntityBase;
import com.emoniph.witchery.brewing.TileEntityCauldron;

import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraftforge.fluids.IFluidHandler;

/**
 * This mixin allows lit campfires to act as a heat source for Witchery cauldrons.
 */
@Mixin(TileEntityCauldron.class)
public abstract class MixinTileEntityCauldron extends TileEntityBase implements IFluidHandler
{

    // func_145845_h (in production environment) / updateEntity (in development environment)
    @Redirect(method = "func_145845_h", at = @At(value = "INVOKE", ordinal = 1), remap = false)
    public Block getBlockProxy(World world, int x, int y, int z)
    {
        Block block = world.getBlock(x, y, z);
        if (CampfireBackportBlocks.isLitCampfire(block))
            return Blocks.fire;
        else
            return block;
    }

}
