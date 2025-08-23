package com.fto.api.topflaky.dto;


import java.time.Instant;


public class TopFlakyItem {
    public String testCaseId;
    public String className;
    public String testName;
    public int runs;
    public int fails;
    public int passes;
    public double failRate; // 0..1
    public int flips; // PASS/FAIL geçiş sayısı
    public Instant lastSeenAt;
    public double score; // 0..1 (skor v0)
}