package com.fto.api.topflaky.dto;


import java.util.List;


public class TopFlakyResponse {
    public List<TopFlakyItem> items;
    public int page;
    public int size;
    public boolean hasNext;
    public int windowDays;
    public int minRuns;
    public String project;
}