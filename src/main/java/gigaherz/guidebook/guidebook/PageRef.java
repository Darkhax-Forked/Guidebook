package gigaherz.guidebook.guidebook;

import com.google.common.base.Strings;
import com.google.common.primitives.Ints;
import gigaherz.guidebook.GuidebookMod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PageRef
{
    public int chapter;
    public int page;

    public boolean resolvedNames = false;
    public String chapterName;
    public String pageName;

    public PageRef(int chapter, int page)
    {
        this.chapter = chapter;
        this.page = page;
        resolvedNames = true;
    }

    public PageRef(String chapter, @Nullable String page)
    {
        this.chapterName = chapter;
        this.pageName = page;
    }

    /**
     * @param bookDocument the book which contains the referenced page
     * @return <code>false</code> if the {@link PageRef} has no valid target
     */
    public boolean resolve(BookDocument bookDocument)
    {
        if (!resolvedNames)
        {
            try{
                if (!Strings.isNullOrEmpty(chapterName))
                {
                    Integer ch = Ints.tryParse(chapterName);
                    if (ch != null)
                    {
                        chapter = ch;
                    }
                    else
                    {
                        chapter = bookDocument.chaptersByName.get(chapterName);
                    }

                    if (!Strings.isNullOrEmpty(pageName))
                    {
                        Integer pg = Ints.tryParse(pageName);
                        if (pg != null)
                        {
                            page = pg;
                        }
                        else
                        {
                            page = bookDocument.chapters.get(chapter).pagesByName.get(pageName);
                        }
                    }
                }
                else if (!Strings.isNullOrEmpty(pageName))
                {
                    PageRef temp = bookDocument.pagesByName.get(pageName);
                    temp.resolve(bookDocument);
                    chapter = temp.chapter;
                    page = temp.page;
                }
                else
                {
                    //throw error if neither field is defined
                    throw new InvalidPageRefException("Invalid format: missing page and chapter");
                }
            }catch (Exception e){ //catch error to prevent crash
                //try to parse the page ref into a string: <chapter>:<page>
                String ref_string=(Strings.isNullOrEmpty(chapterName)?"<none>":(chapterName))+
                        ":" + (Strings.isNullOrEmpty(pageName)?"<none>":(pageName));
                //log error
                GuidebookMod.logger.error(
                        String.format(
                                "Invalid page reference: \"%s\" in book \"%s\" caused by: %s",
                                ref_string,
                                bookDocument.getBookName(),
                                e.toString()
                        )
                );
                return false; // =page ref has no valid target
            }
        }
        return true;
    }

    public PageRef copy()
    {
        return new PageRef(chapter, page);
    }

    /**
     * Thrown by {@link PageRef#resolve(BookDocument)} in any case that normally wouldn't cause an exception
     * but still signifies that the {@link PageRef} has no valid target and is therefore invalid.
     */
    public static class InvalidPageRefException extends Exception{
        public InvalidPageRefException(String s) {
            super(s);
        }
    }

    /**
     * Parses a String into a {@link PageRef}.
     * @param refString the string to be parsed
     */
    public static PageRef fromString(@Nonnull String refString){
        if (refString.indexOf(':') >= 0)
        {
            String[] parts = refString.split(":");
            return new PageRef(parts[0], parts[1]);
        }
        else
        {
            return new PageRef(refString, null);
        }
    }
}
