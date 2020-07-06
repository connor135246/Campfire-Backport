package connor135246.campfirebackport.client.compat;

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.TemplateRecipeHandler;
import connor135246.campfirebackport.common.crafting.GenericCustomInput;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.oredict.OreDictionary;

public abstract class NEIGenericCustomInputHandler extends TemplateRecipeHandler
{
    // thanks to immersive engineering for uhh... having a github. :)

    public abstract class CachedGenericCustomInput extends CachedRecipe
    {
        public List<ItemStack> neiList = new ArrayList<ItemStack>();
        public List<String> types = new ArrayList<String>(2);
        /** {@link GenericCustomInput#inputType} */
        public byte inputType = -1;
        /** {@link GenericCustomInput#dataType} */
        public byte dataType = -1;
        /** {@link GenericCustomInput#neiTooltip} */
        public List<String> tooltip = new LinkedList<String>();

        public CachedGenericCustomInput(GenericCustomInput ginput)
        {
            if (ginput.getTypes().matches(EnumCampfireType.REGULAR))
                this.types.add(EnumCampfireType.REGULAR);
            if (ginput.getTypes().matches(EnumCampfireType.SOUL))
                this.types.add(EnumCampfireType.SOUL);
            if (this.types.isEmpty())
                return;

            this.inputType = ginput.getInputType();
            this.dataType = ginput.getDataType();

            this.tooltip.addAll(ginput.getNEITooltip());
            
            for (ItemStack stack : ginput.getInputList())
            {
                if (stack.getItemDamage() == OreDictionary.WILDCARD_VALUE)
                {
                    List<ItemStack> metaList = new ArrayList<ItemStack>();
                    stack.getItem().getSubItems(stack.getItem(), CreativeTabs.tabAllSearch, metaList);
                    if (ginput.doesInputSizeMatter() || ginput.hasExtraData())
                    {
                        for (ItemStack metaStack : metaList)
                        {
                            if (ginput.doesInputSizeMatter())
                                metaStack.stackSize = ginput.getInputSize();
                            if (ginput.hasExtraData())
                                metaStack.setTagCompound((NBTTagCompound) ginput.getExtraData().copy());
                        }
                    }
                    neiList.addAll(metaList);
                }
                else
                    neiList.add(stack);
            }
        }

        public String getType()
        {
            if (types.size() > 1)
                return types.get((cycleticks % 40) / 20);
            else
                return types.get(0);
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
        return "minecraft:textures/gui/container/furnace.png";
    }

    @Override
    public abstract String getRecipeName();

    @Override
    public List<String> handleItemTooltip(GuiRecipe gui, ItemStack stack, List<String> tooltip, int recipe)
    {
        CachedGenericCustomInput cachedGinput = (CachedGenericCustomInput) this.arecipes.get(recipe % arecipes.size());

        if (cachedGinput != null && !cachedGinput.types.isEmpty())
        {
            Point mouse = GuiDraw.getMousePosition();
            Point offset = gui.getRecipePosition(recipe);
            Point relMouse = new Point(mouse.x - (gui.width - 176) / 2 - offset.x, mouse.y - (gui.height - 166) / 2 - offset.y);

            return handleItemTooltipFromMousePosition(relMouse, cachedGinput, stack, tooltip);
        }
        else
            return tooltip;
    }

    public List<String> handleItemTooltipFromMousePosition(Point relMouse, CachedGenericCustomInput cachedGinput, ItemStack stack, List<String> tooltip)
    {
        if (!tooltip.isEmpty() && hoveringOverInput(relMouse, cachedGinput))
        {
            if (cachedGinput.inputType == 5)
            {
                if (cachedGinput.dataType == 3)
                    tooltip.set(0, EnumChatFormatting.ITALIC + StatCollector.translateToLocal(Reference.MODID + ".nei.any_tinkers"));
                else
                    tooltip.set(0, EnumChatFormatting.ITALIC + StatCollector.translateToLocal(Reference.MODID + ".nei.anything"));
            }
            
            tooltip.addAll(cachedGinput.tooltip);
        }
        return tooltip;
    }
    
    public abstract boolean hoveringOverInput(Point relMouse, CachedGenericCustomInput cachedGinput);

    public static void drawSlot(int x, int y, int w, int h)
    {
        GuiDraw.drawRect(x + 8 - w / 2, y + 8 - h / 2 - 1, w, 1, 0xff373737);
        GuiDraw.drawRect(x + 8 - w / 2 - 1, y + 8 - h / 2 - 1, 1, h + 1, 0xff373737);
        GuiDraw.drawRect(x + 8 - w / 2, y + 8 - h / 2, w, h, 0xff8b8b8b);
        GuiDraw.drawRect(x + 8 - w / 2, y + 8 + h / 2, w + 1, 1, 0xffffffff);
        GuiDraw.drawRect(x + 8 + w / 2, y + 8 - h / 2, 1, h, 0xffffffff);
    }

}
