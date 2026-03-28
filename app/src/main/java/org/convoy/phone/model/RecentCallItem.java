package org.convoy.phone.model;

public class RecentCallItem {
    public final String name;
    public final String number;
    public final String details;

    public RecentCallItem(String name, String number, String details) {
        this.name = name;
        this.number = number;
        this.details = details;
    }

    @Override
    public String toString() {
        return name + "\n" + number + "\n" + details;
    }
}
