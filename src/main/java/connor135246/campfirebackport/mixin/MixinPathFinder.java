package connor135246.campfirebackport.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathPoint;

/**
 * This mixin helps entities avoid campfires when pathfinding.
 */
@Mixin(PathFinder.class)
public abstract class MixinPathFinder
{

    // int flag3 (in production environment) / boolean flag3 (in development environment)
    @Inject(method = "func_82565_a", at = @At(value = "INVOKE", ordinal = 1), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private static void onGetVerticalOffsetStatic(Entity p_82565_0_, int p_82565_1_, int p_82565_2_, int p_82565_3_, PathPoint p_82565_4_,
            boolean p_82565_5_, boolean p_82565_6_, boolean p_82565_7_, CallbackInfoReturnable<Integer> cir, int flag3, int l, int i1, int j1, Block block)
    {
        if (block instanceof BlockCampfire)
            cir.setReturnValue(!p_82565_0_.isImmuneToFire() && ((BlockCampfire) block).isLit() ? -2 : 2);
        // 2 = can walk over it without jumping even if there isn't a solid block below it
        // 1 = treats as air
        // 0 = gets stuck jumping on it (slabs are just a little bit taller than campfires, so they don't quite have this problem)
        // -1 = the same as -2, unless the entity doesn't avoid water (ex: animals following wheat) in which case it's the same as 0
        // -2 = treats as lava
        // -3 = appears to be the same as -2
        // -4 = appears to be the same as -2
    }

}
