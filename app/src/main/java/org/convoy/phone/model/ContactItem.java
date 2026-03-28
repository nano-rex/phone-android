package org.convoy.phone.model;

import android.net.Uri;

public class ContactItem {
    public final long contactId;
    public final String name;
    public final String number;
    public final Uri contactUri;
    public final boolean favorite;

    public ContactItem(long contactId, String name, String number, Uri contactUri, boolean favorite) {
        this.contactId = contactId;
        this.name = name;
        this.number = number;
        this.contactUri = contactUri;
        this.favorite = favorite;
    }

    @Override
    public String toString() {
        return (favorite ? "★ " : "") + name + "\n" + number;
    }
}
