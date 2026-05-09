package com.forum.common.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlUtilTest {

    @Test
    void cleanRichTextKeepsSafeFormatting() {
        String html = "<h2>Title</h2><p>Hello <strong>world</strong></p>"
                + "<ul><li>One</li></ul><pre><code>const a = 1;</code></pre>"
                + "<a href=\"https://example.com\" target=\"_blank\">link</a>"
                + "<img src=\"https://example.com/a.png\" alt=\"a\">";

        String cleaned = HtmlUtil.cleanRichText(html);

        assertThat(cleaned).contains("<h2>Title</h2>");
        assertThat(cleaned).contains("<p>Hello <strong>world</strong></p>");
        assertThat(cleaned).contains("<ul><li>One</li></ul>");
        assertThat(cleaned).contains("<pre><code>const a = 1;</code></pre>");
        assertThat(cleaned).contains("href=\"https://example.com\"");
        assertThat(cleaned).contains("rel=\"noopener noreferrer\"");
        assertThat(cleaned).contains("src=\"https://example.com/a.png\"");
    }

    @Test
    void cleanRichTextRemovesXssVectors() {
        String html = "<p onclick=\"alert(1)\">ok</p>"
                + "<script>alert(1)</script>"
                + "<img src=\"javascript:alert(1)\" onerror=\"alert(1)\">"
                + "<a href=\"javascript:alert(1)\">bad</a>"
                + "<a href=\"ftp://example.com/file\">ftp</a>";

        String cleaned = HtmlUtil.cleanRichText(html);

        assertThat(cleaned).contains("<p>ok</p>");
        assertThat(cleaned).doesNotContain("script");
        assertThat(cleaned).doesNotContain("onclick");
        assertThat(cleaned).doesNotContain("onerror");
        assertThat(cleaned).doesNotContain("javascript:");
        assertThat(cleaned).doesNotContain("ftp://");
    }

    @Test
    void cleanReturnsPlainTextForComments() {
        assertThat(HtmlUtil.clean("<p>Hello <strong>world</strong></p>"))
                .isEqualTo("Hello world");
    }
}
