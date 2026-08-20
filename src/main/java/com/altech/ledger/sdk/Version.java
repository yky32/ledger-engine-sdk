package com.altech.ledger.sdk;

/**
 * Semantic version helper for SDK ↔ engine compatibility.
 */
public final class Version {
    private final int major;
    private final int minor;
    private final int patch;

    public Version(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public static Version parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("version blank");
        }
        String v = raw.trim();
        if (v.startsWith("v") || v.startsWith("V")) {
            v = v.substring(1);
        }
        // strip -SNAPSHOT / +build
        int cut = v.indexOf('-');
        if (cut > 0) {
            v = v.substring(0, cut);
        }
        cut = v.indexOf('+');
        if (cut > 0) {
            v = v.substring(0, cut);
        }
        String[] p = v.split("\\.");
        int maj = Integer.parseInt(p[0]);
        int min = p.length > 1 ? Integer.parseInt(p[1]) : 0;
        int pat = p.length > 2 ? Integer.parseInt(p[2].replaceAll("[^0-9].*", "")) : 0;
        return new Version(maj, min, pat);
    }

    /** this >= other */
    public boolean isAtLeast(Version other) {
        if (major != other.major) {
            return major > other.major;
        }
        if (minor != other.minor) {
            return minor > other.minor;
        }
        return patch >= other.patch;
    }

    public int major() { return major; }
    public int minor() { return minor; }
    public int patch() { return patch; }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
