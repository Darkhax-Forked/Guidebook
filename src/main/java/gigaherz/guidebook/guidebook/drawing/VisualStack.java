package gigaherz.guidebook.guidebook.drawing;

import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.PageRef;
import net.minecraft.item.ItemStack;

public class VisualStack extends VisualElement
{
    public static final int CYCLE_TIME = 1000;//=1s

    public ItemStack[] stacks;
    public float scale = 1.0f;
    public int z;

    public VisualStack(ItemStack[] stacks, Size size, float scale)
    {
        super(size);
        this.stacks = stacks;
        this.scale = scale;
    }

    public ItemStack getCurrentStack()
    {
        if (stacks == null || stacks.length == 0)
            return ItemStack.EMPTY;
        long time = System.currentTimeMillis();
        return stacks[(int) ((time / CYCLE_TIME) % stacks.length)];
    }

    @Override
    public void draw(IBookGraphics nav)
    {
        ItemStack stack = getCurrentStack();
        if (stack.getCount() > 0)
        {
            nav.drawItemStack(position.x, position.y, z, stack, 0xFFFFFFFF, scale);
        }
    }

    @Override
    public boolean wantsHover()
    {
        return true;
    }

    @Override
    public void mouseOver(IBookGraphics nav, int x, int y)
    {
        ItemStack stack = getCurrentStack();
        if (stack.getCount() > 0)
        {
            nav.drawTooltip(stack, x, y);
        }
    }

    @Override
    public void click(IBookGraphics nav)
    {
        PageRef ref = nav.getBook().getStackLink(getCurrentStack());
        if (ref != null)
            nav.navigateTo(ref);
    }
}
