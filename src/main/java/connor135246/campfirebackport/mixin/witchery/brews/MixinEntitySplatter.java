package connor135246.campfirebackport.mixin.witchery.brews;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.emoniph.witchery.brewing.EntitySplatter;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

/**
 * This mixin allows campfires to be ignited by the projectiles created by the Flame brew.
 */
@Mixin(EntitySplatter.class)
public abstract class MixinEntitySplatter extends Entity
{

    public MixinEntitySplatter(World p_i1759_1_)
    {
        super(p_i1759_1_);
    }

    // func_70227_a is onImpact
    @Inject(method = "onImpact",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/util/MovingObjectPosition;field_72310_e:I", ordinal = 0),
            cancellable = true, remap = false)
    protected void onOnImpact(MovingObjectPosition mop, CallbackInfo ci)
    {
        int x = mop.blockX;
        int y = mop.blockY;
        int z = mop.blockZ;

        if (BlockCampfire.igniteOrReigniteCampfire(null, this.worldObj, x, y, z) != 0)
        {
            this.setDead();
            ci.cancel();
        }
    }

}
