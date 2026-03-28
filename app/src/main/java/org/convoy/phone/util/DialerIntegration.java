package org.convoy.phone.util;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.os.Build;
import android.telecom.TelecomManager;

public final class DialerIntegration {
    public static final int REQ_DEFAULT_DIALER = 90;

    private DialerIntegration() {}

    public static boolean isDefaultDialer(Context context) {
        TelecomManager telecomManager = context.getSystemService(TelecomManager.class);
        if (telecomManager == null) {
            return false;
        }
        String packageName = telecomManager.getDefaultDialerPackage();
        return context.getPackageName().equals(packageName);
    }

    public static Intent createRoleRequestIntent(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = context.getSystemService(RoleManager.class);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER);
            }
        }
        TelecomManager telecomManager = context.getSystemService(TelecomManager.class);
        Intent intent = new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER);
        intent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.getPackageName());
        return intent;
    }

    public static boolean requestDefaultDialer(Activity activity) {
        if (isDefaultDialer(activity)) {
            return true;
        }

        Intent intent = createRoleRequestIntent(activity);
        if (intent != null && intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivityForResult(intent, REQ_DEFAULT_DIALER);
            return true;
        }

        Intent fallback = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
        if (fallback.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(fallback);
            return true;
        }

        return false;
    }
}
