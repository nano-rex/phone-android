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
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

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

    public static boolean copyRecordingToFolder(Context context, File sourceFile, String displayName) {
        if (sourceFile == null || !sourceFile.exists() || sourceFile.length() == 0L) {
            return false;
        }
        Uri documentUri = null;
        try {
            Uri treeUri = AppSettings.getRecordingsTreeUri(context);
            if (treeUri == null) {
                return false;
            }
            String parentId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentId);
            documentUri = DocumentsContract.createDocument(context.getContentResolver(), parentUri, "audio/mp4", displayName);
            if (documentUri == null) {
                return false;
            }

            try (InputStream in = new FileInputStream(sourceFile);
                 OutputStream out = context.getContentResolver().openOutputStream(documentUri, "w")) {
                if (out == null) {
                    return false;
                }
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
                return true;
            }
        } catch (Exception e) {
            if (documentUri != null) {
                try {
                    DocumentsContract.deleteDocument(context.getContentResolver(), documentUri);
                } catch (Exception ignored) {
                }
            }
            return false;
        }
    }
}
