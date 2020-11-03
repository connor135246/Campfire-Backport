package connor135246.campfirebackport.client.compat.nei;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import connor135246.campfirebackport.client.rendering.RenderCampfire;
import connor135246.campfirebackport.common.CommonProxy;
import connor135246.campfirebackport.common.items.ItemBlockCampfire;
import connor135246.campfirebackport.common.recipes.BurnOutRule;
import connor135246.campfirebackport.common.recipes.CampfireStateChanger;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import connor135246.campfirebackport.util.StringParsers;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

public class NEICampfireStateChangerHandler extends NEIGenericRecipeHandler
{
    // thanks to immersive engineering for uhh... having a github. :)

    public static final String outputID = Reference.MODID + ".campfireStateChanger";

    /** dispenser stack for {@link CampfireStateChanger#dispensable} recipes */
    public static final PositionedStack dispenser = new PositionedStack(new ItemStack(Blocks.dispenser), 94, 41);
    /** rectangle for the {@link CampfireStateChanger#dispensable} tooltip */
    public static final Rectangle dispenserRect = new Rectangle(93, 40, 20, 20);

    /** recipe transfer rects */
    public static final RecipeTransferRect transfer1 = new RecipeTransferRect(new Rectangle(13, 10, 40, 36), outputID);
    public static final RecipeTransferRect transfer2 = new RecipeTransferRect(new Rectangle(111, 10, 40, 36), outputID);
    public static final RecipeTransferRect transfer3 = new RecipeTransferRect(new Rectangle(70, 0, 24, 15), outputID);

    public class CachedCampfireStateChanger extends CachedGenericRecipe
    {
        /** {@link CampfireStateChanger#dispensable} */
        public boolean dispensable;
        /** {@link CampfireStateChanger#hasOutputs()} */
        public boolean hasOutputs;
        /** {@link CampfireStateChanger#leftClick} */
        public boolean leftClick;
        /** {@link CampfireStateChanger#extinguisher} */
        public boolean extinguisher;

        /** string id for non-recipe state changers */
        public String specialID = null;

        public CachedCampfireStateChanger(CampfireStateChanger cstate)
        {
            super(cstate);

            inputs.add(new PositionedStack(neiLists.get(0), 74, 17, genPerms[0]));
            inputRects[0] = new Rectangle(inputs.get(0).relx - 1, inputs.get(0).rely - 1, 20, 20);

            extinguisher = cstate.isExtinguisher();
            leftClick = cstate.isLeftClick();
            hasOutputs = cstate.hasOutputs();
            dispensable = cstate.isDispensable();

            if (hasOutputs)
                output = new PositionedStack(cstate.getOutput(), 74, 41, false);
        }

        /**
         * constructor for non-recipe state changers
         */
        public CachedCampfireStateChanger(String specialID, EnumCampfireType type, boolean extinguisher, LinkedList<String> tooltip,
                List<ItemStack> inputStacks)
        {
            super(null);

            this.types = type.asArray();

            this.specialID = specialID;
            this.extinguisher = extinguisher;

            this.tooltips = new ArrayList<LinkedList<String>>(1);
            this.tooltips.add(tooltip);

            numInputs = 1;
            inputs = new ArrayList<PositionedStack>(1);
            inputs.add(new PositionedStack(inputStacks, 74, 17, false));
            inputTypes = new byte[] { -1 };
            dataTypes = new byte[] { -1 };
            inputRects = new Rectangle[] { new Rectangle(inputs.get(0).relx - 1, inputs.get(0).rely - 1, 20, 20) };
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
            return dispensable ? dispenser : null;
        }

    }

    @Override
    public String getRecipeName()
    {
        return StringParsers.translateNEI("state_changer_recipe");
    }

    @Override
    public void loadTransferRects()
    {
        transferRects.add(transfer1);
        transferRects.add(transfer2);
        transferRects.add(transfer3);
    }

    @Override
    public String getOutputID()
    {
        return outputID;
    }

    @Override
    public void loadCraftingRecipes(ItemStack result)
    {
        for (CampfireStateChanger cstate : CampfireStateChanger.getMasterList())
        {
            if (cstate.hasOutputs() && NEIServerUtils.areStacksSameTypeCrafting(cstate.getOutput(), result))
                arecipes.add(new CachedCampfireStateChanger(cstate));
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

        for (CampfireStateChanger cstate : CampfireStateChanger.getMasterList())
        {
            CachedCampfireStateChanger cachedCstate = new CachedCampfireStateChanger(cstate);
            if (cachedCstate != null && cachedCstate.types.length != 0 && cachedCstate.inputTypes[0] != 5 && cachedCstate.inputs.get(0).contains(ingredient))
                arecipes.add(cachedCstate);
        }
    }

    @Override
    public void loadAllRecipes()
    {
        for (CampfireStateChanger cstate : CampfireStateChanger.getMasterList())
            arecipes.add(new CachedCampfireStateChanger(cstate));

        // creating non-recipe state changers
        final String[] typeArray = EnumCampfireType.BOTH.asArray();
        final boolean[] extinguisherArray = new boolean[] { true, false };

        // wand
        if (CommonProxy.isThaumcraftLoaded)
        {
            Item wand = GameData.getItemRegistry().getObject("Thaumcraft:WandCasting");
            if (wand != null)
            {
                List<ItemStack> wandList = new ArrayList<ItemStack>();
                wand.getSubItems(wand, CreativeTabs.tabAllSearch, wandList);

                for (String type : typeArray)
                    for (boolean extinguisher : extinguisherArray)
                    {
                        double cost = CampfireBackportConfig.visCosts[EnumCampfireType.toInt(type) + (extinguisher ? 0 : 2)];

                        if (cost != 0.0)
                        {
                            LinkedList<String> tooltip = new LinkedList<String>();
                            tooltip.add("");
                            tooltip.add(EnumChatFormatting.GOLD + StringParsers.translateNEI("vis_cost")
                                    + (extinguisher ? EnumChatFormatting.DARK_AQUA + " " + cost + " Aqua" : EnumChatFormatting.RED + " " + cost + " Ignis"));

                            arecipes.add(new CachedCampfireStateChanger("wand", EnumCampfireType.FROM_NAME.get(type), extinguisher, tooltip, wandList));
                        }
                    }
            }
        }

        // burning out
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        for (String type : typeArray)
        {
            BurnOutRule brule = BurnOutRule.findBurnOutRule(player.worldObj, player.posX, player.posY, player.posZ, type);
            boolean bruleOut = brule.getTimer() != -1;
            boolean rainedOut = CampfireBackportConfig.putOutByRain.matches(type);

            if (bruleOut || rainedOut)
            {
                LinkedList<String> tooltip = new LinkedList<String>();

                if (bruleOut)
                {
                    if (brule.isDefaultRule())
                        tooltip.add(EnumChatFormatting.GOLD + StringParsers.translateNEI("approx_burn_out", brule.getTimer()));
                    else
                    {
                        tooltip.add(EnumChatFormatting.GOLD + StringParsers.translateNEI("approx_burn_out_in", brule.getTimer()));
                        tooltip.addAll(brule.getNEITooltip());
                    }
                }

                if (rainedOut)
                    tooltip.add(EnumChatFormatting.GOLD + StringParsers.translateNEI("rained_out"));

                if (!CampfireBackportConfig.signalFiresBurnOut.matches(type))
                {
                    tooltip.add("");
                    tooltip.add(EnumChatFormatting.GOLD + "" + EnumChatFormatting.ITALIC + StringParsers.translateNEI("signals_live"));
                }

                arecipes.add(new CachedCampfireStateChanger("burnout", EnumCampfireType.FROM_NAME.get(type), true, tooltip,
                        Lists.newArrayList(new ItemStack(Items.written_book))));
            }
        }
    }

    @Override
    public boolean handleMiscTooltipFromMousePosition(Point relMouse, CachedGenericRecipe cachedGrecipe, ItemStack stack, List<String> tooltip)
    {
        CachedCampfireStateChanger cachedCstate = (CachedCampfireStateChanger) cachedGrecipe;
        if (!tooltip.isEmpty() && cachedCstate.specialID != null && cachedCstate.specialID.equals("burnout") && cachedCstate.inputRects[0].contains(relMouse))
        {
            tooltip.remove(0);
            tooltip.addAll(cachedCstate.tooltips.get(0));
        }
        else if (!tooltip.isEmpty() && cachedCstate.dispensable && dispenserRect.contains(relMouse))
            tooltip.set(0, EnumChatFormatting.GOLD + "" + EnumChatFormatting.ITALIC + StringParsers.translateNEI("dispensable"));
        else
            return true;

        return false;
    }

    @Override
    public void drawBackground(int recipe)
    {
        GL11.glPushMatrix();
        GL11.glColor4f(1, 1, 1, 1);

        CachedCampfireStateChanger cachedCstate = (CachedCampfireStateChanger) this.arecipes.get(recipe % arecipes.size());

        if (cachedCstate != null && cachedCstate.types.length != 0)
        {
            GL11.glTranslatef(12, 32, 100);
            GL11.glRotatef(-30, 1, 0, 0);
            GL11.glRotatef(45, 0, 1, 0);
            GL11.glScalef(30, -30, 30);

            RenderCampfire.INSTANCE.renderSimple(cachedCstate.extinguisher, cachedCstate.getType(), cycleticks);

            GL11.glPopMatrix();
            GL11.glPushMatrix();
            GL11.glColor4f(1, 1, 1, 1);

            GL11.glTranslatef(130.5F, 21.5F, 100);
            GL11.glRotatef(-30, 1, 0, 0);
            GL11.glRotatef(-45, 0, 1, 0);
            GL11.glScalef(30, -30, 30);

            RenderCampfire.INSTANCE.renderSimple(!cachedCstate.extinguisher, cachedCstate.getType(), cycleticks);

            GL11.glPopMatrix();
            GL11.glPushMatrix();
            GL11.glColor4f(1, 1, 1, 1);

            GuiDraw.changeTexture(TextureMap.locationBlocksTexture);

            // this is a real mess...
            GL11.glTranslatef(9, 21, -20);
            GL11.glScalef(3, 3, 3);
            RenderItem.getInstance().renderItemIntoGUI(fonty, rendy, grassStack, 0, 0);

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
            RenderItem.getInstance().renderItemIntoGUI(fonty, rendy, grassStack, 0, 0);

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

            GL11.glTranslatef(0, 0, 50);

            GuiDraw.changeTexture(neiBackground);

            if (!drawSpecialBackground(cachedCstate))
            {
                GuiDraw.drawTexturedModalRect(56, 0, cachedCstate.leftClick ? 0 : 60, cachedCstate.output != null ? 59 : 2, 52, 41);

                GL11.glTranslatef(0, 0, 4);

                if (cachedCstate.dispensable)
                    drawSlot(dispenser.relx, dispenser.rely);
            }
        }

        GL11.glPopMatrix();
    }

    /**
     * Handles background display for non-recipe state changers.
     * 
     * @return true if a special display was done and so we shouldn't continue to regular display stuff, false otherwise
     */
    public boolean drawSpecialBackground(CachedCampfireStateChanger cachedCstate)
    {
        if (cachedCstate.specialID == null)
            return false;
        else if (cachedCstate.specialID.equals("burnout"))
        {
            GuiDraw.drawTexturedModalRect(56, 0, 120, 59, 52, 41);

            String info = StringParsers.translateNEI("burn_out");
            fonty.drawString(info, 82 - fonty.getStringWidth(info) / 2, 6, 0x777777);
        }
        else if (cachedCstate.specialID.equals("wand"))
        {
            GuiDraw.drawTexturedModalRect(56, 0, 60, 2, 52, 41);
        }

        return true;
    }

}
