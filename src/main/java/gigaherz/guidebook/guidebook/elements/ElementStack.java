package gigaherz.guidebook.guidebook.elements;

import com.google.common.primitives.Ints;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.drawing.Point;
import gigaherz.guidebook.guidebook.drawing.Size;
import gigaherz.guidebook.guidebook.drawing.VisualElement;
import gigaherz.guidebook.guidebook.drawing.VisualStack;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.Collections;
import java.util.List;

public class ElementStack extends Element
{
    public static final String WILDCARD = "*";

    public ItemStack[] stacks;

    public ElementStack(int defaultPositionMode)
    {
        super(defaultPositionMode);
    }

    private VisualStack getVisual()
    {
        int width = (int) (16 * scale);
        int height = (int) (16 * scale);
        return new VisualStack(stacks, new Size(width,height), scale);
    }

    @Override
    public List<VisualElement> measure(IBookGraphics nav, int width, int firstLineWidth)
    {
        return Collections.singletonList(getVisual());
    }

    @Override
    public int reflow(List<VisualElement> paragraph, IBookGraphics nav, int left, int top, int width, int height)
    {
        VisualStack element = getVisual();
        element.position = new Point(left, top);
        paragraph.add(element);
        return top + element.size.height;
    }

    @Override
    public void parse(NamedNodeMap attributes)
    {
        int meta = 0;
        int stackSize = 1;
        NBTTagCompound tag = new NBTTagCompound();

        super.parse(attributes);

        Node attr = attributes.getNamedItem("meta");
        if (attr != null)
        {
            if (attr.getTextContent().equals(WILDCARD))
                meta = -1;
            else
                meta = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("count");
        if (attr != null)
        {
            stackSize = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("tag");
        if (attr != null)
        {
            try
            {
                tag = JsonToNBT.getTagFromJson(attr.getTextContent());
            }
            catch (NBTException e)
            {
                GuidebookMod.logger.warn("Invalid tag format: " + e.getMessage());
            }
        }

        attr = attributes.getNamedItem("item");
        if (attr != null)
        {
            String itemName = attr.getTextContent();

            Item item = Item.REGISTRY.getObject(new ResourceLocation(itemName));

            if (item != null)
            {
                if (((meta == OreDictionary.WILDCARD_VALUE) || meta == -1) && item.getHasSubtypes())
                {
                    NonNullList<ItemStack> processed_items = NonNullList.create();
                    NonNullList<ItemStack> subitems = NonNullList.create();

                    item.getSubItems(CreativeTabs.SEARCH, subitems);

                    for (ItemStack subitem : subitems)
                    {
                        subitem = subitem.copy();

                        subitem.setCount(stackSize);
                        subitem.setTagCompound(tag);

                        processed_items.add(subitem);
                    }

                    stacks = subitems.toArray(new ItemStack[subitems.size()]);
                }
                else
                {
                    ItemStack stack = new ItemStack(item, stackSize, meta);
                    stack.setTagCompound(tag);
                    stacks = new ItemStack[]{stack};
                }
            }
        }

        //get stacks from ore dictionary
        attr = attributes.getNamedItem("ore");
        if (attr != null)
        {
            String oreName = attr.getTextContent();
            //list of matching item stack; may contain wildcard meta data
            NonNullList<ItemStack> items = OreDictionary.getOres(oreName);

            if (items.size() != 0)
            {
                //init empty list to fill with resolved items
                NonNullList<ItemStack> items_processed = NonNullList.create();

                //foreach item: try to resolve wildcard meta data
                for (ItemStack item : items)
                {
                    //make sure not to mess up ore dictionary item stacks
                    item = item.copy();
                    meta = item.getMetadata();

                    if (meta == OreDictionary.WILDCARD_VALUE && item.getHasSubtypes())
                    {
                        //replace wildcard metas with subitems
                        NonNullList<ItemStack> subitems = NonNullList.create();
                        item.getItem().getSubItems(CreativeTabs.SEARCH, subitems);
                        for (ItemStack subitem : subitems)
                        {
                            //just in case the ItemStack instance is not just a copy or a new instance
                            subitem = subitem.copy();

                            subitem.setCount(stackSize);
                            subitem.setTagCompound(tag);
                            items_processed.add(subitem);
                        }
                    }
                    else
                    {
                        item.setCount(stackSize);
                        items_processed.add(item);
                    }
                }

                //
                stacks = items_processed.toArray(new ItemStack[items_processed.size()]);
            }
        }

    }

    @Override
    public Element copy()
    {
        ElementStack stack = super.copy(new ElementStack(position));
        if (this.stacks != null)
        {
            stack.stacks = new ItemStack[this.stacks.length];
            for (int i = 0; i < this.stacks.length; i++)
            {
                stack.stacks[i] = this.stacks[i].copy();
            }
        }
        else
        {
            stack.stacks = null;
        }
        return stack;
    }

    @Override
    public boolean supportsPageLevel()
    {
        return true;
    }
}
