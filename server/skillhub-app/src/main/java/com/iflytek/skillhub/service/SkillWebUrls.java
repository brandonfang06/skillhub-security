package com.iflytek.skillhub.service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Public links use deployment configuration only, never request forwarding headers. */
@Component
public final class SkillWebUrls {
    private static final Pattern NON_ASCII = Pattern.compile("[^\\x00-\\x7f]+");
    private final String base;

    public SkillWebUrls(@Value("${skillhub.public.base-url:}") String configuredBase) {
        this.base = trustedBase(configuredBase);
    }

    public String detail(String namespace, String slug) {
        if (base == null || namespace == null || slug == null) return null;
        return base + "/space/" + segment(namespace) + "/" + segment(slug);
    }

    private static String trustedBase(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (!StandardCharsets.UTF_8.newEncoder().canEncode(trimmed)) return null;
        try {
            URI uri = URI.create(trimmed);
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getRawUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || uri.getPort() > 65535) return null;
            // URI.toASCIIString() applies NFC, which can change a deployment's path identity.
            return NON_ASCII.matcher(uri.toString())
                    .replaceAll(match -> URLEncoder.encode(match.group(), StandardCharsets.UTF_8))
                    .replaceAll("/+$", "");
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    private static String segment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
