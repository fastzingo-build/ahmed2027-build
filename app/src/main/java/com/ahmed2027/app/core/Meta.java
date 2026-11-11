package com.ahmed2027.app.core;

import java.util.LinkedHashMap;
import java.util.Map;

/** Tiny "key=value;key=value" store for the items.meta_json column (values must not contain ; or =). Pure. */
public final class Meta {
    private final Map<String, String> m = new LinkedHashMap<>();

    public static Meta parse(String s) {
        Meta r = new Meta();
        if (s == null || s.isEmpty()) return r;
        for (String part : s.split(";")) {
            int i = part.indexOf('=');
            if (i > 0) r.m.put(part.substring(0, i), part.substring(i + 1));
        }
        return r;
    }

    public String get(String k) { return m.get(k); }
    public String get(String k, String def) { String v = m.get(k); return v == null ? def : v; }
    public boolean has(String k) { return m.containsKey(k); }
    public boolean flag(String k) { return "1".equals(m.get(k)); }
    public long getLong(String k, long def) {
        try { return m.containsKey(k) ? Long.parseLong(m.get(k)) : def; } catch (NumberFormatException e) { return def; }
    }

    public Meta set(String k, String v) {
        if (k == null || k.isEmpty() || k.contains(";") || k.contains("=")) return this;
        if (v == null) m.remove(k); else m.put(k, v.replace(";", ",").replace("=", "-"));
        return this;
    }
    public Meta set(String k, long v) { return set(k, String.valueOf(v)); }
    public Meta setFlag(String k, boolean on) { return set(k, on ? "1" : null); }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : m.entrySet()) {
            if (sb.length() > 0) sb.append(';');
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }
}
