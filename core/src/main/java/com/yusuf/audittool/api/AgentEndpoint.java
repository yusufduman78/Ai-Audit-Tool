package com.yusuf.audittool.api;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class AgentEndpoint {

    private final URI uri;
    private final Map<String, String> headers;

    public AgentEndpoint(URI uri) {
        this(uri, Map.of());
    }

    public AgentEndpoint(URI uri, Map<String, String> headers) {
        this.uri = validateUri(uri);
        this.headers = Map.copyOf(Objects.requireNonNull(headers, "Headers must not be null."));
    }

    public static AgentEndpoint of(String uri) {
        if (uri == null || uri.isBlank()) {
            throw new IllegalArgumentException("Agent endpoint must not be blank.");
        }
        return new AgentEndpoint(URI.create(uri.strip()));
    }

    public URI getUri() {
        return uri;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    private URI validateUri(URI value) {
        Objects.requireNonNull(value, "Agent endpoint must not be null.");
        String scheme = value.getScheme();
        if (!value.isAbsolute() || scheme == null) {
            throw new IllegalArgumentException("Agent endpoint must be an absolute HTTP URL.");
        }

        String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
        if (!"http".equals(normalizedScheme) && !"https".equals(normalizedScheme)) {
            throw new IllegalArgumentException("Agent endpoint must use HTTP or HTTPS.");
        }
        return value;
    }
}
