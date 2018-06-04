package gigaherz.guidebook.guidebook.elements;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.IConditionSource;
import gigaherz.guidebook.guidebook.conditions.ConditionContext;
import gigaherz.guidebook.guidebook.drawing.Point;
import gigaherz.guidebook.guidebook.drawing.Rect;
import gigaherz.guidebook.guidebook.drawing.VisualElement;
import net.minecraft.util.ResourceLocation;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public class ElementPanel extends Element
{
    public final List<Element> innerElements;
    public boolean asPercent;
    public int space;

    public ElementPanel()
    {
        this.innerElements = Lists.newArrayList();
    }

    public ElementPanel(List<Element> innerElements)
    {
        this.innerElements = Lists.newArrayList(innerElements);
    }

    @Override
    public void parse(IConditionSource book, NamedNodeMap attributes)
    {
        super.parse(book, attributes);

        Node attr = attributes.getNamedItem("height");
        if (attr != null)
        {
            String t = attr.getTextContent();
            if (t.endsWith("%"))
            {
                asPercent = true;
                t = t.substring(0, t.length() - 1);
            }

            space = Ints.tryParse(t);
        }
    }

    @Override
    public boolean reevaluateConditions(ConditionContext ctx)
    {
        boolean oldValue = conditionResult;
        conditionResult = condition== null || condition.test(ctx);

        boolean anyChanged = conditionResult != oldValue;
        for(Element element : innerElements)
        {
            anyChanged |= element.reevaluateConditions(ctx);
        }

        return anyChanged;
    }

    @Override
    public int reflow(List<VisualElement> list, IBookGraphics nav, Rect bounds, Rect pageBounds)
    {
        Point adjustedPosition = applyPosition(bounds.position, bounds.position);
        Rect adjustedBounds = new Rect(adjustedPosition, bounds.size);
        for(Element element : innerElements)
        {
            element.reflow(list, nav, adjustedBounds, pageBounds);
        }
        if (position != 0)
            return bounds.position.y;
        return adjustedPosition.y+space;
    }

    @Override
    public void findTextures(Set<ResourceLocation> textures)
    {
        for (Element child : innerElements)
        {
            child.findTextures(textures);
        }
    }

    @Override
    public Element copy()
    {
        ElementPanel space = super.copy(new ElementPanel());
        space.asPercent = asPercent;
        space.space = this.space;
        return space;
    }

    @Nullable
    @Override
    public Element applyTemplate(IConditionSource book, List<Element> sourceElements)
    {
        if (innerElements.size() == 0)
            return null;

        ElementPanel paragraph = super.copy(new ElementPanel());
        paragraph.space = space;
        for(Element element : innerElements)
        {
            Element t = element.applyTemplate(book, sourceElements);
            if (t != null)
                paragraph.innerElements.add(t);
        }

        if (paragraph.innerElements.size() == 0)
            return null;

        return paragraph;
    }

    @Override
    public boolean supportsPageLevel()
    {
        return true;
    }
}
