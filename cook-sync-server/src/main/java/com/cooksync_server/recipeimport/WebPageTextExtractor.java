package com.cooksync_server.recipeimport;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

/**
 * Fetches a web page and reduces it to its main readable text — nav bars, scripts, styles, and
 * comments stripped out — before it's handed to {@link RecipeExtractionProvider}. Recipe pages
 * are typically full of ads/related-recipe rails/comment sections that would otherwise dilute the
 * extraction prompt with irrelevant text.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
@Component
public class WebPageTextExtractor {

    private static final int TIMEOUT_MS = 15_000;
    /** Recipe pages can be long; caps the text handed to the extraction prompt. */
    private static final int MAX_TEXT_LENGTH = 20_000;

    /**
     * Identifies this app to the source server, since some sites block requests with no/a
     * generic {@code User-Agent} outright.
     */
    private static final String USER_AGENT = "CookSyncRecipeImportBot/1.0 (+https://cooksync.app)";

    /**
     * Fetches {@code url} and returns its main readable text, plus its own cover image if it
     * declares one — a nice-to-have default for the imported recipe's photo, not something the
     * extraction model is asked to guess at.
     *
     * @param url the recipe page's URL
     * @return the page's cleaned text and og:image URL (if any)
     * @throws IOException if the page can't be fetched (network error, non-2xx status, timeout)
     */
    public PageContent fetchReadableContent(String url) throws IOException {
        Document document = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .get();

        String ogImage = document.select("meta[property=og:image]").attr("content");

        document.select("script, style, nav, footer, header, noscript, iframe, svg").remove();

        String text = document.body() != null ? document.body().text() : document.text();
        String truncated = text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH) : text;

        return new PageContent(truncated, ogImage.isBlank() ? null : ogImage);
    }

    /**
     * A fetched page's cleaned text plus its own declared cover image, if any.
     *
     * @param text the page's main readable text
     * @param imageUrl the page's {@code og:image} URL, or {@code null} if it doesn't declare one
     */
    public record PageContent(String text, String imageUrl) {
    }
}
