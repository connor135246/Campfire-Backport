package connor135246.campfirebackport.client.rendering;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;

import org.apache.commons.io.IOUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.SimpleResource;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;

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

    private boolean shouldInterpolate = false;
    private Field fmcmetaInputStream;

    public InterpolatedIcon(String name)
    {
        super(name);
        fanimationMetadata = ReflectionHelper.findField(TextureAtlasSprite.class, "animationMetadata", "field_110982_k");
        fanimationMetadata.setAccessible(true);
        fmcmetaInputStream = ReflectionHelper.findField(SimpleResource.class, "mcmetaInputStream", "field_110531_d");
        fmcmetaInputStream.setAccessible(true);
    }

    @Override
    public void updateAnimation()
    {
        super.updateAnimation();

        if (shouldInterpolate)
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

    /**
     * Since we have access to the IResourceManager here, we use this method to check if this icon actually has "interpolate": true in its metadata.
     */
    @Override
    public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location)
    {
        shouldInterpolate = false;

        try
        {
            IResource resource = manager.getResource(new ResourceLocation(location.getResourceDomain(), "textures/blocks/" + location.getResourcePath() + ".png"));
            if (resource.hasMetadata())
            {
                InputStream stream = (InputStream) fmcmetaInputStream.get(resource); // assumes IResource is a SimpleResource!
                BufferedReader bufferedreader = null;
                JsonObject mcmetaJson = null;

                try
                {
                    bufferedreader = new BufferedReader(new InputStreamReader(stream));
                    mcmetaJson = (new JsonParser()).parse(bufferedreader).getAsJsonObject();
                }
                finally
                {
                    IOUtils.closeQuietly(bufferedreader);
                }

                shouldInterpolate = JsonUtils.getJsonObjectBooleanFieldValue(mcmetaJson.getAsJsonObject("animation"), "interpolate");
            }
        }
        catch (IOException | JsonSyntaxException | IllegalArgumentException | IllegalAccessException excep)
        {
            ;
        }

        return super.hasCustomLoader(manager, location);
    }

}
