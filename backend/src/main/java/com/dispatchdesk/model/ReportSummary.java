package com.dispatchdesk.model;

import java.util.ArrayList;
import java.util.List;

/** Aggregated report metrics and per-area delivery breakdown. */
public class ReportSummary {
    public int totalOrders;
    public int deliveredOrders;
    public int failedOrders;
    public double successRate;
    public List<AreaStat> areaStats = new ArrayList<>();
}
