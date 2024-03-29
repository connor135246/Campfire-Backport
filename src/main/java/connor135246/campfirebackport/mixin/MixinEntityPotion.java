package connor135246.campfirebackport.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.MathHelper;
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

    @Inject(method = "onImpact", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/EntityPotion;setDead()V", ordinal = 0))
    protected void onOnImpact(MovingObjectPosition mop, CallbackInfo ci)
    {
        int x = MathHelper.floor_double(mop.hitVec.xCoord);
        int y = MathHelper.floor_double(mop.hitVec.yCoord);
        int z = MathHelper.floor_double(mop.hitVec.zCoord);

        extinguishLitCampfireAt(x, y, z);
        extinguishLitCampfireAt(x + 1, y, z);
        extinguishLitCampfireAt(x - 1, y, z);
        extinguishLitCampfireAt(x, y, z + 1);
        extinguishLitCampfireAt(x, y, z - 1);
    }

    protected void extinguishLitCampfireAt(int i, int j, int k)
    {
        BlockCampfire.extinguishCampfire(null, this.worldObj, i, j, k);
    }

}
