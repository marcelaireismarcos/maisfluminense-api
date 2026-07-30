package maisfluminense.vikkynsnorth.noticias.util;

import android.net.Uri;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolve links do Google News RSS para a URL original do artigo quando ela
 * está embutida no próprio path codificado do link.
 */
public final class GoogleNewsUrlResolver {

    private static final Pattern URL_PATTERN =
            Pattern.compile("https?://[^\\u0000-\\u001F\\s\"'<>]+");

    private GoogleNewsUrlResolver() {}

    public static String resolve(String url) {
        if (url == null || url.isEmpty() || !isGoogleNewsUrl(url)) {
            return url;
        }

        String directUrl = tryExtractFromEncodedPath(url);
        return directUrl != null ? directUrl : url;
    }

    private static boolean isGoogleNewsUrl(String url) {
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            return host != null && host.contains("news.google.com");
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String tryExtractFromEncodedPath(String url) {
        try {
            Uri uri = Uri.parse(url);
            List<String> segments = uri.getPathSegments();
            int articlesIndex = segments.indexOf("articles");
            if (articlesIndex < 0 || articlesIndex + 1 >= segments.size()) {
                return null;
            }

            String encodedSegment = segments.get(articlesIndex + 1);
            if (encodedSegment == null || encodedSegment.isEmpty()) {
                return null;
            }

            String normalized = normalizeBase64Url(encodedSegment);
            byte[] decodedBytes = Base64.decode(normalized, Base64.URL_SAFE | Base64.NO_WRAP);
            String decodedText = new String(decodedBytes, StandardCharsets.UTF_8);

            Matcher matcher = URL_PATTERN.matcher(decodedText);
            while (matcher.find()) {
                String candidate = cleanupUrlCandidate(matcher.group());
                if (looksLikePublisherArticle(candidate)) {
                    return candidate;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String normalizeBase64Url(String encoded) {
        String normalized = encoded.replace('-', '+').replace('_', '/');
        int remainder = normalized.length() % 4;
        if (remainder == 2) {
            normalized += "==";
        } else if (remainder == 3) {
            normalized += "=";
        }
        return normalized;
    }

    private static String cleanupUrlCandidate(String candidate) {
        String cleaned = candidate;
        while (cleaned.endsWith(".")
                || cleaned.endsWith(",")
                || cleaned.endsWith(";")
                || cleaned.endsWith(")")
                || cleaned.endsWith("]")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned;
    }

    private static boolean looksLikePublisherArticle(String candidate) {
        return candidate != null
                && candidate.startsWith("http")
                && !candidate.contains("news.google.com")
                && !candidate.contains("google.com/search");
    }
}
