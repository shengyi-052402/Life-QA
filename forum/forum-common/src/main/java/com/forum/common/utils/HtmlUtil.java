package com.forum.common.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * HTML utility methods for plain-text extraction and safe rich text storage.
 */
public class HtmlUtil {
    private static final Safelist RICH_TEXT_SAFELIST = Safelist.relaxed()
            .addTags("pre", "code", "span", "hr", "s")
            .addAttributes("a", "target", "rel")
            .addAttributes("img", "width", "height", "alt")
            .addEnforcedAttribute("a", "rel", "noopener noreferrer")
            .removeProtocols("a", "href", "ftp")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https");

    private static final Document.OutputSettings OUTPUT_SETTINGS = new Document.OutputSettings()
            .prettyPrint(false);

    /**
     * Remove HTML tags and return plain text.
     */
    public static String removeHtmlTags(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        return Jsoup.parse(html).text();
    }

    /**
     * Build a summary from the plain-text version of HTML content.
     */
    public static String getSummary(String html, int length) {
        String text = removeHtmlTags(html);
        if (text.length() <= length) {
            return text;
        }
        return text.substring(0, length) + "...";
    }

    /**
     * Remove all HTML and keep only text. Suitable for plain-text fields.
     */
    public static String clean(String html) {
        return removeHtmlTags(html);
    }

    /**
     * Sanitize rich text before storing or rendering it with v-html.
     */
    public static String cleanRichText(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        return Jsoup.clean(html, "", RICH_TEXT_SAFELIST, OUTPUT_SETTINGS);
    }
}
