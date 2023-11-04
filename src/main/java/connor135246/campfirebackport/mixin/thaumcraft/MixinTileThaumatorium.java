package connor135246.campfirebackport.mixin.thaumcraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import net.minecraft.inventory.ISidedInventory;
import thaumcraft.api.TileThaumcraft;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.tiles.TileThaumatorium;

/**
 * This mixin allows lit campfires to act as a heat source for Thaumcraft thaumatoriums.
 */
@Mixin(TileThaumatorium.class)
public abstract class MixinTileThaumatorium extends TileThaumcraft implements IAspectContainer, IEssentiaTransport, ISidedInventory
{

    @Inject(method = "checkHeat", at = @At(value = "HEAD"), cancellable = true, remap = false)
    public void onCheckHeat(CallbackInfoReturnable<Boolean> cir)
    {
        if (CampfireBackportBlocks.isLitCampfire(this.worldObj.getBlock(this.xCoord, this.yCoord - 2, this.zCoord)))
            cir.setReturnValue(true);
    }

}
