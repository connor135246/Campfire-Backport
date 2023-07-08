package connor135246.campfirebackport.client.compat.nei;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;
import connor135246.campfirebackport.common.compat.CampfireBackportCompat;
import connor135246.campfirebackport.common.items.ItemBlockCampfire;
import connor135246.campfirebackport.common.recipes.BurnOutRule;
import connor135246.campfirebackport.common.recipes.CampfireStateChanger;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.config.ConfigNetworkManager;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import connor135246.campfirebackport.util.StringParsers;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

public class NEICampfireStateChangerHandler extends NEIGenericRecipeHandler
{
    // thanks to immersive engineering for uhh... having a github. :)

    public static final String outputID = Reference.NEI_STATECHANGER_ID;

    /** dispenser stack for {@link CampfireStateChanger#dispensable} recipes */
    public static final PositionedStack dispenser = new PositionedStack(new ItemStack(Blocks.dispenser), 94, 41);
    /** rectangle for the {@link CampfireStateChanger#dispensable} tooltip */
    public static final Rectangle dispenserRect = new Rectangle(93, 40, 20, 20);

    /** recipe transfer rects */
    public static final RecipeTransferRect transfer1 = new RecipeTransferRect(new Rectangle(13, 10, 40, 36), outputID),
            transfer2 = new RecipeTransferRect(new Rectangle(111, 10, 40, 36), outputID),
            transfer3 = new RecipeTransferRect(new Rectangle(70, 0, 24, 15), outputID);

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

        /** if true, clicking on the input sends you to {@link NEISignalFireBlocksHandler} */
        public boolean sendsToSignalBlocks = false;

        public CachedCampfireStateChanger(CampfireStateChanger cstate)
        {
            super(cstate);

            List<ItemStack> expandedInputList = expandInputList(cstate.getInput());
            if (expandedInputList.isEmpty()) // if the recipe has no inputs, it's invalid!
            {
                types = EnumCampfireType.NEITHER;
                return;
            }
            inputs.add(new PositionedStack(expandedInputList, 74, 17, false));
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
        public CachedCampfireStateChanger(String specialID, EnumCampfireType type, boolean extinguisher, ArrayList<LinkedList<String>> tooltips,
                List<ItemStack> inputStacks, boolean sendsToSignalBlocks)
        {
            super(null);

            this.types = type;

            this.specialID = specialID;
            this.extinguisher = extinguisher;

            this.tooltips = tooltips;

            numInputs = 1;
            inputs = new ArrayList<PositionedStack>(1);
            inputs.add(new PositionedStack(inputStacks, 74, 17, false));
            inputRects = new Rectangle[] { new Rectangle(73, 16, 20, 20) };

            this.sendsToSignalBlocks = sendsToSignalBlocks;
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
            if (matchesCrafting(cstate, result))
                loadValidRecipe(cstate);
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
            if (matchesUsage(cstate, ingredient))
                loadValidRecipe(cstate);
    }

    public void loadValidRecipe(CampfireStateChanger cstate)
    {
        CachedCampfireStateChanger cachedCstate = new CachedCampfireStateChanger(cstate);
        if (cachedCstate.types != null && cachedCstate.types != EnumCampfireType.NEITHER)
            arecipes.add(cachedCstate);
    }

    @Override
    public void loadAllRecipes()
    {
        for (CampfireStateChanger cstate : CampfireStateChanger.getMasterList())
            loadValidRecipe(cstate);

        // creating non-recipe state changers

        // wand
        if (CampfireBackportCompat.isThaumcraftLoaded)
        {
            Item wand = GameData.getItemRegistry().getObject("Thaumcraft:WandCasting");
            if (wand != null)
            {
                List<ItemStack> wandList = new ArrayList<ItemStack>();
                wand.getSubItems(wand, CreativeTabs.tabAllSearch, wandList);

                if (!wandList.isEmpty())
                {
                    boolean sameCosts = CampfireBackportConfig.visCosts[0] == CampfireBackportConfig.visCosts[1]
                            && CampfireBackportConfig.visCosts[2] == CampfireBackportConfig.visCosts[3];

                    for (boolean extinguisher : new boolean[] { true, false })
                    {
                        double regCost = CampfireBackportConfig.visCosts[extinguisher ? 0 : 2];
                        double soulCost = CampfireBackportConfig.visCosts[extinguisher ? 1 : 3];

                        if (regCost > 0.0)
                        {
                            arecipes.add(new CachedCampfireStateChanger("wand", sameCosts ? EnumCampfireType.BOTH : EnumCampfireType.REG_ONLY,
                                    extinguisher, new ArrayList<LinkedList<String>>(), wandList, false));
                        }

                        if (!sameCosts && soulCost > 0.0)
                        {
                            arecipes.add(new CachedCampfireStateChanger("wand", EnumCampfireType.SOUL_ONLY,
                                    extinguisher, new ArrayList<LinkedList<String>>(), wandList, false));
                        }
                    }
                }
            }
        }

        // branch & brew
        // check that witchery mixins are enabled!
        if (ConfigNetworkManager.mixins && ConfigNetworkManager.witcheryMixins && Loader.isModLoaded("witchery"))
        {
            Item branch = GameData.getItemRegistry().getObject("witchery:mysticbranch");
            if (branch != null)
            {
                arecipes.add(new CachedCampfireStateChanger("branch", EnumCampfireType.BOTH, true, new ArrayList<LinkedList<String>>(),
                        Lists.newArrayList(new ItemStack(branch)), false));
                arecipes.add(new CachedCampfireStateChanger("branch", EnumCampfireType.BOTH, false, new ArrayList<LinkedList<String>>(),
                        Lists.newArrayList(new ItemStack(branch)), false));
            }

            Item brew = GameData.getItemRegistry().getObject("witchery:brewbottle");
            if (brew != null)
            {
                String brewOf = StatCollector.translateToLocal("witchery:brew.potion");
                String extinguish = StatCollector.translateToLocal("witchery:brew.extinguish");
                String flames = StatCollector.translateToLocal("witchery:brew.inferno");
                String splash = StatCollector.translateToLocal("witchery:brew.dispersal.splash");

                ItemStack extinguishBrew = new ItemStack(brew);
                NBTTagCompound extinguishNBT = new NBTTagCompound();
                extinguishNBT.setString("BrewName", splash + " " + brewOf + " " + extinguish + " ");
                extinguishNBT.setString("BrewInfo", splash + "\n" + extinguish + "\n");
                extinguishNBT.setBoolean("Splash", true);
                extinguishNBT.setInteger("Color", 202434153);
                extinguishNBT.setInteger("Power", 0);
                extinguishNBT.setInteger("EffectCount", 1);
                extinguishNBT.setInteger("UsedCapacity", 1);
                extinguishNBT.setInteger("RemainingCapacity", 0);
                extinguishNBT.setInteger("BrewDrinkSpeed", 32);
                extinguishBrew.setTagCompound(extinguishNBT);
                arecipes.add(new CachedCampfireStateChanger("brew", EnumCampfireType.BOTH, true, new ArrayList<LinkedList<String>>(),
                        Lists.newArrayList(extinguishBrew), false));

                ItemStack flamesBrew = new ItemStack(brew);
                NBTTagCompound flamesNBT = new NBTTagCompound();
                flamesNBT.setString("BrewName", splash + " " + brewOf + " " + flames + " ");
                flamesNBT.setString("BrewInfo", splash + "\n" + flames + "\n");
                flamesNBT.setBoolean("Splash", true);
                flamesNBT.setInteger("Color", -1972966640);
                flamesNBT.setInteger("Power", 900);
                flamesNBT.setInteger("EffectCount", 1);
                flamesNBT.setInteger("UsedCapacity", 3);
                flamesNBT.setInteger("RemainingCapacity", 2);
                flamesNBT.setInteger("BrewDrinkSpeed", 32);
                flamesBrew.setTagCompound(flamesNBT);
                arecipes.add(new CachedCampfireStateChanger("brew", EnumCampfireType.BOTH, false, new ArrayList<LinkedList<String>>(),
                        Lists.newArrayList(flamesBrew), false));
            }
        }

        // lens
        if (CampfireBackportCompat.isBotaniaLoaded)
        {
            Item lens = GameData.getItemRegistry().getObject("Botania:lens");
            if (lens != null)
            {
                arecipes.add(new CachedCampfireStateChanger("lens", EnumCampfireType.BOTH, false, new ArrayList<LinkedList<String>>(),
                        Lists.newArrayList(new ItemStack(lens, 1, 15)), false)); // meta 15 is kindle lens
            }
        }

        // burning out
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        BurnOutRule bruleReg = BurnOutRule.findBurnOutRule(player.worldObj, player.posX, player.posY, player.posZ, EnumCampfireType.regular);
        BurnOutRule bruleSoul = BurnOutRule.findBurnOutRule(player.worldObj, player.posX, player.posY, player.posZ, EnumCampfireType.soul);
        if (bruleReg.getTimer() != -1 || bruleSoul.getTimer() != -1 || CampfireBackportConfig.putOutByRain != EnumCampfireType.NEITHER)
        {
            if ((bruleReg == bruleSoul || (bruleReg.isDefaultRule() && bruleSoul.isDefaultRule() && bruleReg.getTimer() == bruleSoul.getTimer()))
                    && CampfireBackportConfig.putOutByRain.sameForBoth() && CampfireBackportConfig.signalFiresBurnOut.sameForBoth())
            {
                addBurnoutStateChanger(bruleReg, EnumCampfireType.BOTH);
            }
            else
            {
                if (bruleReg.getTimer() != -1 || CampfireBackportConfig.putOutByRain.acceptsRegular())
                    addBurnoutStateChanger(bruleReg, EnumCampfireType.REG_ONLY);

                if (bruleSoul.getTimer() != -1 || CampfireBackportConfig.putOutByRain.acceptsSoul())
                    addBurnoutStateChanger(bruleSoul, EnumCampfireType.SOUL_ONLY);
            }

        }
    }

    protected void addBurnoutStateChanger(BurnOutRule brule, EnumCampfireType types)
    {
        ArrayList<LinkedList<String>> tooltips = new ArrayList<LinkedList<String>>();

        // there are different versions of the tooltips. one with the burn out in human readable time, the other with it in exact ticks. the rest is identical.
        LinkedList<String> tooltipReadable = new LinkedList<String>();
        LinkedList<String> tooltipTicks = new LinkedList<String>();

        if (brule.getTimer() != -1)
        {
            if (brule.isDefaultRule())
            {
                // human readable
                tooltipReadable.add(EnumChatFormatting.GOLD + StringParsers.translateNEI("approx_burn_out",
                        StringParsers.translateTimeHumanReadable(brule.getTimer())));
                // ticks
                tooltipTicks.add(EnumChatFormatting.GOLD + StringParsers.translateNEI("approx_burn_out",
                        StringParsers.translateTime("ticks", brule.getTimer() + "")));
            }
            else
            {
                // human readable
                tooltipReadable.add(EnumChatFormatting.GOLD + StringParsers.translateNEI("approx_burn_out_in",
                        StringParsers.translateTimeHumanReadable(brule.getTimer())));
                // ticks
                tooltipTicks.add(EnumChatFormatting.GOLD + StringParsers.translateNEI("approx_burn_out_in",
                        StringParsers.translateTime("ticks", brule.getTimer() + "")));

                if (brule.hasBiomeId())
                {
                    String biomeId = EnumChatFormatting.GRAY + StringParsers.translateNEI("biome") + " " + brule.getBiomeName();
                    tooltipReadable.add(biomeId);
                    tooltipTicks.add(biomeId);
                }

                if (brule.hasDimensionId())
                {
                    String dimId = EnumChatFormatting.GRAY + StringParsers.translateNEI("dimension") + " " + brule.getDimensionName();
                    tooltipReadable.add(dimId);
                    tooltipTicks.add(dimId);
                }
            }
        }

        if (CampfireBackportConfig.putOutByRain.accepts(types))
        {
            String rain = EnumChatFormatting.GOLD + StringParsers.translateNEI("rained_out");
            tooltipReadable.add(rain);
            tooltipTicks.add(rain);
        }

        boolean signalsLive = !CampfireBackportConfig.signalFiresBurnOut.accepts(types);
        if (signalsLive)
        {
            String signal = EnumChatFormatting.GOLD + "" + EnumChatFormatting.ITALIC + StringParsers.translateNEI("signals_live");
            tooltipReadable.add("");
            tooltipTicks.add("");
            tooltipReadable.add(signal);
            tooltipTicks.add(signal);

            String seeSignals = StringParsers.translateNEI("see_signal_fire_blocks");
            tooltipReadable.add(seeSignals);
            tooltipTicks.add(seeSignals);
        }

        tooltips.add(tooltipReadable);
        tooltips.add(tooltipTicks);
        arecipes.add(new CachedCampfireStateChanger("burnout", types, true, tooltips, Lists.newArrayList(new ItemStack(Items.written_book)), signalsLive));
    }

    @Override
    public boolean mouseClicked(GuiRecipe gui, int button, int recipe)
    {
        boolean result = false;
        CachedCampfireStateChanger cachedCstate = (CachedCampfireStateChanger) this.arecipes.get(recipe % arecipes.size());

        if (cachedCstate.sendsToSignalBlocks && cachedCstate.inputRects[0].contains(getRelMouse(gui, recipe)))
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
        CachedCampfireStateChanger cachedCstate = (CachedCampfireStateChanger) cachedGrecipe;
        if (!tooltip.isEmpty() && cachedCstate.specialID != null && cachedCstate.specialID.equals("burnout") && cachedCstate.inputRects[0].contains(relMouse))
        {
            tooltip.clear();
            if (NEIClientUtils.shiftKey())
                tooltip.addAll(cachedCstate.tooltips.get(1)); // ticks
            else
                tooltip.addAll(cachedCstate.tooltips.get(0)); // human readable
        }
        else if (!tooltip.isEmpty() && cachedCstate.dispensable && dispenserRect.contains(relMouse))
        {
            tooltip.clear();
            tooltip.add(EnumChatFormatting.GOLD + "" + EnumChatFormatting.ITALIC + StringParsers.translateNEI("dispensable"));
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

        CachedCampfireStateChanger cachedCstate = (CachedCampfireStateChanger) this.arecipes.get(recipe % arecipes.size());

        if (cachedCstate != null && cachedCstate.types != EnumCampfireType.NEITHER)
        {
            GuiDraw.changeTexture(TextureMap.locationBlocksTexture);

            // this is a real mess...
            GL11.glTranslatef(12, 58, 0);
            renderBlock(Blocks.grass);

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
            GL11.glTranslatef(109.5F, 58, -6);
            renderBlock(Blocks.grass);

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

            GL11.glTranslatef(12, 32, 100);

            renderCampfire(cachedCstate.types, cachedCstate.extinguisher, 2);

            GL11.glPopMatrix();
            GL11.glPushMatrix();
            GL11.glColor4f(1, 1, 1, 1);

            GL11.glTranslatef(109.5F, 32, 100);

            renderCampfire(cachedCstate.types, !cachedCstate.extinguisher, 4);

            GL11.glPopMatrix();
            GL11.glPushMatrix();
            GL11.glColor4f(1, 1, 1, 1);

            GL11.glTranslatef(0, 0, 50);

            GuiDraw.changeTexture(neiBackground);

            if (!drawSpecialBackground(cachedCstate))
            {
                GuiDraw.drawTexturedModalRect(56, 0, cachedCstate.leftClick ? 0 : 60, cachedCstate.hasOutputs ? 59 : 2, 52, 41);

                if (cachedCstate.dispensable)
                {
                    GL11.glTranslatef(0, 0, 4);
                    drawSlot(dispenser.relx, dispenser.rely);
                }
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
            GuiDraw.drawTexturedModalRect(56, 0, 120, 59, 52, 41);

            double cost = CampfireBackportConfig.visCosts[(cachedCstate.types.acceptsRegular() ? 0 : 1) + (cachedCstate.extinguisher ? 0 : 2)];
            String info = cost + (cachedCstate.extinguisher ? " Aqua" : " Ignis");
            fonty.drawString(info, 82 - fonty.getStringWidth(info) / 2, 6, cachedCstate.extinguisher ? 0x00AAAA : 0xFF5555);
        }
        else if (cachedCstate.specialID.equals("branch"))
        {
            GuiDraw.drawTexturedModalRect(56, 0, 120, 59, 52, 41);

            String info = StatCollector.translateToLocal(cachedCstate.extinguisher ? "witchery.pott.aguamenti" : "witchery.pott.incendio")
                    + " " + StatCollector.translateToLocal("enchantment.level.1");
            fonty.drawString(info, 82 - fonty.getStringWidth(info) / 2, 6, 0x777777);
        }
        else if (cachedCstate.specialID.equals("brew"))
        {
            GuiDraw.drawTexturedModalRect(56, 0, 120, 59, 52, 41);

            String info = StatCollector.translateToLocal(cachedCstate.extinguisher ? "witchery:brew.extinguish" : "witchery:brew.inferno");
            fonty.drawString(info, 82 - fonty.getStringWidth(info) / 2, 6, 0x777777);
        }
        else if (cachedCstate.specialID.equals("lens"))
        {
            GuiDraw.drawTexturedModalRect(56, 0, 120, 59, 52, 41);

            String info = StatCollector.translateToLocal("entity.Botania.botania:manaBurst.name");
            fonty.drawString(info, 82 - fonty.getStringWidth(info) / 2, 6, 0x777777);
        }

        return true;
    }

}
