package connor135246.campfirebackport.mixin;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntitySmallFireball;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

@Mixin(EntitySmallFireball.class)
public abstract class MixinEntitySmallFireball extends EntityFireball
{

    public MixinEntitySmallFireball(World p_i1759_1_)
    {
        super(p_i1759_1_);
        // TODO Auto-generated constructor stub
    }

    @Inject(method = "onImpact",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/util/MovingObjectPosition;blockX:I", ordinal = 0), cancellable = true)
    protected void onOnImpact(MovingObjectPosition mop, CallbackInfo ci)
    {
        boolean mobgriefing = this.worldObj.getGameRules().getGameRuleBooleanValue("mobGriefing");

        if (this.shootingEntity == null || mobgriefing)
        {
            int x = mop.blockX;
            int y = mop.blockY;
            int z = mop.blockZ;

            Block block = this.worldObj.getBlock(x, y, z);

            if (block instanceof BlockCampfire)
            {
                if (!((BlockCampfire) block).isLit())
                {
                    BlockCampfire.updateCampfireBlockState(true, worldObj, x, y, z, ((BlockCampfire)block).getType());
                    this.setDead();
                    ci.cancel();
                }
            }
        }

        // vanilla bugfix - blazes can still light fires when mobgriefing is false
        if (!mobgriefing && this.shootingEntity != null)
        {
            this.setDead();
            ci.cancel();
        }
    }
    
}
