package connor135246.campfirebackport.client.compat.nei;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;
import connor135246.campfirebackport.common.items.ItemBlockCampfire;
import connor135246.campfirebackport.common.recipes.CampfireRecipe;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import connor135246.campfirebackport.util.StringParsers;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

public class NEICampfireRecipeHandler extends NEIGenericRecipeHandler
{
    // thanks to immersive engineering for uhh... having a github. :)

    public static final String outputID = Reference.NEI_RECIPE_ID;

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
                List<ItemStack> expandedInputList = expandInputList(crecipe.getInputs()[i]);
                if (expandedInputList.isEmpty()) // if the recipe has no inputs, it's invalid!
                {
                    types = EnumCampfireType.NEITHER;
                    return;
                }
                inputs.add(new PositionedStack(expandedInputList, getInputX(i), getInputY(i), false));
                inputRects[i] = new Rectangle(inputs.get(i).relx - 1, inputs.get(i).rely - 1, 18, 18);
            }

            cookingTime = crecipe.getCookingTime();
            signalFire = crecipe.getSignalFire();
            output = new PositionedStack(crecipe.getOutput(), 120, 23, false);

            if (crecipe.hasByproduct())
            {
                byproductChance = crecipe.getByproductChance();
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
            if (matchesCrafting(crecipe, result) || (crecipe.hasByproduct() && NEIServerUtils.areStacksSameTypeCrafting(crecipe.getByproduct(), result)))
                loadValidRecipe(crecipe);
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
            if (matchesUsage(crecipe, ingredient))
                loadValidRecipe(crecipe);
    }

    public void loadValidRecipe(CampfireRecipe crecipe)
    {
        CachedCampfireRecipe cachedCrecipe = new CachedCampfireRecipe(crecipe);
        if (cachedCrecipe.types != null && cachedCrecipe.types != EnumCampfireType.NEITHER)
            arecipes.add(cachedCrecipe);
    }

    @Override
    public void loadAllRecipes()
    {
        for (CampfireRecipe crecipe : CampfireRecipe.getMasterList())
            loadValidRecipe(crecipe);
    }

    @Override
    public boolean mouseClicked(GuiRecipe gui, int button, int recipe)
    {
        boolean result = false;
        CachedCampfireRecipe cachedCrecipe = (CachedCampfireRecipe) this.arecipes.get(recipe % arecipes.size());

        if (cachedCrecipe.signalFire != 0 && signalRect.contains(getRelMouse(gui, recipe)))
        {
            if (button == 0)
                result = GuiCraftingRecipe.openRecipeGui(NEISignalFireBlocksHandler.recipeID);
            else if (button == 1)
                result = GuiUsageRecipe.openRecipeGui(NEISignalFireBlocksHandler.recipeID);
        }

        return result || super.mouseClicked(gui, button, recipe);
    }

    @Override
    public boolean handleMiscTooltipFromMousePosition(Point relMouse, CachedGenericRecipe cachedGrecipe, ItemStack stack, List<String> tooltip)
    {
        CachedCampfireRecipe cachedCrecipe = (CachedCampfireRecipe) cachedGrecipe;
        if (cachedCrecipe.signalFire != 0 && signalRect.contains(relMouse))
        {
            tooltip.add(EnumChatFormatting.GOLD + "" + EnumChatFormatting.ITALIC
                    + StringParsers.translateNEI(cachedCrecipe.signalFire == 1 ? "onlysignal" : "notsignal"));
            tooltip.add(StringParsers.translateNEI("see_signal_fire_blocks"));
        }
        else if (!tooltip.isEmpty() && cachedCrecipe.byproduct != null && byproductRect.contains(relMouse) && cachedCrecipe.byproductChance < 1.0D)
        {
            tooltip.add("");
            tooltip.add(EnumChatFormatting.GOLD + "" + EnumChatFormatting.ITALIC + StringParsers.translateNEI("chance") + " "
                    + Math.min(Math.round(cachedCrecipe.byproductChance * 1000), 999) / 10.0D + "%");
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

        if (cachedCrecipe != null && cachedCrecipe.types != EnumCampfireType.NEITHER)
        {
            GuiDraw.changeTexture(neiBackground);
            GuiDraw.drawTexturedModalRect(54, 7, 118, 2, 63, 41);

            for (int i = 0; i < 4; ++i)
                drawSlot(getInputX(i), getInputY(i));

            drawSlot(cachedCrecipe.output.relx, cachedCrecipe.output.rely);
            if (cachedCrecipe.byproduct != null)
                drawSlot(cachedCrecipe.byproduct.relx, cachedCrecipe.byproduct.rely);

            GL11.glTranslatef(0, 0, 300);

            String timeString;
            if (NEIClientUtils.shiftKey())
                timeString = StringParsers.translateTime("ticks", cachedCrecipe.cookingTime + "");
            else
                timeString = StringParsers.translateTimeHumanReadable(cachedCrecipe.cookingTime);
            fonty().drawString(timeString, 82 - fonty().getStringWidth(timeString) / 2, 1, 0x777777);

            GL11.glPopMatrix();
            GL11.glPushMatrix();
            GL11.glColor4f(1, 1, 1, 1);

            GuiDraw.changeTexture(TextureMap.locationBlocksTexture);

            // this is a real mess...
            GL11.glTranslatef(61, 64, 0);

            renderBlock(cachedCrecipe.signalFire == 0 ? Blocks.grass : (cachedCrecipe.signalFire == 1 ? Blocks.hay_block : Blocks.stone));

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

            renderCampfire(cachedCrecipe.types, true, 2);
        }

        GL11.glPopMatrix();
    }

}
