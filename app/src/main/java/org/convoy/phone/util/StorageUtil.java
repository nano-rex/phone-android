package org.convoy.phone.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import org.convoy.phone.model.RecordingItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class StorageUtil {
    private StorageUtil() {}

    public static List<RecordingItem> listRecordings(Context context) {
        ArrayList<RecordingItem> items = new ArrayList<>();
        Uri treeUri = AppSettings.getRecordingsTreeUri(context);
        if (treeUri == null) {
            return items;
        }
        ContentResolver resolver = context.getContentResolver();
        String docId = DocumentsContract.getTreeDocumentId(treeUri);
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId);
        try (Cursor cursor = resolver.query(childrenUri, new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, null)) {
            if (cursor == null) {
                return items;
            }
            while (cursor.moveToNext()) {
                String childId = cursor.getString(0);
                String name = cursor.getString(1);
                String mimeType = cursor.getString(2);
                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                    continue;
                }
                Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId);
                items.add(new RecordingItem(name, documentUri));
            }
        } catch (Exception ignored) {
        }
        return items;
    }

    public static Uri createRecordingDocument(Context context) {
        Uri treeUri = AppSettings.getRecordingsTreeUri(context);
        if (treeUri == null) {
            return null;
        }
        String name = "call_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".m4a";
        try {
            String parentId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentId);
            return DocumentsContract.createDocument(context.getContentResolver(), parentUri, "audio/mp4", name);
        } catch (Exception e) {
            return null;
        }
    }
}
