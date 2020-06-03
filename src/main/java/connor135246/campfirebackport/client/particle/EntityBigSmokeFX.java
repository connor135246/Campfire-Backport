package connor135246.campfirebackport.client.particle;

import cpw.mods.fml.client.FMLClientHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import org.lwjgl.opengl.GL11;

import connor135246.campfirebackport.util.CampfireBackportEventHandler;
import connor135246.campfirebackport.util.Reference;

public class EntityBigSmokeFX extends EntityFX
{

    private int texIndex = rand.nextInt(12);

    public static ArrayDeque<EntityBigSmokeFX>[] queuedRenders = new ArrayDeque[] {
            new ArrayDeque<EntityBigSmokeFX>(), new ArrayDeque<EntityBigSmokeFX>(), new ArrayDeque<EntityBigSmokeFX>(),
            new ArrayDeque<EntityBigSmokeFX>(), new ArrayDeque<EntityBigSmokeFX>(), new ArrayDeque<EntityBigSmokeFX>(),
            new ArrayDeque<EntityBigSmokeFX>(), new ArrayDeque<EntityBigSmokeFX>(), new ArrayDeque<EntityBigSmokeFX>(),
            new ArrayDeque<EntityBigSmokeFX>(), new ArrayDeque<EntityBigSmokeFX>(), new ArrayDeque<EntityBigSmokeFX>() };

    float fScale;
    float f2;
    float f3;
    float f4;
    float f5;
    float f6;

    public EntityBigSmokeFX(World world, double x, double y, double z, boolean signalFire)
    {
        super(world, x, y, z);

        setPosition((double) x + 0.5D + rand.nextDouble() / 3.0D * (double) (rand.nextBoolean() ? 1 : -1),
                (double) y + rand.nextDouble() + rand.nextDouble(),
                (double) z + 0.5D + rand.nextDouble() / 3.0D * (double) (rand.nextBoolean() ? 1 : -1));

        setScale(3.0F * (rand.nextFloat() * 0.5F + 0.5F) * 2.0F);
        setSize(0.25F, 0.25F);
        motionY = 0.07 + (double) (this.rand.nextFloat() / 500.0F);
        this.noClip = false;

        if (signalFire)
        {
            setMaxAge(rand.nextInt(50) + 280);
            setAlphaF(0.95F);
        }
        else
        {
            setMaxAge(rand.nextInt(50) + 80);
            setAlphaF(0.9F);
        }

    }

    public static void dispatchQueuedRenders(Tessellator tess)
    {
        CampfireBackportEventHandler.bigSmokeCount = 0;

        for (int i = 0; i < queuedRenders.length; ++i)
        {
            if (!queuedRenders[i].isEmpty())
            {
                Minecraft.getMinecraft().renderEngine.bindTexture(getThisTexture(i));
                tess.startDrawingQuads();
                for (EntityBigSmokeFX smokes : queuedRenders[i])
                    smokes.renderQueued(tess);
                tess.draw();
            }
            queuedRenders[i].clear();
        }
    }

    public void renderQueued(Tessellator tess)
    {

        ++CampfireBackportEventHandler.bigSmokeCount;

        float f10 = 0.1F * particleScale;
        float f11 = (float) (prevPosX + (posX - prevPosX) * fScale - interpPosX);
        float f12 = (float) (prevPosY + (posY - prevPosY) * fScale - interpPosY);
        float f13 = (float) (prevPosZ + (posZ - prevPosZ) * fScale - interpPosZ);

        tess.setColorRGBA_F(particleRed, particleGreen, particleBlue, particleAlpha);

        tess.addVertexWithUV(f11 - f2 * f10 - f5 * f10, f12 - f3 * f10, f13 - f4 * f10 - f6 * f10, 0, 1);
        tess.addVertexWithUV(f11 - f2 * f10 + f5 * f10, f12 + f3 * f10, f13 - f4 * f10 + f6 * f10, 0, 0);
        tess.addVertexWithUV(f11 + f2 * f10 + f5 * f10, f12 + f3 * f10, f13 + f4 * f10 + f6 * f10, 1, 0);
        tess.addVertexWithUV(f11 + f2 * f10 - f5 * f10, f12 - f3 * f10, f13 + f4 * f10 - f6 * f10, 1, 1);
    }

    @Override
    public void renderParticle(Tessellator tess, float fScale, float f2, float f3, float f4, float f5, float f6)
    {
        this.fScale = fScale;
        this.f2 = f2;
        this.f3 = f3;
        this.f4 = f4;
        this.f5 = f5;
        this.f6 = f6;

        queuedRenders[this.texIndex].add(this);
    }

    @Override
    public void onUpdate()
    {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.particleAge++ < this.particleMaxAge && !(this.particleAlpha <= 0.0F))
        {
            this.motionX += (double) (this.rand.nextFloat() / 5000.0F * (float) (this.rand.nextBoolean() ? 1 : -1));
            this.motionZ += (double) (this.rand.nextFloat() / 5000.0F * (float) (this.rand.nextBoolean() ? 1 : -1));
            this.motionY -= (double) this.particleGravity;
            this.moveEntity(this.motionX, this.motionY, this.motionZ);

            if (this.particleAge >= this.particleMaxAge - 60 && this.particleAlpha > 0.01F)
                this.particleAlpha -= 0.015F;

        }
        else
            this.setDead();
    }

    public void setMaxAge(int i)
    {
        this.particleMaxAge = i;
    }

    public void setGravity(float f)
    {
        this.particleGravity = f;
    }

    public void setScale(float f)
    {
        this.particleScale = f;
    }

    public static ResourceLocation getThisTexture(int index)
    {
        return new ResourceLocation(Reference.MODID + ":" + "textures/particle/big_smoke_" + index + ".png");
    }

}
