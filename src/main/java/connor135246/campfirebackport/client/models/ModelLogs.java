package connor135246.campfirebackport.client.models;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

/**
 * campfire logs - connor135246 Created using Tabula 4.1.1
 */
public class ModelLogs extends CampfireModelBase
{
    public ModelRenderer topbacklog;
    public ModelRenderer topfrontlog;
    public ModelRenderer bottomrightlog;
    public ModelRenderer bottomleftlog;
    public ModelRenderer firepit;

    public ModelLogs()
    {
        this.textureWidth = 40;
        this.textureHeight = 33;
        this.firepit = new ModelRenderer(this, -16, 16);
        this.firepit.setRotationPoint(-3.0F, 23.0F, -8.0F);
        this.firepit.addBox(0.0F, 0.0F, 0.0F, 6, 1, 16, 0.0F);
        this.bottomrightlog = new ModelRenderer(this, 0, 0);
        this.bottomrightlog.mirror = true;
        this.bottomrightlog.setRotationPoint(-3.0F, 20.0F, -8.0F);
        this.bottomrightlog.addBox(0.0F, 0.0F, 0.0F, 16, 4, 4, 0.0F);
        this.setRotateAngle(bottomrightlog, 0.0F, -1.5707963267948966F, 0.0F);
        this.bottomleftlog = new ModelRenderer(this, 0, 0);
        this.bottomleftlog.setRotationPoint(3.0F, 20.0F, 8.0F);
        this.bottomleftlog.addBox(0.0F, 0.0F, 0.0F, 16, 4, 4, 0.0F);
        this.setRotateAngle(bottomleftlog, 0.0F, 1.5707963267948966F, 0.0F);
        this.topbacklog = new ModelRenderer(this, 0, 8);
        this.topbacklog.setRotationPoint(-8.0F, 17.0F, 3.0F);
        this.topbacklog.addBox(0.0F, 0.0F, 0.0F, 16, 4, 4, 0.0F);
        this.topfrontlog = new ModelRenderer(this, 0, 8);
        this.topfrontlog.mirror = true;
        this.topfrontlog.setRotationPoint(-8.0F, 17.0F, -7.0F);
        this.topfrontlog.addBox(0.0F, 0.0F, 0.0F, 16, 4, 4, 0.0F);
    }

    @Override
    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5)
    {
        this.firepit.render(f5);
        this.bottomrightlog.render(f5);
        this.bottomleftlog.render(f5);
        this.topbacklog.render(f5);
        this.topfrontlog.render(f5);
    }

}
