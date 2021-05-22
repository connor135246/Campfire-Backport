package connor135246.campfirebackport.client.models;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

/**
 * campfire - connor135246 Created using Tabula 4.1.1
 */
public class ModelCampfire extends ModelBase
{
    public ModelRenderer topbacklog;
    public ModelRenderer topfrontlog;
    public ModelRenderer bottomrightlog;
    public ModelRenderer bottomleftlog;
    public ModelRenderer firepit;
    public ModelRenderer fire1;
    public ModelRenderer fire2;

    public boolean renderFire = true;

    public ModelCampfire()
    {
        this.textureWidth = 40;
        this.textureHeight = 49;
        this.bottomleftlog = new ModelRenderer(this, 0, 0);
        this.bottomleftlog.setRotationPoint(3.0F, 20.0F, 8.0F);
        this.bottomleftlog.addBox(0.0F, 0.0F, 0.0F, 16, 4, 4, 0.0F);
        this.setRotateAngle(bottomleftlog, 0.0F, 1.5707963267948966F, 0.0F);
        this.firepit = new ModelRenderer(this, -16, 16);
        this.firepit.setRotationPoint(-3.0F, 23.0F, -8.0F);
        this.firepit.addBox(0.0F, 0.0F, 0.0F, 6, 1, 16, 0.0F);
        this.fire1 = new ModelRenderer(this, 0, 13);
        this.fire1.setRotationPoint(-7.0F, 7.0F, -7.0F);
        this.fire1.addBox(0.0F, 0.0F, 0.0F, 0, 16, 20, 0.0F);
        this.setRotateAngle(fire1, 0.0F, 0.7853981633974483F, 0.0F);
        this.fire2 = new ModelRenderer(this, 0, 13);
        this.fire2.setRotationPoint(7.0F, 7.0F, -7.0F);
        this.fire2.addBox(0.0F, 0.0F, 0.0F, 0, 16, 20, 0.0F);
        this.setRotateAngle(fire2, 0.0F, -0.7853981633974483F, 0.0F);
        this.bottomrightlog = new ModelRenderer(this, 0, 0);
        this.bottomrightlog.mirror = true;
        this.bottomrightlog.setRotationPoint(-3.0F, 20.0F, -8.0F);
        this.bottomrightlog.addBox(0.0F, 0.0F, 0.0F, 16, 4, 4, 0.0F);
        this.setRotateAngle(bottomrightlog, 0.0F, -1.5707963267948966F, 0.0F);
        this.topbacklog = new ModelRenderer(this, 0, 8);
        this.topbacklog.setRotationPoint(-8.0F, 17.0F, 3.0F);
        this.topbacklog.addBox(0.0F, 0.0F, 0.0F, 16, 4, 4, 0.0F);
        this.topfrontlog = new ModelRenderer(this, 0, 8);
        this.topfrontlog.mirror = true;
        this.topfrontlog.setRotationPoint(-8.0F, 17.0F, -7.0F);
        this.topfrontlog.addBox(0.0F, 0.0F, 0.0F, 16, 4, 4, 0.0F);
    }

    /**
     * renders the logs normally; renders the fire fullbright
     */
    @Override
    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5)
    {
        this.topbacklog.render(f5);
        this.topfrontlog.render(f5);
        this.bottomrightlog.render(f5);
        this.bottomleftlog.render(f5);
        this.firepit.render(f5);

        if (renderFire)
        {
            boolean lighting = GL11.glGetBoolean(GL11.GL_LIGHTING);
            boolean alphatest = GL11.glGetBoolean(GL11.GL_ALPHA_TEST);

            if (lighting)
                GL11.glDisable(GL11.GL_LIGHTING);

            if (!alphatest)
                GL11.glEnable(GL11.GL_ALPHA_TEST);

            this.fire1.render(f5);
            this.fire2.render(f5);

            if (lighting)
                GL11.glEnable(GL11.GL_LIGHTING);

            if (!alphatest)
                GL11.glDisable(GL11.GL_ALPHA_TEST);
        }
    }

    /**
     * This is a helper function from Tabula to set the rotation of model parts
     */
    public void setRotateAngle(ModelRenderer modelRenderer, float x, float y, float z)
    {
        modelRenderer.rotateAngleX = x;
        modelRenderer.rotateAngleY = y;
        modelRenderer.rotateAngleZ = z;
    }
}
