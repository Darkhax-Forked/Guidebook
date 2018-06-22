package gigaherz.guidebook.guidebook.drawing;

import gigaherz.guidebook.guidebook.IBookGraphics;

public class VisualPageBreak extends VisualElement
{
    public VisualPageBreak(Size size)
    {
        super(size, 0, 0, 0);
    }

    @Override
    public void draw(IBookGraphics nav)
    {
        // not a drawableelement
    }
}
