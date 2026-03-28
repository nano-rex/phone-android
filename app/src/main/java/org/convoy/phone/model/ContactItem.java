package org.convoy.phone.model;

public class ContactItem {
    public final String name;
    public final String number;

    public ContactItem(String name, String number) {
        this.name = name;
        this.number = number;
    }

    @Override
    public String toString() {
        return name + "\n" + number;
    }
}
