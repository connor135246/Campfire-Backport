package connor135246.campfirebackport.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import net.minecraft.block.Block;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

/**
 * This mixin allows thrown splash potions to put out campfires where they land.
 */
@Mixin(EntityPotion.class)
public abstract class MixinEntityPotion extends EntityThrowable
{
    public MixinEntityPotion(World p_i1776_1_)
    {
        super(p_i1776_1_);
    }

    @Inject(method = "onImpact", at = @At(value = "INVOKE", ordinal = 0))
    protected void onOnImpact(MovingObjectPosition mop, CallbackInfo ci)
    {
        int x = mop.blockX;
        int y = mop.blockY;
        int z = mop.blockZ;

        int[][] posArray = new int[][] { { x, y, z }, { x, y + 1, z }, { x + 1, y + 1, z }, { x - 1, y + 1, z }, { x, y + 1, z + 1 }, { x, y + 1, z - 1 } };

        for (int[] check : posArray)
        {
            Block block = this.worldObj.getBlock(check[0], check[1], check[2]);
            if (block instanceof BlockCampfire)
            {
                BlockCampfire cblock = (BlockCampfire) block;

                if (cblock.isLit())
                    cblock.toggleCampfireBlockState(this.worldObj, check[0], check[1], check[2]);
            }
        }
    }
}