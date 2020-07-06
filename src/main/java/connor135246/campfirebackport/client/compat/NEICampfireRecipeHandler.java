package connor135246.campfirebackport.client.compat;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import connor135246.campfirebackport.client.compat.NEICampfireStateChangerHandler.CachedCampfireStateChanger;
import connor135246.campfirebackport.client.compat.NEIGenericCustomInputHandler.CachedGenericCustomInput;
import connor135246.campfirebackport.client.rendering.RenderCampfire;
import connor135246.campfirebackport.common.crafting.CampfireRecipe;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.util.Reference;
import net.minecraft.block.Block;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockHay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.ColorizerGrass;

public class NEICampfireRecipeHandler extends NEIGenericCustomInputHandler
{
    // thanks to immersive engineering for uhh... having a github. :)

    public class CachedCampfireRecipe extends CachedGenericCustomInput
    {
        public List<PositionedStack> inputs = new ArrayList<PositionedStack>(4);
        public PositionedStack output;
        public int cooking;
        public Boolean signal;

        public Rectangle inputRect;
        public Rectangle signalRect;

        public CachedCampfireRecipe(CampfireRecipe crecipe)
        {
            super(crecipe);

            for (int i = 0; i < crecipe.getInputSize(); ++i)
                inputs.add(new PositionedStack(neiList, getInputX(i), getInputY(i), false));

            inputRect = new Rectangle(inputs.get(0).relx - 1, inputs.get(0).rely - 1, 40, 40);

            output = new PositionedStack(crecipe.getOutput(), 120, 23, false);

            cooking = crecipe.getCookingTime();
            if (crecipe.doesSignalMatter())
            {
                signal = crecipe.requiresSignalFire();
                signalRect = new Rectangle(60, 40, 44, 20);
            }
        }

        @Override
        public List<PositionedStack> getIngredients()
        {
            return getCycledIngredients(cycleticks / 20, inputs);
        }

        @Override
        public PositionedStack getResult()
        {
            return output;
        }
    }

    public int getInputX(int i)
    {
        return 15 + (i % 2) * 18;
    }

    public int getInputY(int i)
    {
        return 14 + (i > 1 ? 18 : 0);
    }

    @Override
    public String getRecipeName()
    {
        return StatCollector.translateToLocal(Reference.MODID + ".nei.cooking_recipe");
    }

    @Override
    public void loadTransferRects()
    {
        transferRects.add(new RecipeTransferRect(new Rectangle(62, 15, 40, 24), getRecipeID()));
    }

    public String getRecipeID()
    {
        return Reference.MODID + ".recipeCampfire";
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results)
    {
        if (outputId.equals(getRecipeID()))
        {
            for (CampfireRecipe crecipe : CampfireRecipe.getMasterList())
                arecipes.add(new CachedCampfireRecipe(crecipe));
        }
        else
            super.loadCraftingRecipes(outputId, results);
    }

    @Override
    public void loadCraftingRecipes(ItemStack result)
    {
        for (CampfireRecipe crecipe : CampfireRecipe.getMasterList())
        {
            if (NEIServerUtils.areStacksSameTypeCrafting(crecipe.getOutput(), result))
                arecipes.add(new CachedCampfireRecipe(crecipe));
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient)
    {
        for (CampfireRecipe crecipe : CampfireRecipe.getMasterList())
        {
            CachedCampfireRecipe cachedCrecipe = new CachedCampfireRecipe(crecipe);
            if (cachedCrecipe != null && !cachedCrecipe.types.isEmpty() && cachedCrecipe.inputType != 4
                    && cachedCrecipe.contains(cachedCrecipe.inputs, ingredient))
                arecipes.add(new CachedCampfireRecipe(crecipe));
        }
    }

    @Override
    public void drawBackground(int recipe)
    {
        GL11.glPushMatrix();
        GL11.glColor4f(1, 1, 1, 1);

        CachedCampfireRecipe cachedCrecipe = (CachedCampfireRecipe) this.arecipes.get(recipe % arecipes.size());

        if (cachedCrecipe != null && !cachedCrecipe.types.isEmpty())
        {
            GuiDraw.changeTexture(Reference.MODID + ":" + "textures/gui/neiElements.png");
            GuiDraw.drawTexturedModalRect(54, 7, 118, 2, 63, 41);

            for (int i = 0; i < 4; ++i)
                drawSlot(getInputX(i), getInputY(i), 16, 16);

            drawSlot(cachedCrecipe.output.relx, cachedCrecipe.output.rely, 16, 16);

            FontRenderer fonty = Minecraft.getMinecraft().fontRenderer;

            GL11.glTranslatef(0, 0, 300);

            String ticks = cachedCrecipe.cooking + " Ticks";
            fonty.drawString(ticks, 82 - fonty.getStringWidth(ticks) / 2, 1, 0x777777);

            GL11.glPopMatrix();
            GL11.glPushMatrix();
            GL11.glColor4f(1, 1, 1, 1);

            ItemStack stack;

            if (cachedCrecipe.signal != null)
                stack = new ItemStack(cachedCrecipe.signal ? Blocks.hay_block : Blocks.stone);
            else
                stack = new ItemStack(Blocks.grass);

            GuiDraw.changeTexture(TextureMap.locationBlocksTexture);

            // this is a real mess...
            GL11.glTranslatef(58, 27, -20);
            GL11.glScalef(3, 3, 3);
            RenderItem.getInstance().renderItemIntoGUI(Minecraft.getMinecraft().fontRenderer, Minecraft.getMinecraft().renderEngine, stack, 0, 0);

            GL11.glPopMatrix();
            GL11.glPushMatrix();

            GL11.glTranslatef(0, 0, 100);
            GL11.glRotatef(30, 1, 0, 0);
            GL11.glRotatef(45, 0, 1, 0);
            GL11.glTranslatef(79, 11, 0);
            GuiDraw.drawGradientRect(0, 0, 38, 15, 0x00c6c6c6, 0xffc6c6c6);
            GuiDraw.drawRect(0, 15, 38, 13, 0xffc6c6c6);

            GL11.glPopMatrix();
            GL11.glPushMatrix();

            GL11.glTranslatef(0, 0, 100);
            GL11.glRotatef(30, 1, 0, 0);
            GL11.glRotatef(-45, 0, 1, 0);
            GL11.glTranslatef(117, 106, 0);
            GuiDraw.drawGradientRect(0, 0, 38, 15, 0x00c6c6c6, 0xffc6c6c6);
            GuiDraw.drawRect(0, 15, 38, 13, 0xffc6c6c6);
            //

            GL11.glPopMatrix();
            GL11.glPushMatrix();
            GL11.glColor4f(1, 1, 1, 1);

            GL11.glTranslatef(61, 38, 100);
            GL11.glRotatef(-30, 1, 0, 0);
            GL11.glRotatef(45, 0, 1, 0);
            GL11.glScalef(30, -30, 30);

            TileEntityCampfire tilecamp = new TileEntityCampfire();
            tilecamp.setThisLit(true);
            tilecamp.setThisType(cachedCrecipe.getType());
            tilecamp.setAnimTimer(cycleticks);

            ((RenderCampfire) TileEntityRendererDispatcher.instance.getSpecialRenderer(tilecamp)).renderSimple(tilecamp);
        }

        GL11.glPopMatrix();
    }

    @Override
    public List<String> handleItemTooltipFromMousePosition(Point relMouse, CachedGenericCustomInput cachedGinput, ItemStack stack, List<String> tooltip)
    {
        CachedCampfireRecipe cachedCrecipe = (CachedCampfireRecipe) cachedGinput;
        if (cachedCrecipe.signal != null && cachedCrecipe.signalRect != null && cachedCrecipe.signalRect.contains(relMouse))
        {
            tooltip.add(EnumChatFormatting.BOLD + "" + EnumChatFormatting.ITALIC
                    + StatCollector.translateToLocal(Reference.MODID + ".nei." + (cachedCrecipe.signal ? "onlysignal" : "notsignal")));
        }
        else
            super.handleItemTooltipFromMousePosition(relMouse, cachedGinput, stack, tooltip);

        return tooltip;
    }

    @Override
    public boolean hoveringOverInput(Point relMouse, CachedGenericCustomInput cachedGinput)
    {
        return (((CachedCampfireRecipe) cachedGinput).inputRect.contains(relMouse));
    }

}
