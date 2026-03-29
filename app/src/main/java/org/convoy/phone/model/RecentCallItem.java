package org.convoy.phone.model;

public class RecentCallItem {
    public final String name;
    public final String number;
    public final String details;
    public final long timestamp;

    public RecentCallItem(String name, String number, String details, long timestamp) {
        this.name = name;
        this.number = number;
        this.details = details;
        this.timestamp = timestamp;
    }

    public RecentCallItem(String name, String number, String details) {
        this(name, number, details, 0L);
    }

    @Override
    public String toString() {
        return name + "\n" + number + "\n" + details;
    }
}
