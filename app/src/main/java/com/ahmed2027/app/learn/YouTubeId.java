package com.ahmed2027.app.learn;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

/** Pure YouTube link handling: only the official embed is ever used. Shorts links are refused (no Shorts). */
public final class YouTubeId {
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]{11}");
    private YouTubeId() {}

    /** @return the 11-char video id, or null when the text is not a normal YouTube video link/id. */
    public static String parse(String s) {
        if (s == null) return null;
        s = s.trim();
        if (ID.matcher(s).matches()) return s;
        try {
            URI u = new URI(s.startsWith("http") ? s : "https://" + s);
            String host = u.getHost() == null ? "" : u.getHost().toLowerCase(Locale.ROOT);
            if (host.startsWith("www.")) host = host.substring(4);
            if (host.startsWith("m.")) host = host.substring(2);
            String path = u.getPath() == null ? "" : u.getPath();
            String id = null;
            if (host.equals("youtu.be")) {
                id = path.startsWith("/") ? path.substring(1) : path;
            } else if (host.equals("youtube.com") || host.equals("youtube-nocookie.com")) {
                if (path.startsWith("/shorts/")) return null;
                if (path.equals("/watch")) id = query(u.getRawQuery(), "v");
                else if (path.startsWith("/embed/")) id = path.substring(7);
                else if (path.startsWith("/live/")) id = path.substring(6);
            }
            if (id != null && id.length() >= 11) id = id.substring(0, 11);
            return id != null && ID.matcher(id).matches() ? id : null;
        } catch (java.net.URISyntaxException e) {
            return null;
        }
    }

    private static String query(String q, String key) {
        if (q == null) return null;
        for (String p : q.split("&")) {
            int i = p.indexOf('=');
            if (i > 0 && p.substring(0, i).equals(key)) return p.substring(i + 1);
        }
        return null;
    }

    /** Official privacy-enhanced embed. rel=0: related videos come from the same channel only. */
    public static String embedUrl(String id) {
        return "https://www.youtube-nocookie.com/embed/" + id + "?rel=0&modestbranding=1&playsinline=1";
    }

    public static String embedHtml(String id) {
        return "<!doctype html><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<style>html,body{margin:0;background:#000;height:100%}iframe{border:0;width:100%;height:100%}</style></head><body>"
                + "<iframe src=\"" + embedUrl(id) + "\" allow=\"encrypted-media; picture-in-picture\" allowfullscreen></iframe></body></html>";
    }

    /** True for the only host the in-app player may navigate to. */
    public static boolean isAllowedHost(String host) { return "www.youtube-nocookie.com".equalsIgnoreCase(host); }
}
