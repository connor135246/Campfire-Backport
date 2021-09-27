package connor135246.campfirebackport.client.models;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

/**
 * campfire fire - connor135246 Created using Tabula 4.1.1
 */
public class ModelFire extends CampfireModelBase
{
    public ModelRenderer fire1;
    public ModelRenderer fire2;

    public ModelFire()
    {
        this.textureWidth = 20;
        this.textureHeight = 16;
        this.fire1 = new ModelRenderer(this, 0, -4);
        this.fire1.setRotationPoint(-7.0F, 7.0F, -7.0F);
        this.fire1.addBox(0.0F, 0.0F, 0.0F, 0, 16, 20, 0.0F);
        this.setRotateAngle(fire1, 0.0F, 0.7853981633974483F, 0.0F);
        this.fire2 = new ModelRenderer(this, 0, -4);
        this.fire2.setRotationPoint(7.0F, 7.0F, -7.0F);
        this.fire2.addBox(0.0F, 0.0F, 0.0F, 0, 16, 20, 0.0F);
        this.setRotateAngle(fire2, 0.0F, -0.7853981633974483F, 0.0F);
    }

    @Override
    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5)
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
