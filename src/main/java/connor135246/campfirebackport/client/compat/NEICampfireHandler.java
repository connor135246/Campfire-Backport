package connor135246.campfirebackport.client.compat;

import static codechicken.lib.gui.GuiDraw.drawRect;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.TemplateRecipeHandler;
import connor135246.campfirebackport.client.rendering.RenderCampfire;
import connor135246.campfirebackport.common.crafting.CampfireRecipe;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.oredict.OreDictionary;

public class NEICampfireHandler extends TemplateRecipeHandler
{
    // thanks to immersive engineering for uhh... having a github. :)

    public class CachedCampfireRecipe extends CachedRecipe
    {
        public List<PositionedStack> inputs = new ArrayList<PositionedStack>(4);
        public PositionedStack output;
        public int ticks;
        public List<String> types = new ArrayList<String>(2);
        /** true if this is an ItemStack crecipe. false if this is an OreDict crecipe. */
        public boolean stackInput;
        /** null if this is an ItemStack crecipe. otherwise, it's the OreDict. */
        public String oreDict;

        public CachedCampfireRecipe(CampfireRecipe crecipe)
        {
            if (crecipe.getTypes().matches(EnumCampfireType.REGULAR))
                types.add(EnumCampfireType.REGULAR);
            if (crecipe.getTypes().matches(EnumCampfireType.SOUL))
                types.add(EnumCampfireType.SOUL);
            if (types.isEmpty())
                return;

            stackInput = !crecipe.isOreDictRecipe();

            ItemStack stack = ItemStack.copyItemStack(crecipe.getInputStack());
            oreDict = crecipe.getInputOre();
            ArrayList<ItemStack> oreDictStacks = OreDictionary.getOres(oreDict);

            boolean cycleMetas = stackInput && !crecipe.doesMetaMatter() && stack.getItem().getHasSubtypes();
            List metalist = new ArrayList<ItemStack>();
            if (cycleMetas)
                stack.getItem().getSubItems(stack.getItem(), CreativeTabs.tabAllSearch, metalist);

            for (int i = 0; i < crecipe.getInputSize(); ++i)
            {
                if (stackInput)
                {
                    if (cycleMetas)
                        inputs.add(new PositionedStack(metalist, getInputX(i), getInputY(i)));
                    else
                        inputs.add(new PositionedStack(stack, getInputX(i), getInputY(i)));
                }
                else
                    inputs.add(new PositionedStack(oreDictStacks, getInputX(i), getInputY(i)));
            }

            output = new PositionedStack(crecipe.getOutput(), 120, 25);
            ticks = crecipe.getCookingTime();
        }

        @Override
        public List<PositionedStack> getIngredients()
        {
            return getCycledIngredients(cycleticks / 20, inputs);
        }

        public String getType()
        {
            if (types.size() > 1)
                return cycleticks % 40 < 20 ? types.get(0) : types.get(1);
            else
                return types.get(0);
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
        return 16 + (i > 1 ? 18 : 0);
    }

    @Override
    public String getRecipeName()
    {
        return StatCollector.translateToLocal("tile.campfire.name");
    }

    @Override
    public String getGuiTexture()
    {
        return "minecraft:textures/gui/container/furnace.png";
    }

    @Override
    public void loadTransferRects()
    {
        transferRects.add(new RecipeTransferRect(new Rectangle(64, 20, 36, 26), getRecipeID()));
    }

    public String getRecipeID()
    {
        return Reference.MODID + ".recipeCampfire";
    }

    @Override
    public int recipiesPerPage()
    {
        return 2;
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
            if (cachedCrecipe != null && !cachedCrecipe.types.isEmpty() && cachedCrecipe.contains(cachedCrecipe.inputs, ingredient))
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
            for (int i = 0; i < 4; ++i)
                drawSlot(getInputX(i), getInputY(i), 16, 16);

            drawSlot(cachedCrecipe.output.relx, cachedCrecipe.output.rely, 16, 16);

            String ticks = cachedCrecipe.ticks + " Ticks";
            Minecraft.getMinecraft().fontRenderer.drawString(ticks, 82 - Minecraft.getMinecraft().fontRenderer.getStringWidth(ticks) / 2, 2, 0x777777, false);
            GL11.glColor4f(1, 1, 1, 1);

            GL11.glTranslatef(61, 40, 100);
            GL11.glRotatef(-30, 1, 0, 0);
            GL11.glRotatef(45, 0, 1, 0);
            GL11.glScalef(30, -30, 30);

            TileEntityCampfire tilecamp = new TileEntityCampfire();
            tilecamp.setThisLit(true);
            tilecamp.setThisType(cachedCrecipe.getType());
            tilecamp.setAnimTimer(cycleticks);

            ((RenderCampfire) TileEntityRendererDispatcher.instance.getSpecialRenderer(tilecamp)).renderStatic(tilecamp, 0.0, 0.0, 0.0);
        }

        GL11.glPopMatrix();
    }

    public static void drawSlot(int x, int y, int w, int h)
    {
        drawRect(x + 8 - w / 2, y + 8 - h / 2 - 1, w, 1, 0xff373737);
        drawRect(x + 8 - w / 2 - 1, y + 8 - h / 2 - 1, 1, h + 1, 0xff373737);
        drawRect(x + 8 - w / 2, y + 8 - h / 2, w, h, 0xff8b8b8b);
        drawRect(x + 8 - w / 2, y + 8 + h / 2, w + 1, 1, 0xffffffff);
        drawRect(x + 8 + w / 2, y + 8 - h / 2, 1, h, 0xffffffff);
    }

    @Override
    public List<String> handleItemTooltip(GuiRecipe gui, ItemStack stack, List<String> tooltip, int recipe)
    {
        CachedCampfireRecipe cachedCrecipe = (CachedCampfireRecipe) this.arecipes.get(recipe % arecipes.size());

        if (cachedCrecipe != null && !cachedCrecipe.types.isEmpty() && !cachedCrecipe.stackInput && CampfireRecipe.matchesTheOre(cachedCrecipe.oreDict, stack))
            tooltip.add(EnumChatFormatting.GRAY + cachedCrecipe.oreDict);

        return tooltip;
    }

}
