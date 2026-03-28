package org.convoy.phone.model;

import android.net.Uri;

public class RecordingItem {
    public final String name;
    public final Uri uri;

    public RecordingItem(String name, Uri uri) {
        this.name = name;
        this.uri = uri;
    }

    @Override
    public String toString() {
        return name;
    }
}
