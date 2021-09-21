package connor135246.campfirebackport.client.particle;

import org.lwjgl.opengl.GL11;

import connor135246.campfirebackport.common.compat.CampfireBackportCompat;
import connor135246.campfirebackport.util.Reference;
import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;

public class EntityBigSmokeFX extends EntityFX
{

    /** there are 24 smoke textures. the first 12 are the normal ones, the next 12 are brighter copies that look better when coloured. */
    public static final int TEXTURE_COUNT = 12;
    public static final ResourceLocation TEXTURES = new ResourceLocation(Reference.MODID + ":" + "textures/particle/big_smokes.png");

    protected final int texIndex;
    protected boolean alphaFading = false;
    protected float alphaFadePerTick = -0.015F;
    protected boolean hasOxygen = true;

    /**
     * Posts a {@link EntityBigSmokeFXConstructingEvent}.
     */
    public EntityBigSmokeFX(World world, double x, double y, double z, boolean signalFire, float[] colours)
    {
        super(world, x, y, z);

        this.setPosition(x + 0.5 + (rand.nextDouble() / 3.0 * (rand.nextBoolean() ? 1 : -1)),
                y + rand.nextDouble() + rand.nextDouble(),
                z + 0.5 + (rand.nextDouble() / 3.0 * (rand.nextBoolean() ? 1 : -1)));

        this.particleScale = 6.0F * (rand.nextFloat() * 0.5F + 0.5F);
        this.setSize(0.25F, 0.25F);
        this.particleGravity = 0.000003F;
        this.motionY = 0.075 + this.rand.nextFloat() / 500.0F;
        this.noClip = false;

        if (signalFire)
        {
            this.particleMaxAge = rand.nextInt(50) + 280;
            this.particleAlpha = 0.95F;
        }
        else
        {
            this.particleMaxAge = rand.nextInt(50) + 80;
            this.particleAlpha = 0.9F;
        }

        if (colours.length == 3)
            this.setRBGColorF(colours[0], colours[1], colours[2]);

        EntityBigSmokeFXConstructingEvent constructing = new EntityBigSmokeFXConstructingEvent(this, (int) x, (int) y, (int) z);

        MinecraftForge.EVENT_BUS.post(constructing);

        if (constructing.isCanceled())
        {
            this.setDead();
            this.texIndex = 0;
            return;
        }

        this.particleRed = constructing.particleRed;
        this.particleGreen = constructing.particleGreen;
        this.particleBlue = constructing.particleBlue;
        this.motionX = constructing.motionX;
        this.motionY = constructing.motionY;
        this.motionZ = constructing.motionZ;
        this.particleGravity = constructing.particleGravity;
        this.particleAlpha = constructing.particleAlpha;
        this.alphaFading = constructing.alphaFading;
        this.alphaFadePerTick = constructing.alphaFadePerTick;
        this.particleMaxAge = constructing.particleMaxAge;

        this.texIndex = rand.nextInt(TEXTURE_COUNT) + (this.particleRed != 1.0F || this.particleGreen != 1.0F || this.particleBlue != 1.0F ? TEXTURE_COUNT : 0);
        this.setParticleTextureIndex(this.texIndex);
    }

    @Override
    public void renderParticle(Tessellator tess, float partialTicks, float rotX, float rotXZ, float rotZ, float rotYZ, float rotXY)
    {
        rotX = ActiveRenderInfo.rotationX;
        rotXZ = ActiveRenderInfo.rotationXZ;
        rotZ = ActiveRenderInfo.rotationZ;
        rotYZ = ActiveRenderInfo.rotationYZ;
        rotXY = ActiveRenderInfo.rotationXY;

        EntityLivingBase view = Minecraft.getMinecraft().renderViewEntity;
        double interpX = view.lastTickPosX + (view.posX - view.lastTickPosX) * partialTicks;
        double interpY = view.lastTickPosY + (view.posY - view.lastTickPosY) * partialTicks;
        double interpZ = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partialTicks;

        float partialPosX = (float) (prevPosX + (posX - prevPosX) * partialTicks - interpX);
        float partialPosY = (float) (prevPosY + (posY - prevPosY) * partialTicks - interpY);
        float partialPosZ = (float) (prevPosZ + (posZ - prevPosZ) * partialTicks - interpZ);

        float scale = 0.1F * particleScale;

        double minU = this.particleTextureIndexX / 4.0;
        double maxU = minU + 0.25;
        double minV = this.particleTextureIndexY / 8.0;
        double maxV = minV + 0.125;

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glDepthMask(true);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.003921569F);

        Minecraft.getMinecraft().entityRenderer.disableLightmap((double) partialTicks);

        Minecraft.getMinecraft().renderEngine.bindTexture(TEXTURES);

        tess.startDrawingQuads();

        tess.setColorRGBA_F(particleRed, particleGreen, particleBlue, particleAlpha);

        tess.addVertexWithUV(partialPosX - rotX * scale - rotYZ * scale, partialPosY - rotXZ * scale, partialPosZ - rotZ * scale - rotXY * scale, maxU, maxV);
        tess.addVertexWithUV(partialPosX - rotX * scale + rotYZ * scale, partialPosY + rotXZ * scale, partialPosZ - rotZ * scale + rotXY * scale, maxU, minV);
        tess.addVertexWithUV(partialPosX + rotX * scale + rotYZ * scale, partialPosY + rotXZ * scale, partialPosZ + rotZ * scale + rotXY * scale, minU, minV);
        tess.addVertexWithUV(partialPosX + rotX * scale - rotYZ * scale, partialPosY - rotXZ * scale, partialPosZ + rotZ * scale - rotXY * scale, minU, maxV);

        tess.draw();

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(false);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);

        Minecraft.getMinecraft().entityRenderer.enableLightmap((double) partialTicks);
    }

    @Override
    public int getFXLayer()
    {
        return 3;
    }

    @Override
    public void setParticleTextureIndex(int texIndex)
    {
        boolean bright = texIndex >= TEXTURE_COUNT;
        if (bright)
            texIndex = texIndex % TEXTURE_COUNT;
        this.particleTextureIndexX = texIndex / 8 + (bright ? 2 : 0);
        this.particleTextureIndexY = texIndex % 8;
    }

    @Override
    public void onUpdate()
    {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.particleAge % 10 == 0 && this.hasOxygen && !CampfireBackportCompat.hasOxygen(this.worldObj, Blocks.air,
                MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY), MathHelper.floor_double(this.posZ)))
        {
            this.hasOxygen = false;
            this.alphaFading = true;
            this.alphaFadePerTick = -0.09F;
        }

        if (this.particleAge++ >= this.particleMaxAge - 60)
            this.alphaFading = true;

        if (this.particleAge < this.particleMaxAge && this.particleAlpha > 0.0F)
        {
            if (this.hasOxygen)
            {
                this.motionX += this.rand.nextFloat() / 5000.0F * (this.rand.nextBoolean() ? 1 : -1);
                this.motionZ += this.rand.nextFloat() / 5000.0F * (this.rand.nextBoolean() ? 1 : -1);
            }
            else
            {
                this.motionX += this.rand.nextFloat() / 10.0F * (this.rand.nextBoolean() ? 1 : -1);
                this.motionZ += this.rand.nextFloat() / 10.0F * (this.rand.nextBoolean() ? 1 : -1);
            }

            this.motionY -= this.particleGravity;

            this.moveEntity(this.motionX, this.motionY, this.motionZ);

            if (this.alphaFading)
                this.particleAlpha = MathHelper.clamp_float(this.particleAlpha + this.alphaFadePerTick, 0.0F, 1.0F);
        }
        else
            this.setDead();
    }

    /**
     * Allows you to change various aspects of campfire smoke particles:<br>
     * - {@link #particleRed}, {@link #particleGreen}, and {@link #particleBlue} are the float r, g, b values of the smoke. <br>
     * - {@link #motionX}, {@link #motionY}, and {@link #motionZ} are the x, y, z motion of the smoke.<br>
     * - {@link #particleGravity} is the amount the smoke's y motion changes per tick. <br>
     * - {@link #particleAlpha} is the smoke's initial alpha. <br>
     * - {@link #alphaFading} is whether the smoke's alpha should currently be changing (fading in or out). <br>
     * - {@link #alphaFadePerTick} is the amount the smoke's alpha changes per tick if {@link #alphaFading} is true. Negative values make it fade out, positive values make it fade
     * back in. <br>
     * - {@link #particleMaxAge} is the maximum number of ticks the particle will live for. <br>
     * <br>
     * You're also given {@link #campfirePosition}, which is the x, y, z position of the campfire tile entity where the smoke was created. <br>
     * <br>
     * Also, {@link EntityBigSmokeFX} are only created on the client side, so you should mark your subscribe event with {@link Side#CLIENT}. <br>
     * <br>
     * This event is posted from {@link EntityBigSmokeFX#EntityBigSmokeFX} on the {@link MinecraftForge#EVENT_BUS}.<br>
     * This event is {@link Cancelable}. If canceled, the entity will be removed.<br>
     */
    @Cancelable
    public static class EntityBigSmokeFXConstructingEvent extends EntityConstructing
    {
        public final int[] campfirePosition;

        public float particleRed;
        public float particleGreen;
        public float particleBlue;
        public double motionX;
        public double motionY;
        public double motionZ;
        public float particleGravity;
        public float particleAlpha;
        public boolean alphaFading;
        public float alphaFadePerTick;
        public int particleMaxAge;

        public EntityBigSmokeFXConstructingEvent(EntityBigSmokeFX entity, int x, int y, int z)
        {
            super(entity);

            campfirePosition = new int[] { x, y, z };

            particleRed = entity.particleRed;
            particleGreen = entity.particleGreen;
            particleBlue = entity.particleBlue;
            motionX = entity.motionX;
            motionY = entity.motionY;
            motionZ = entity.motionZ;
            particleGravity = entity.particleGravity;
            particleAlpha = entity.particleAlpha;
            alphaFading = entity.alphaFading;
            alphaFadePerTick = entity.alphaFadePerTick;
            particleMaxAge = entity.particleMaxAge;
        }

    }

}
