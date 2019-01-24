package de.tudarmstadt.ukp.inception.app.ui.externalsearch.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;

public class Utilities
{
    public static String cleanHighlight(String aHighlight) {
        Whitelist wl = new Whitelist();
        wl.addTags("em");
        Document dirty = Jsoup.parseBodyFragment(aHighlight, "");
        Cleaner cleaner = new Cleaner(wl);
        Document clean = cleaner.clean(dirty);
        clean.select("em").tagName("mark");

        return clean.body().html();
    }
}
