package org.convoy.phone.model;

import android.net.Uri;

public class ContactItem {
    public final String name;
    public final String number;
    public final Uri contactUri;

    public ContactItem(String name, String number, Uri contactUri) {
        this.name = name;
        this.number = number;
        this.contactUri = contactUri;
    }

    @Override
    public String toString() {
        return name + "\n" + number;
    }
}
