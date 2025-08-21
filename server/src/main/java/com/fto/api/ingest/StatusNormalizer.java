package com.fto.api.ingest;


import java.util.Locale;


public final class StatusNormalizer {
    private StatusNormalizer() {}

    /**
     * Ham statüyü trim + uppercase yapar ve kanonik değere çevirir.
     * Kabul edilen kanonik set: PASSED / FAILED / SKIPPED
     */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toUpperCase(Locale.ROOT);
        return switch (s) {
            case "PASS", "PASSED", "SUCCESS" -> "PASSED";
            case "FAIL", "FAILED", "FAILURE", "ERROR" -> "FAILED";
            case "SKIP", "SKIPPED", "IGNORE", "IGNORED" -> "SKIPPED";
            default -> s;
        };
    }
}