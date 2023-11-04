package connor135246.campfirebackport.mixin.witchery.symbols;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.emoniph.witchery.entity.EntitySpellEffect;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

/**
 * This mixin allows campfires to be extinguished by the Aguamenti symbol. It must be level 1 (level 3 in the nether).
 */
@Mixin(targets = "com.emoniph.witchery.infusion.infusions.symbols.EffectRegistry$11")
public abstract class MixinAguamenti
{

    // func_147439_a is getBlock
    @Inject(method = "Lcom/emoniph/witchery/infusion/infusions/symbols/EffectRegistry$11;onCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/util/MovingObjectPosition;Lcom/emoniph/witchery/entity/EntitySpellEffect;)V",
            at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/World;func_147439_a(III)Lnet/minecraft/block/Block;", ordinal = 0),
            cancellable = true, remap = false)
    public void onOnCollision(World world, EntityLivingBase caster, MovingObjectPosition mop, EntitySpellEffect spell, CallbackInfo ci)
    {
        if (BlockCampfire.extinguishCampfire(null, world, mop.blockX, mop.blockY, mop.blockZ) != 0)
            ci.cancel();
    }

}
