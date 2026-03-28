package org.convoy.phone.util;

import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
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
}
