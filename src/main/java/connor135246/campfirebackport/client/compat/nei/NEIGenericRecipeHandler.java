package connor135246.campfirebackport.client.compat.nei;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.TemplateRecipeHandler;
import connor135246.campfirebackport.common.compat.CampfireBackportCompat.ICraftTweakerIngredient;
import connor135246.campfirebackport.common.recipes.CustomInput;
import connor135246.campfirebackport.common.recipes.GenericRecipe;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.MiscUtil;
import connor135246.campfirebackport.util.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public abstract class NEIGenericRecipeHandler extends TemplateRecipeHandler
{
    // thanks to immersive engineering for uhh... having a github. :)

    public static final FontRenderer fonty = Minecraft.getMinecraft().fontRenderer;
    public static final TextureManager rendy = Minecraft.getMinecraft().renderEngine;

    public static final String basicBackground = "minecraft:textures/gui/container/furnace.png",
            neiBackground = Reference.MODID + ":" + "textures/gui/neiElements.png";

    public static final ItemStack hayStack = new ItemStack(Blocks.hay_block),
            stoneStack = new ItemStack(Blocks.stone),
            grassStack = new ItemStack(Blocks.grass);

    public abstract class CachedGenericRecipe extends CachedRecipe
    {
        /** {@link GenericRecipe#types} */
        public EnumCampfireType types;
        /** {@link GenericRecipe#inputs} */
        public List<PositionedStack> inputs;
        /** {@link GenericRecipe#outputs} */
        public PositionedStack output;
        /** the length of {@link GenericRecipe#inputs} */
        public int numInputs;

        /** {@link CustomInput#neiTooltip} */
        public List<LinkedList<String>> tooltips;

        /** rectangles that cover each input */
        public Rectangle[] inputRects;

        public CachedGenericRecipe(GenericRecipe grecipe)
        {
            if (grecipe != null)
            {
                types = grecipe.getTypes();

                numInputs = grecipe.getInputs().length;

                tooltips = new ArrayList<LinkedList<String>>(numInputs);
                for (int cinputIndex = 0; cinputIndex < numInputs; ++cinputIndex)
                {
                    tooltips.add(new LinkedList<String>());
                    tooltips.get(cinputIndex).addAll(grecipe.getInputs()[cinputIndex].getNEITooltip());
                }

                inputs = new ArrayList<PositionedStack>(numInputs);
                inputRects = new Rectangle[numInputs];
            }
        }

        /**
         * see {@link connor135246.campfirebackport.client.rendering.RenderCampfire#renderModelAt}
         */
        public @Nullable String getTypeStringForRender()
        {
            if (types == EnumCampfireType.BOTH)
                return null;
            else if (types == EnumCampfireType.SOUL_ONLY)
                return EnumCampfireType.soul;
            else
                return EnumCampfireType.regular;
        }

    }

    @Override
    public int recipiesPerPage()
    {
        return 2;
    }

    @Override
    public String getGuiTexture()
    {
        return basicBackground;
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results)
    {
        if (outputId.equals(getOutputID()))
            loadAllRecipes();
        else
            super.loadCraftingRecipes(outputId, results);
    }

    /**
     * @return the output id that identifies the type of recipe this is
     */
    public abstract String getOutputID();

    /**
     * Loads all the possible recipes to the matching recipes list.
     */
    public abstract void loadAllRecipes();

    @Override
    public List<String> handleItemTooltip(GuiRecipe gui, ItemStack stack, List<String> tooltip, int recipe)
    {
        CachedGenericRecipe cachedGrecipe = (CachedGenericRecipe) this.arecipes.get(recipe % arecipes.size());

        if (cachedGrecipe != null && cachedGrecipe.types != EnumCampfireType.NEITHER)
        {
            Point mouse = GuiDraw.getMousePosition();
            Point offset = gui.getRecipePosition(recipe);
            Point relMouse = new Point(mouse.x - (gui.width - 176) / 2 - offset.x, mouse.y - (gui.height - 166) / 2 - offset.y);

            if (handleMiscTooltipFromMousePosition(relMouse, cachedGrecipe, stack, tooltip))
                handleInputTooltipFromMousePosition(relMouse, cachedGrecipe, stack, tooltip);
        }

        return tooltip;
    }

    /**
     * Override to add recipe-specific tooltips.
     * 
     * @param relMouse
     *            - the player's mouse position
     * @return true to continue on to {@link #handleInputTooltipFromMousePosition}, false to skip checking for recipe input tooltips
     */
    public boolean handleMiscTooltipFromMousePosition(Point relMouse, CachedGenericRecipe cachedGrecipe, ItemStack stack, List<String> tooltip)
    {
        return true;
    }

    /**
     * Creates tooltips for the recipe's inputs.
     * 
     * @param relMouse
     *            - the player's mouse position
     */
    public void handleInputTooltipFromMousePosition(Point relMouse, CachedGenericRecipe cachedGrecipe, ItemStack stack, List<String> tooltip)
    {
        if (!tooltip.isEmpty())
        {
            int cinputIndex = hoveringOverInput(relMouse, cachedGrecipe);
            if (cinputIndex > -1 && cinputIndex < cachedGrecipe.numInputs)
                tooltip.addAll(cachedGrecipe.tooltips.get(cinputIndex));
        }
    }

    /**
     * @return the index of the CustomInput the player is hovering over, or -1 if they aren't hovering over an input.
     */
    public int hoveringOverInput(Point relMouse, CachedGenericRecipe cachedGrecipe)
    {
        for (int i = 0; i < cachedGrecipe.inputRects.length; ++i)
        {
            if (cachedGrecipe.inputRects[i].contains(relMouse))
                return i;
        }
        return -1;
    }

    // Static Methods

    /**
     * Returns the {@link CustomInput#inputList} expanded using {@link net.minecraft.item.Item#getSubItems} and modified with {@link CustomInput#modifyStackForDisplay}. <br>
     * Because {@link PositionedStack#generatePermutations()} doesn't always do what I want it to...
     */
    public static List<ItemStack> expandInputList(CustomInput cinput)
    {
        List<ItemStack> neiList = new ArrayList<ItemStack>();

        for (final ItemStack cinputStack : cinput.getInputList())
        {
            if (cinputStack.getItemDamage() == OreDictionary.WILDCARD_VALUE)
            {
                List<ItemStack> metaList = new ArrayList<ItemStack>();
                cinputStack.getItem().getSubItems(cinputStack.getItem(), CreativeTabs.tabAllSearch, metaList);

                for (ItemStack metaStack : metaList)
                {
                    if (cinput.isIIngredientInput() && cinputStack.hasTagCompound())
                        metaStack.setTagCompound(MiscUtil.mergeNBT(metaStack.getTagCompound(), cinputStack.getTagCompound()));

                    metaStack = cinput.modifyStackForDisplay(metaStack);

                    if (metaStack.getItem().isDamageable())
                    {
                        try
                        {
                            for (int i = 0; i < 4; ++i)
                            {
                                ItemStack damagedStack = metaStack.copy();
                                damagedStack.setItemDamage(damagedStack.getMaxDamage() * i / 4);
                                neiList.add(damagedStack);
                            }

                            continue;
                        }
                        catch (Exception excep)
                        {
                            // CommonProxy.modlog.error("Error while attempting to set a stack's damage: " + excep.getClass().getName() + ": " + excep.getLocalizedMessage());
                        }
                    }

                    neiList.add(metaStack);
                }
            }
            else
                neiList.add(cinput.modifyStackForDisplay(cinputStack.copy()));
        }

        return neiList;
    }

    /**
     * @return true if the result matches any of the grecipe's outputs
     */
    public static boolean matchesCrafting(GenericRecipe grecipe, ItemStack result)
    {
        if (grecipe.hasOutputs())
        {
            for (ItemStack output : grecipe.getOutputs())
                if (NEIServerUtils.areStacksSameTypeCrafting(output, result))
                    return true;
        }
        return false;
    }

    /**
     * @return true if the ingredient matches any of the grecipe's inputs
     */
    public static boolean matchesUsage(GenericRecipe grecipe, ItemStack ingredient)
    {
        for (CustomInput cinput : grecipe.getInputs())
        {
            if (cinput.isDataInput())
            {
                if (CustomInput.matchesData(cinput, ingredient))
                    return true;
            }
            else if (cinput.isIIngredientInput() && ((ICraftTweakerIngredient) cinput.getInput()).isWildcard())
            {
                if (((ICraftTweakerIngredient) cinput.getInput()).matches(ingredient, false))
                    return true;
            }
            else
            {
                for (ItemStack cinputStack : cinput.getInputList())
                    if (NEIServerUtils.areStacksSameTypeCrafting(cinputStack, ingredient))
                        return true;
            }
        }
        return false;
    }

    /**
     * Draws an inventory slot at x, y.
     */
    public static void drawSlot(int x, int y)
    {
        GuiDraw.drawRect(x + 8 - 16 / 2, y + 8 - 16 / 2 - 1, 16, 1, 0xff373737);
        GuiDraw.drawRect(x + 8 - 16 / 2 - 1, y + 8 - 16 / 2 - 1, 1, 16 + 1, 0xff373737);
        GuiDraw.drawRect(x + 8 - 16 / 2, y + 8 - 16 / 2, 16, 16, 0xff8b8b8b);
        GuiDraw.drawRect(x + 8 - 16 / 2, y + 8 + 16 / 2, 16 + 1, 1, 0xffffffff);
        GuiDraw.drawRect(x + 8 + 16 / 2, y + 8 - 16 / 2, 1, 16, 0xffffffff);
    }

}
