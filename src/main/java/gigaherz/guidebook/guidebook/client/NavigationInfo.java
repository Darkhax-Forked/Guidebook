package gigaherz.guidebook.guidebook.client;

import gigaherz.common.client.StackRenderingHelper;
import gigaherz.guidebook.guidebook.BookDocument;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Rectangle;

public class NavigationInfo
{
    public static final int DEFAULT_BOOK_WIDTH = 276;
    public static final int DEFAULT_BOOK_HEIGHT = 198;
    public static final int DEFAULT_INNER_MARGIN = 22;
    public static final int DEFAULT_OUTER_MARGIN = 10;
    public static final int DEFAULT_VERTICAL_MARGIN = 18;

    private BookDocument book;
    int scaledWidth;
    int scaledHeight;

    final Minecraft mc = Minecraft.getMinecraft();
    final GuiGuidebook gui;

    private int bookWidth;
    private int bookHeight;
    private int innerMargin;
    private int outerMargin;
    private int verticalMargin;
    private int pageWidth = bookWidth / 2 - innerMargin - outerMargin;
    private int pageHeight = bookHeight - verticalMargin;

    final java.util.Stack<BookDocument.PageRef> history = new java.util.Stack<>();
    int currentChapter = 0;
    int currentPair = 0;
    private boolean hasScale;

    private float scalingFactor;

    NavigationInfo(BookDocument book, GuiGuidebook gui)
    {
        this.book = book;
        this.gui = gui;
    }

    public void computeScaledResolution2(Minecraft minecraftClient, float scaleFactorCoef)
    {
        this.scaledWidth = minecraftClient.displayWidth;
        this.scaledHeight = minecraftClient.displayHeight;
        int scaleFactor = 1;
        boolean flag = minecraftClient.isUnicode();
        int i = minecraftClient.gameSettings.guiScale;

        if (i == 0)
        {
            i = 1000;
        }

        while (scaleFactor < i && this.scaledWidth / (scaleFactor + 1) >= 320 && this.scaledHeight / (scaleFactor + 1) >= 240)
        {
            ++scaleFactor;
        }

        scaleFactor = MathHelper.floor_double(Math.max(1, scaleFactor * scaleFactorCoef));

        if (flag && scaleFactor % 2 != 0 && scaleFactor != 1)
        {
            --scaleFactor;
        }

        double scaledWidthD = (double) this.scaledWidth / (double) scaleFactor;
        double scaledHeightD = (double) this.scaledHeight / (double) scaleFactor;
        this.scaledWidth = MathHelper.ceiling_double_int(scaledWidthD);
        this.scaledHeight = MathHelper.ceiling_double_int(scaledHeightD);
    }

    public void setScalingFactor()
    {
        float fontSize = book.getFontSize();

        if (MathHelper.epsilonEquals(fontSize, 1.0f))
        {
            this.scaledWidth = gui.width;
            this.scaledHeight = gui.height;

            this.hasScale = false;
            this.scalingFactor = 1.0f;

            this.bookWidth = DEFAULT_BOOK_WIDTH;
            this.bookHeight = DEFAULT_BOOK_HEIGHT;
            this.innerMargin = DEFAULT_INNER_MARGIN;
            this.outerMargin = DEFAULT_OUTER_MARGIN;
            this.verticalMargin = DEFAULT_VERTICAL_MARGIN;
        }
        else
        {
            ScaledResolution sr = new ScaledResolution(mc);
            computeScaledResolution2(mc, fontSize);

            this.hasScale = true;
            this.scalingFactor = Math.min(sr.getScaledWidth() / (float)scaledWidth, sr.getScaledHeight() / (float)scaledHeight);

            this.bookWidth = (int) (DEFAULT_BOOK_WIDTH / fontSize);
            this.bookHeight = (int) (DEFAULT_BOOK_HEIGHT / fontSize);
            this.innerMargin = (int) (DEFAULT_INNER_MARGIN / fontSize);
            this.outerMargin = (int) (DEFAULT_OUTER_MARGIN / fontSize);
            this.verticalMargin = (int) (DEFAULT_VERTICAL_MARGIN / fontSize);
        }

        this.pageWidth = this.bookWidth / 2 - this.innerMargin - this.outerMargin;
        this.pageHeight = this.bookHeight - this.verticalMargin;
    }

    public float getScalingFactor()
    {
        return scalingFactor;
    }

    public boolean canGoBack()
    {
        return (currentPair > 0 || currentChapter > 0);
    }

    public boolean canGoNextPage()
    {
        return (currentPair + 1 < book.getChapter(currentChapter).pagePairs || currentChapter + 1 < book.chapterCount());
    }

    public boolean canGoPrevPage()
    {
        return (currentPair > 0 || currentChapter > 0);
    }

    public boolean canGoNextChapter()
    {
        return (currentChapter + 1 < book.chapterCount());
    }

    public boolean canGoPrevChapter()
    {
        return (currentChapter > 0);
    }

    public void navigateTo(final BookDocument.PageRef target)
    {
        pushHistory();

        target.resolve();
        currentChapter = Math.max(0, Math.min(book.chapterCount() - 1, target.chapter));
        currentPair = Math.max(0, Math.min(book.getChapter(currentChapter).pagePairs - 1, target.page / 2));
    }

    public void nextPage()
    {
        if (currentPair + 1 < book.getChapter(currentChapter).pagePairs)
        {
            pushHistory();
            currentPair++;
        }
        else if (currentChapter + 1 < book.chapterCount())
        {
            pushHistory();
            currentPair = 0;
            currentChapter++;
        }
    }

    public void prevPage()
    {
        if (currentPair > 0)
        {
            pushHistory();
            currentPair--;
        }
        else if (currentChapter > 0)
        {
            pushHistory();
            currentChapter--;
            currentPair = book.getChapter(currentChapter).pairCount() - 1;
        }
    }

    public void nextChapter()
    {
        if (currentChapter + 1 < book.chapterCount())
        {
            pushHistory();
            currentPair = 0;
            currentChapter++;
        }
    }

    public void prevChapter()
    {
        if (currentChapter > 0)
        {
            pushHistory();
            currentPair = 0;
            currentChapter--;
        }
    }

    public void navigateBack()
    {
        if (history.size() > 0)
        {
            BookDocument.PageRef target = history.pop();
            target.resolve();
            currentChapter = target.chapter;
            currentPair = target.page / 2;
        }
        else
        {
            currentChapter = 0;
            currentPair = 0;
        }
    }

    private void pushHistory()
    {
        history.push(book.new PageRef(currentChapter, currentPair * 2));
    }

    private int getSplitWidth(FontRenderer fontRenderer, String s)
    {
        int height = fontRenderer.splitStringWidth(s, pageWidth);
        return height > fontRenderer.FONT_HEIGHT ? pageWidth : fontRenderer.getStringWidth(s);
    }

    public int addStringWrapping(int left, int top, String s, int color, int align)
    {
        FontRenderer fontRenderer = gui.getFontRenderer();

        if (align == 1)
        {
            left += (pageWidth - getSplitWidth(fontRenderer, s)) / 2;
        }
        else if (align == 2)
        {
            left += pageWidth - getSplitWidth(fontRenderer, s);
        }

        fontRenderer.drawSplitString(s, left, top, pageWidth, color);
        return fontRenderer.splitStringWidth(s, pageWidth);
    }

    public boolean mouseClicked(int mouseButton)
    {
        Minecraft mc = Minecraft.getMinecraft();
        int dw = hasScale ? scaledWidth : gui.width;
        int dh = hasScale ? scaledHeight : gui.height;
        int mouseX = Mouse.getX() * dw / mc.displayWidth;
        int mouseY = dh - Mouse.getY() * dh / mc.displayHeight;

        if (mouseButton == 0)
        {
            BookDocument.ChapterData ch = book.getChapter(currentChapter);
            BookDocument.PageData pg = ch.pages.get(currentPair * 2);
            if (mouseClickPage(mouseX, mouseY, pg))
                return true;

            if (currentPair * 2 + 1 < ch.pages.size())
            {
                pg = ch.pages.get(currentPair * 2 + 1);
                if (mouseClickPage(mouseX, mouseY, pg))
                    return true;
            }
        }

        return false;
    }

    private boolean mouseClickPage(int mX, int mY, BookDocument.PageData pg)
    {
        for (BookDocument.IPageElement e : pg.elements)
        {
            if (e instanceof BookDocument.IClickablePageElement)
            {
                BookDocument.IClickablePageElement l = (BookDocument.IClickablePageElement) e;
                Rectangle b = l.getBounds();
                if (mX >= b.getX() && mX <= (b.getX() + b.getWidth()) &&
                        mY >= b.getY() && mY <= (b.getY() + b.getHeight()))
                {
                    l.click(this);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean mouseHover(int mouseX, int mouseY)
    {
        BookDocument.ChapterData ch = book.getChapter(currentChapter);
        BookDocument.PageData pg = ch.pages.get(currentPair * 2);
        if (mouseHoverPage(mouseX, mouseY, pg))
            return true;

        if (currentPair * 2 + 1 < ch.pages.size())
        {
            pg = ch.pages.get(currentPair * 2 + 1);
            if (mouseHoverPage(mouseX, mouseY, pg))
                return true;
        }

        return false;
    }

    private boolean mouseHoverPage(int mouseX, int mouseY, BookDocument.PageData pg)
    {
        Minecraft mc = Minecraft.getMinecraft();
        int dw = hasScale ? scaledWidth : gui.width;
        int dh = hasScale ? scaledHeight : gui.height;
        int mX = Mouse.getX() * dw / mc.displayWidth;
        int mY = dh - Mouse.getY() * dh / mc.displayHeight;

        for (BookDocument.IPageElement e : pg.elements)
        {
            if (e instanceof BookDocument.IHoverPageElement)
            {
                BookDocument.IHoverPageElement l = (BookDocument.IHoverPageElement) e;
                Rectangle b = l.getBounds();
                if (mX >= b.getX() && mX <= (b.getX() + b.getWidth()) &&
                        mY >= b.getY() && mY <= (b.getY() + b.getHeight()))
                {
                    l.mouseOver(this, mouseX, mouseY);
                    return true;
                }
            }
        }
        return false;
    }

    public void drawCurrentPages()
    {
        int guiWidth = gui.width;
        int guiHeight = gui.height;

        if (hasScale)
        {
            GlStateManager.pushMatrix();
            GlStateManager.scale(scalingFactor, scalingFactor, scalingFactor);

            guiWidth = scaledWidth;
            guiHeight = scaledHeight;
        }

        int left = guiWidth / 2 - pageWidth - innerMargin;
        int right = guiWidth / 2 + innerMargin;
        int top = (guiHeight - pageHeight) / 2 - 9;
        int bottom = top + pageHeight - 3;

        drawPage(left, top, currentPair * 2);
        drawPage(right, top, currentPair * 2 + 1);

        String cnt = "" + ((book.getChapter(currentChapter).startPair + currentPair) * 2 + 1) + "/" + (book.getTotalPairCount() * 2);
        addStringWrapping(left, bottom, cnt, 0xFF000000, 1);

        if (hasScale)
        {
            GlStateManager.popMatrix();
        }
    }

    private void drawPage(int left, int top, int page)
    {
        BookDocument.ChapterData ch = book.getChapter(currentChapter);
        if (page >= ch.pages.size())
            return;

        BookDocument.PageData pg = ch.pages.get(page);

        for (BookDocument.IPageElement e : pg.elements)
        {
            top += e.apply(this, left, top);
        }
    }

    public BookDocument getBook()
    {
        return book;
    }

    public int getPageWidth()
    {
        return pageWidth;
    }

    public int getPageHeight()
    {
        return pageHeight;
    }

    public void drawItemStack(int left, int top, ItemStack stack, int color)
    {
        StackRenderingHelper.renderItemStack(gui.getMesher(), gui.getRenderEngine(), left, top, stack, color);
    }

    public void drawImage(ResourceLocation loc, int x, int y, int tx, int ty, int w, int h, int tw, int th)
    {
        if (w == 0 || h == 0)
        {
            TextureAtlasSprite tas = mc.getTextureMapBlocks().getAtlasSprite(loc.toString());
            if (w == 0) w = tas.getIconWidth();
            if (h == 0) h = tas.getIconHeight();
        }

        int sw = tw != 0 ? tw : w;
        int sh = th != 0 ? th : h;

        ResourceLocation locExpanded = new ResourceLocation(loc.getResourceDomain(), "textures/" + loc.getResourcePath() + ".png");
        gui.getRenderEngine().bindTexture(locExpanded);

        GlStateManager.enableRescaleNormal();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        Gui.drawModalRectWithCustomSizedTexture(x, y, tx, ty, w, h, sw, sh);
    }

    public Rectangle getStringBounds(NavigationInfo nav, String text, int left, int top)
    {
        FontRenderer fontRenderer = nav.gui.getFontRenderer();

        int height = fontRenderer.splitStringWidth(text, nav.getPageWidth());
        int width = height > fontRenderer.FONT_HEIGHT ? nav.getPageWidth() : fontRenderer.getStringWidth(text);
        return new Rectangle(left, top, width, height);
    }

    public void drawTooltip(ItemStack stack, int x, int y)
    {
        gui.drawTooltip(stack, x, y);
    }
}