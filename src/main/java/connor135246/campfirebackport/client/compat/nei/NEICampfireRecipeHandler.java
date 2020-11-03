package connor135246.campfirebackport.client.compat.nei;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import connor135246.campfirebackport.client.rendering.RenderCampfire;
import connor135246.campfirebackport.common.items.ItemBlockCampfire;
import connor135246.campfirebackport.common.recipes.CampfireRecipe;
import connor135246.campfirebackport.util.Reference;
import connor135246.campfirebackport.util.StringParsers;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

public class NEICampfireRecipeHandler extends NEIGenericRecipeHandler
{
    // thanks to immersive engineering for uhh... having a github. :)

    public static final String outputID = Reference.MODID + ".campfireRecipe";

    /** rectangle for the {@link CampfireRecipe#signalFire} tooltip */
    public static final Rectangle signalRect = new Rectangle(60, 40, 44, 20);
    /** rectangle for the {@link CampfireRecipe#byproduct} tooltip */
    public static final Rectangle byproductRect = new Rectangle(137, 22, 20, 20);

    /** recipe transfer rects */
    public static final RecipeTransferRect transfer1 = new RecipeTransferRect(new Rectangle(62, 15, 40, 24), outputID);

    public class CachedCampfireRecipe extends CachedGenericRecipe
    {
        /** {@link CampfireRecipe#byproduct} */
        public PositionedStack byproduct;
        /** {@link CampfireRecipe#byproductChance} */
        public double byproductChance;
        /** {@link CampfireRecipe#cookingTime} */
        public int cookingTime;
        /** {@link CampfireRecipe#signalFire} */
        public byte signalFire;

        public CachedCampfireRecipe(CampfireRecipe crecipe)
        {
            super(crecipe);

            for (int i = 0; i < numInputs; ++i)
            {
                inputs.add(new PositionedStack(neiLists.get(i), getInputX(i), getInputY(i), genPerms[i]));
                inputRects[i] = new Rectangle(inputs.get(i).relx - 1, inputs.get(i).rely - 1, 18, 18);
            }

            cookingTime = crecipe.getCookingTime();
            signalFire = crecipe.getSignalFire();
            output = new PositionedStack(crecipe.getOutput(), 120, 23, false);

            if (crecipe.hasByproduct())
            {
                byproductChance = crecipe.getByproductChance() * 100;
                if (byproductChance > 0)
                    byproduct = new PositionedStack(crecipe.getByproduct(), 138, 23, false);
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

        @Override
        public PositionedStack getOtherStack()
        {
            return byproduct;
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
        return StringParsers.translateNEI("cooking_recipe");
    }

    @Override
    public void loadTransferRects()
    {
        transferRects.add(transfer1);
    }

    @Override
    public String getOutputID()
    {
        return outputID;
    }

    @Override
    public void loadCraftingRecipes(ItemStack result)
    {
        for (CampfireRecipe crecipe : CampfireRecipe.getMasterList())
        {
            if (NEIServerUtils.areStacksSameTypeCrafting(crecipe.getOutput(), result))
                arecipes.add(new CachedCampfireRecipe(crecipe));
            else if (crecipe.hasByproduct() && NEIServerUtils.areStacksSameTypeCrafting(crecipe.getByproduct(), result))
                arecipes.add(new CachedCampfireRecipe(crecipe));
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient)
    {
        if (ingredient.getItem() instanceof ItemBlockCampfire)
        {
            loadAllRecipes();
            return;
        }

        for (CampfireRecipe crecipe : CampfireRecipe.getMasterList())
        {
            CachedCampfireRecipe cachedCrecipe = new CachedCampfireRecipe(crecipe);

            if (cachedCrecipe != null && cachedCrecipe.types.length != 0)
            {
                for (int i = 0; i < cachedCrecipe.numInputs; ++i)
                {
                    if (cachedCrecipe.inputTypes[i] != 5 && cachedCrecipe.inputs.get(i).contains(ingredient))
                    {
                        arecipes.add(cachedCrecipe);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void loadAllRecipes()
    {
        for (CampfireRecipe crecipe : CampfireRecipe.getMasterList())
            arecipes.add(new CachedCampfireRecipe(crecipe));
    }

    @Override
    public boolean handleMiscTooltipFromMousePosition(Point relMouse, CachedGenericRecipe cachedGrecipe, ItemStack stack, List<String> tooltip)
    {
        CachedCampfireRecipe cachedCrecipe = (CachedCampfireRecipe) cachedGrecipe;
        if (cachedCrecipe.signalFire != 0 && signalRect.contains(relMouse))
        {
            tooltip.add(EnumChatFormatting.GOLD + "" + EnumChatFormatting.ITALIC
                    + StringParsers.translateNEI(cachedCrecipe.signalFire == 1 ? "onlysignal" : "notsignal"));
        }
        else if (!tooltip.isEmpty() && cachedCrecipe.byproduct != null && byproductRect.contains(relMouse) && cachedCrecipe.byproductChance < 100)
        {
            tooltip.add("");
            tooltip.add(EnumChatFormatting.GOLD + "" + EnumChatFormatting.ITALIC + cachedCrecipe.byproductChance + "%");
        }
        else
            return true;

        return false;
    }

    @Override
    public void drawBackground(int recipe)
    {
        GL11.glPushMatrix();
        GL11.glColor4f(1, 1, 1, 1);

        CachedCampfireRecipe cachedCrecipe = (CachedCampfireRecipe) this.arecipes.get(recipe % arecipes.size());

        if (cachedCrecipe != null && cachedCrecipe.types.length != 0)
        {
            GuiDraw.changeTexture(neiBackground);
            GuiDraw.drawTexturedModalRect(54, 7, 118, 2, 63, 41);

            for (int i = 0; i < 4; ++i)
                drawSlot(getInputX(i), getInputY(i));

            drawSlot(cachedCrecipe.output.relx, cachedCrecipe.output.rely);
            if (cachedCrecipe.byproduct != null)
                drawSlot(cachedCrecipe.byproduct.relx, cachedCrecipe.byproduct.rely);

            GL11.glTranslatef(0, 0, 300);

            String ticks = StringParsers.translateNEI("num_ticks", cachedCrecipe.cookingTime);
            fonty.drawString(ticks, 82 - fonty.getStringWidth(ticks) / 2, 1, 0x777777);

            GL11.glPopMatrix();
            GL11.glPushMatrix();
            GL11.glColor4f(1, 1, 1, 1);

            GuiDraw.changeTexture(TextureMap.locationBlocksTexture);

            // this is a real mess...
            GL11.glTranslatef(58, 27, -20);
            GL11.glScalef(3, 3, 3);
            RenderItem.getInstance().renderItemIntoGUI(fonty, rendy,
                    cachedCrecipe.signalFire == 0 ? grassStack : (cachedCrecipe.signalFire == 1 ? hayStack : stoneStack), 0, 0);

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

            RenderCampfire.INSTANCE.renderSimple(true, cachedCrecipe.getType(), cycleticks);
        }

        GL11.glPopMatrix();
    }

}
