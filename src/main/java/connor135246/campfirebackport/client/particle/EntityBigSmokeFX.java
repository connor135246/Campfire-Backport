package connor135246.campfirebackport.client.particle;

import java.util.ArrayDeque;

import connor135246.campfirebackport.util.Reference;
import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;

public class EntityBigSmokeFX extends EntityFX
{

    // botania license attribution clause, etc etc
    // damn you vazkii!!!!! but thanks

    public static int bigSmokeCount = 0;
    public static ArrayDeque<EntityBigSmokeFX>[] queuedRenders = new ArrayDeque[] {
            new ArrayDeque<EntityBigSmokeFX>(), new ArrayDeque<EntityBigSmokeFX>(), new ArrayDeque<EntityBigSmokeFX>(),
            new ArrayDeque<EntityBigSmokeFX>(), new ArrayDeque<EntityBigSmokeFX>(), new ArrayDeque<EntityBigSmokeFX>(),
            new ArrayDeque<EntityBigSmokeFX>(), new ArrayDeque<EntityBigSmokeFX>(), new ArrayDeque<EntityBigSmokeFX>(),
            new ArrayDeque<EntityBigSmokeFX>(), new ArrayDeque<EntityBigSmokeFX>(), new ArrayDeque<EntityBigSmokeFX>() };

    private int texIndex = rand.nextInt(12);
    private float fScale, f2, f3, f4, f5, f6;

    public EntityBigSmokeFX(World world, double x, double y, double z, boolean signalFire, float[] colours)
    {
        super(world, x, y, z);

        this.setPosition((double) x + 0.5D + rand.nextDouble() / 3.0D * (double) (rand.nextBoolean() ? 1 : -1),
                (double) y + rand.nextDouble() + rand.nextDouble(),
                (double) z + 0.5D + rand.nextDouble() / 3.0D * (double) (rand.nextBoolean() ? 1 : -1));

        this.particleScale = 6.0F * (rand.nextFloat() * 0.5F + 0.5F);
        this.setSize(0.25F, 0.25F);
        this.particleGravity = 0.000003F;
        this.motionY = 0.07 + (double) (this.rand.nextFloat() / 500.0F);
        this.noClip = false;

        if (signalFire)
        {
            this.particleMaxAge = rand.nextInt(50) + 280;
            this.setAlphaF(0.95F);
        }
        else
        {
            this.particleMaxAge = rand.nextInt(50) + 80;
            this.setAlphaF(0.9F);
        }

        if (colours.length == 3)
            this.setRBGColorF(colours[0], colours[1], colours[2]);

        EntityBigSmokeFXConstructingEvent constructing = new EntityBigSmokeFXConstructingEvent(this, (int) x, (int) y, (int) z);

        MinecraftForge.EVENT_BUS.post(constructing);

        if (constructing.isCanceled())
        {
            this.setDead();
            return;
        }

        if (constructing.doChange)
        {
            this.particleRed = constructing.coloursToChangeTo[0] != -1 ? constructing.coloursToChangeTo[0] : this.particleRed;
            this.particleGreen = constructing.coloursToChangeTo[1] != -1 ? constructing.coloursToChangeTo[1] : this.particleGreen;
            this.particleBlue = constructing.coloursToChangeTo[2] != -1 ? constructing.coloursToChangeTo[2] : this.particleBlue;
            this.motionX *= constructing.motionMultipliers[0];
            this.motionY *= constructing.motionMultipliers[1];
            this.motionZ *= constructing.motionMultipliers[2];
        }
    }

    public static void dispatchQueuedRenders(Tessellator tess)
    {
        bigSmokeCount = 0;

        for (int i = 0; i < queuedRenders.length; ++i)
        {
            Minecraft.getMinecraft().renderEngine.bindTexture(getThisTexture(i));
            tess.startDrawingQuads();

            EntityBigSmokeFX smoke;
            while ((smoke = queuedRenders[i].poll()) != null)
                smoke.renderQueued(tess);

            tess.draw();
        }
    }

    public void renderQueued(Tessellator tess)
    {
        ++bigSmokeCount;

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

        if (this.particleAge++ < this.particleMaxAge && this.particleAlpha > 0.0F)
        {
            this.motionX += (double) (this.rand.nextFloat() / 5000.0F * (float) (this.rand.nextBoolean() ? 1 : -1));
            this.motionZ += (double) (this.rand.nextFloat() / 5000.0F * (float) (this.rand.nextBoolean() ? 1 : -1));
            this.motionY -= (double) this.particleGravity;
            this.moveEntity(this.motionX, this.motionY, this.motionZ);

            if (this.particleAge >= this.particleMaxAge - 60)
                this.particleAlpha = Math.max(0.0F, this.particleAlpha - 0.015F);
        }
        else
            this.setDead();
    }

    public static ResourceLocation getThisTexture(int index)
    {
        return new ResourceLocation(Reference.MODID + ":" + "textures/particle/big_smoke_" + index + ".png");
    }

    /**
     * Allows you to change the colours and motion of campfire smoke particles. {@link #campfirePosition} is the x, y, z position of the <code>TileEntityCampfire</code> calling
     * this event. {@link #coloursToChangeTo} is the r, g, b values to change the smoke to. {@link #motionMultipliers} is a set of multipliers for the base x, y, z motion values.
     * For both settings, you don't have to set all of them; if you only set some, those ones will change but the others will stay the same. <br>
     * Finally, make sure to set {@link #doChange} to true for your changes to actually be used! <br>
     * <br>
     * Also, {@link EntityBigSmokeFX} are only created on the client side, so you probably should mark your subscribe event with {@link Side#CLIENT}. I think. <br>
     * <br>
     * This event is posted on the {@link MinecraftForge#EVENT_BUS}.<br>
     * This event is {@link Cancelable}. If canceled, the entity will be removed.<br>
     */
    @Cancelable
    public class EntityBigSmokeFXConstructingEvent extends EntityConstructing
    {
        public final int[] campfirePosition;

        public boolean doChange = false;
        public float[] coloursToChangeTo = new float[] { -1, -1, -1 };
        public double[] motionMultipliers = new double[] { 1, 1, 1 };

        public EntityBigSmokeFXConstructingEvent(EntityBigSmokeFX entity, int x, int y, int z)
        {
            super(entity);
            campfirePosition = new int[] { x, y, z };
        }
    }

}
