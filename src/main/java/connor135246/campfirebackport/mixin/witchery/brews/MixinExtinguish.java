package connor135246.campfirebackport.mixin.witchery.brews;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.blocks.CampfireBackportBlocks;
import net.minecraft.block.Block;
import net.minecraft.world.World;

/**
 * This mixin allows campfires to be extinguished by the Extinguish brew.
 */
@Mixin(targets = "com.emoniph.witchery.brewing.WitcheryBrewRegistry$30$1")
public class MixinExtinguish
{

    // func_147439_a is getBlock
    @Inject(method = "Lcom/emoniph/witchery/brewing/WitcheryBrewRegistry$30$1;onBlock(Lnet/minecraft/world/World;III)V",
            at = @At(value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/World;func_147439_a(III)Lnet/minecraft/block/Block;"),
            locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true, remap = false)
    public void onOnBlock(World world, int x, int y, int z, CallbackInfo ci, int dy, Block block)
    {
        if (CampfireBackportBlocks.isLitCampfire(block))
        {
            BlockCampfire.extinguishCampfire(null, world, x, dy, z);
            if (!world.isRemote)
                world.playSoundEffect(x, y, z, "random.fizz", 1.0F, 2.0F);
            ci.cancel();
        }
    }

}
