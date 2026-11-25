package com.dispatchdesk.model;

/** Area-wise delivery performance statistics for analytics reports. */
public class AreaStat {
    public final String area;
    public final int total;
    public final int delivered;
    public final int failed;

    public AreaStat(String area, int total, int delivered, int failed) {
        this.area = area;
        this.total = total;
        this.delivered = delivered;
        this.failed = failed;
    }
}
