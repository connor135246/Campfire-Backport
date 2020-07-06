package connor135246.campfirebackport.client.compat;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import connor135246.campfirebackport.client.rendering.RenderCampfire;
import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.crafting.CampfireStateChanger;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

public class NEICampfireStateChangerHandler extends NEIGenericCustomInputHandler
{
    // thanks to immersive engineering for uhh... having a github. :)

    public class CachedCampfireStateChanger extends CachedGenericCustomInput
    {
        public List<PositionedStack> input = new ArrayList<PositionedStack>(1);
        public PositionedStack output;
        public PositionedStack dispenser;
        public boolean dispensable;
        public boolean hasResult;
        public boolean leftClick;
        public boolean extinguisher;

        public Rectangle dispenserRect;
        public Rectangle inputRect;

        public CachedCampfireStateChanger(CampfireStateChanger cstate)
        {
            super(cstate);

            extinguisher = cstate.isExtinguisher();
            leftClick = cstate.isLeftClick();
            hasResult = cstate.hasReturnStack();
            dispensable = cstate.isDispensable();

            input.add(new PositionedStack(neiList, 74, 17, false));
            inputRect = new Rectangle(input.get(0).relx - 1, input.get(0).rely - 1, 20, 20);

            if (hasResult)
                output = new PositionedStack(cstate.getReturnStack(), 74, 41, false);

            if (dispensable)
            {
                dispenser = new PositionedStack(new ItemStack(Blocks.dispenser), 94, 41);
                dispenserRect = new Rectangle(dispenser.relx - 1, dispenser.rely - 1, 20, 20);
            }
        }

        @Override
        public List<PositionedStack> getIngredients()
        {
            return getCycledIngredients(cycleticks / 20, input);
        }

        @Override
        public PositionedStack getResult()
        {
            return output;
        }

        @Override
        public PositionedStack getOtherStack()
        {
            return dispenser;
        }
    }

    @Override
    public String getRecipeName()
    {
        return StatCollector.translateToLocal(Reference.MODID + ".nei.state_changer_recipe");
    }

    @Override
    public void loadTransferRects()
    {
        transferRects.add(new RecipeTransferRect(new Rectangle(13, 4, 40, 36), getRecipeID()));
        transferRects.add(new RecipeTransferRect(new Rectangle(111, 4, 40, 36), getRecipeID()));
        transferRects.add(new RecipeTransferRect(new Rectangle(70, 0, 24, 15), getRecipeID()));
    }

    public String getRecipeID()
    {
        return Reference.MODID + ".campfireStateChanger";
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results)
    {
        if (outputId.equals(getRecipeID()))
        {
            for (CampfireStateChanger cstate : CampfireStateChanger.getMasterList())
                arecipes.add(new CachedCampfireStateChanger(cstate));
        }
        else
            super.loadCraftingRecipes(outputId, results);
    }

    @Override
    public void loadCraftingRecipes(ItemStack result)
    {
        for (CampfireStateChanger cstate : CampfireStateChanger.getMasterList())
        {
            if (cstate.hasReturnStack())
                if (NEIServerUtils.areStacksSameTypeCrafting(cstate.getReturnStack(), result))
                    arecipes.add(new CachedCampfireStateChanger(cstate));
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient)
    {
        if (ingredient.getItem() instanceof ItemBlock)
        {
            Block block = Block.getBlockFromItem(ingredient.getItem());
            if (block instanceof BlockCampfire)
            {
                for (CampfireStateChanger cstate : CampfireStateChanger.getMasterList())
                    arecipes.add(new CachedCampfireStateChanger(cstate));
                return;
            }
        }

        for (CampfireStateChanger cstate : CampfireStateChanger.getMasterList())
        {
            CachedCampfireStateChanger cachedCstate = new CachedCampfireStateChanger(cstate);
            if (cachedCstate != null && !cachedCstate.types.isEmpty() && cachedCstate.inputType != 4 && cachedCstate.contains(cachedCstate.input, ingredient))
                arecipes.add(new CachedCampfireStateChanger(cstate));
        }
    }

    @Override
    public void drawBackground(int recipe)
    {
        GL11.glPushMatrix();
        GL11.glColor4f(1, 1, 1, 1);

        CachedCampfireStateChanger cachedCstate = (CachedCampfireStateChanger) this.arecipes.get(recipe % arecipes.size());

        if (cachedCstate != null && !cachedCstate.types.isEmpty())
        {
            GL11.glTranslatef(0, 0, 50);

            GuiDraw.changeTexture(Reference.MODID + ":" + "textures/gui/neiElements.png");
            GuiDraw.drawTexturedModalRect(56, 0, cachedCstate.leftClick ? 0 : 60, cachedCstate.output != null ? 59 : 2, 52, 41);

            GL11.glTranslatef(0, 0, 4);

            if (cachedCstate.dispensable)
                drawSlot(cachedCstate.dispenser.relx, cachedCstate.dispenser.rely, 16, 16);

            TileEntityCampfire litcamp = new TileEntityCampfire();
            litcamp.setThisLit(true);
            litcamp.setThisType(cachedCstate.getType());
            litcamp.setAnimTimer(cycleticks);

            TileEntityCampfire unlitcamp = new TileEntityCampfire();
            unlitcamp.setThisLit(false);
            unlitcamp.setThisType(EnumCampfireType.REGULAR);

            GL11.glTranslatef(12, 32, 46);
            GL11.glRotatef(-30, 1, 0, 0);
            GL11.glRotatef(45, 0, 1, 0);
            GL11.glScalef(30, -30, 30);

            ((RenderCampfire) TileEntityRendererDispatcher.instance.getSpecialRenderer(litcamp)).renderSimple(cachedCstate.extinguisher ? litcamp : unlitcamp);

            GL11.glPopMatrix();
            GL11.glPushMatrix();
            GL11.glColor4f(1, 1, 1, 1);
            
            ItemStack stack = new ItemStack(Blocks.grass);

            GuiDraw.changeTexture(TextureMap.locationBlocksTexture);

            // this is a real mess...
            GL11.glTranslatef(9, 21, -20);
            GL11.glScalef(3, 3, 3);
            RenderItem.getInstance().renderItemIntoGUI(Minecraft.getMinecraft().fontRenderer, Minecraft.getMinecraft().renderEngine, stack, 0, 0);

            GL11.glPopMatrix();
            GL11.glPushMatrix();

            GL11.glTranslatef(0, 0, 100);
            GL11.glRotatef(30, 1, 0, 0);
            GL11.glRotatef(45, 0, 1, 0);
            GL11.glTranslatef(8, 33, 0);
            GuiDraw.drawGradientRect(0, 0, 38, 15, 0x00c6c6c6, 0xffc6c6c6);
            GuiDraw.drawRect(0, 15, 38, 13, 0xffc6c6c6);

            GL11.glPopMatrix();
            GL11.glPushMatrix();

            GL11.glTranslatef(0, 0, 100);
            GL11.glRotatef(30, 1, 0, 0);
            GL11.glRotatef(-45, 0, 1, 0);
            GL11.glTranslatef(46, 71, 0);
            GuiDraw.drawGradientRect(0, 0, 38, 15, 0x00c6c6c6, 0xffc6c6c6);
            GuiDraw.drawRect(0, 15, 38, 13, 0xffc6c6c6);

            GL11.glPopMatrix();
            GL11.glPushMatrix();
            
            GL11.glColor4f(1, 1, 1, 1);
            GL11.glTranslatef(106.5F, 21F, -20);
            GL11.glScalef(3, 3, 3);
            RenderItem.getInstance().renderItemIntoGUI(Minecraft.getMinecraft().fontRenderer, Minecraft.getMinecraft().renderEngine, stack, 0, 0);

            GL11.glPopMatrix();
            GL11.glPushMatrix();

            GL11.glTranslatef(0, 0, 148);
            GL11.glRotatef(30, 1, 0, 0);
            GL11.glRotatef(45, 0, 1, 0);
            GL11.glTranslatef(154, -25, 0);
            GuiDraw.drawGradientRect(0, 0, 30, 15, 0x00c6c6c6, 0xffc6c6c6);
            GL11.glTranslatef(6, -5, -6);
            GuiDraw.drawRect(0, 15, 30, 14, 0xffc6c6c6);

            GL11.glPopMatrix();
            GL11.glPushMatrix();

            GL11.glTranslatef(0, 0, 100);
            GL11.glRotatef(30, 1, 0, 0);
            GL11.glRotatef(-45, 0, 1, 0);
            GL11.glTranslatef(184, 126, 0);
            GuiDraw.drawGradientRect(0, 0, 38, 15, 0x00c6c6c6, 0xffc6c6c6);
            GuiDraw.drawRect(0, 15, 38, 14, 0xffc6c6c6);
            //
            
            GL11.glPopMatrix();
            GL11.glPushMatrix();
            GL11.glColor4f(1, 1, 1, 1);
            
            GL11.glTranslatef(130.5F, 21.5F, 100);
            GL11.glRotatef(-30, 1, 0, 0);
            GL11.glRotatef(-45, 0, 1, 0);
            GL11.glScalef(30, -30, 30);

            ((RenderCampfire) TileEntityRendererDispatcher.instance.getSpecialRenderer(litcamp)).renderSimple(cachedCstate.extinguisher ? unlitcamp : litcamp);
            
            GL11.glPopMatrix();
            GL11.glPushMatrix();
        }

        GL11.glPopMatrix();
    }

    @Override
    public List<String> handleItemTooltipFromMousePosition(Point relMouse, CachedGenericCustomInput cachedGinput, ItemStack stack, List<String> tooltip)
    {
        CachedCampfireStateChanger cachedCstate = (CachedCampfireStateChanger) cachedGinput;
        if (!tooltip.isEmpty() && stack != null && cachedCstate.dispensable && stack.getItem() == Item.getItemFromBlock(Blocks.dispenser)
                && cachedCstate.dispenserRect.contains(relMouse))
        {
            tooltip.set(0, EnumChatFormatting.BOLD + "" + EnumChatFormatting.ITALIC + StatCollector.translateToLocal(Reference.MODID + ".nei.dispensable"));
        }
        else
            super.handleItemTooltipFromMousePosition(relMouse, cachedGinput, stack, tooltip);

        return tooltip;
    }

    @Override
    public boolean hoveringOverInput(Point relMouse, CachedGenericCustomInput cachedGinput)
    {
        return (((CachedCampfireStateChanger) cachedGinput).inputRect.contains(relMouse));
    }

}
