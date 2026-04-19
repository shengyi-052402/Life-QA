package com.forum.common.utils;

import org.jsoup.Jsoup;

/**
 * HTML 工具类 - 清除HTML标签，提取纯文本
 * 主要用于 ES 搜索索引和帖子摘要生成
 */
public class HtmlUtil {

    /**
     * 去除HTML标签，返回纯文本
     */
    public static String removeHtmlTags(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        return Jsoup.parse(html).text();
    }

    /**
     * 截取摘要 (去除HTML标签后截取前N个字符)
     *
     * @param html   HTML内容
     * @param length 截取长度
     * @return 摘要文本
     */
    public static String getSummary(String html, int length) {
        String text = removeHtmlTags(html);
        if (text.length() <= length) {
            return text;
        }
        return text.substring(0, length) + "...";
    }

    /**
     * 清理恶意HTML标签，防止 XSS 攻击
     */
    public static String clean(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        // 使用 Jsoup 内置的 Safelist 进行安全过滤，移除所有的危险脚本
        return Jsoup.clean(html, org.jsoup.safety.Safelist.none());
    }
}
