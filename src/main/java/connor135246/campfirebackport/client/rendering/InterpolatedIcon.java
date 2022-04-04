package connor135246.campfirebackport.client.rendering;

import java.lang.reflect.Field;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.data.AnimationMetadataSection;

/**
 * thanks, et futurum <br>
 * https://github.com/ganymedes01/Et-Futurum/blob/master/src/main/java/ganymedes01/etfuturum/client/PrismarineIcon.java <br>
 * https://github.com/ganymedes01/Et-Futurum/blob/master/src/main/java/ganymedes01/etfuturum/blocks/PrismarineBlocks.java <br>
 * https://github.com/ganymedes01/Et-Futurum/blob/master/src/main/java/ganymedes01/etfuturum/core/handlers/ClientEventHandler.java
 */
public class InterpolatedIcon extends TextureAtlasSprite
{

    protected int[][] interpolatedFrameData;
    private Field fanimationMetadata;

    public InterpolatedIcon(String name)
    {
        super(name);
        fanimationMetadata = ReflectionHelper.findField(TextureAtlasSprite.class, "animationMetadata", "field_110982_k");
        fanimationMetadata.setAccessible(true);
    }

    @Override
    public void updateAnimation()
    {
        super.updateAnimation();

        try
        {
            updateAnimationInterpolated();
        }
        catch (Exception e)
        {
            ;
        }
    }

    private void updateAnimationInterpolated() throws IllegalArgumentException, IllegalAccessException
    {
        AnimationMetadataSection animationMetadata = (AnimationMetadataSection) fanimationMetadata.get(this);

        double d0 = 1.0D - tickCounter / (double) animationMetadata.getFrameTimeSingle(frameCounter);
        int i = animationMetadata.getFrameIndex(frameCounter);
        int j = animationMetadata.getFrameCount() == 0 ? framesTextureData.size() : animationMetadata.getFrameCount();
        int k = animationMetadata.getFrameIndex((frameCounter + 1) % j);

        if (i != k && k >= 0 && k < framesTextureData.size())
        {
            int[][] aint = (int[][]) framesTextureData.get(i);
            int[][] aint1 = (int[][]) framesTextureData.get(k);

            if (interpolatedFrameData == null || interpolatedFrameData.length != aint.length)
                interpolatedFrameData = new int[aint.length][];

            for (int l = 0; l < aint.length; l++)
            {
                if (interpolatedFrameData[l] == null)
                    interpolatedFrameData[l] = new int[aint[l].length];

                if (l < aint1.length && aint1[l].length == aint[l].length)
                    for (int i1 = 0; i1 < aint[l].length; ++i1)
                    {
                        int j1 = aint[l][i1];
                        int k1 = aint1[l][i1];
                        int l1 = (int) (((j1 & 16711680) >> 16) * d0 + ((k1 & 16711680) >> 16) * (1.0D - d0));
                        int i2 = (int) (((j1 & 65280) >> 8) * d0 + ((k1 & 65280) >> 8) * (1.0D - d0));
                        int j2 = (int) ((j1 & 255) * d0 + (k1 & 255) * (1.0D - d0));
                        interpolatedFrameData[l][i1] = j1 & -16777216 | l1 << 16 | i2 << 8 | j2;
                    }
            }

            TextureUtil.uploadTextureMipmap(interpolatedFrameData, width, height, originX, originY, false, false);
        }
    }
}
