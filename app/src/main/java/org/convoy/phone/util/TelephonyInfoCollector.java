package org.convoy.phone.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.CellIdentity;
import android.telephony.CellInfo;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import java.util.List;

public final class TelephonyInfoCollector {
    private TelephonyInfoCollector() {
    }

    public static String collect(Context context) {
        StringBuilder builder = new StringBuilder();
        append(builder, "Network operator", safeNetworkOperatorName(context));
        append(builder, "SIM operator", safeSimOperatorName(context));
        append(builder, "Network country", safeNetworkCountryIso(context));
        append(builder, "SIM country", safeSimCountryIso(context));
        append(builder, "Network type", safeNetworkType(context));
        append(builder, "Roaming", safeRoaming(context));
        append(builder, "Service state", safeServiceState(context));
        append(builder, "Active subscription", safeSubscriptionInfo(context));
        append(builder, "Cell info", safeCellInfo(context));
        return builder.toString().trim();
    }

    private static void append(StringBuilder builder, String label, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(label).append(": ").append(value);
    }

    private static TelephonyManager telephonyManager(Context context) {
        return context.getSystemService(TelephonyManager.class);
    }

    private static boolean hasPhoneState(Context context) {
        return context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean hasFineLocation(Context context) {
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private static String safeNetworkOperatorName(Context context) {
        try {
            TelephonyManager manager = telephonyManager(context);
            return manager == null ? "Unavailable" : emptyToUnavailable(manager.getNetworkOperatorName());
        } catch (Exception e) {
            return "Unavailable";
        }
    }

    private static String safeSimOperatorName(Context context) {
        try {
            TelephonyManager manager = telephonyManager(context);
            return manager == null ? "Unavailable" : emptyToUnavailable(manager.getSimOperatorName());
        } catch (Exception e) {
            return "Unavailable";
        }
    }

    private static String safeNetworkCountryIso(Context context) {
        try {
            TelephonyManager manager = telephonyManager(context);
            return manager == null ? "Unavailable" : emptyToUnavailable(manager.getNetworkCountryIso());
        } catch (Exception e) {
            return "Unavailable";
        }
    }

    private static String safeSimCountryIso(Context context) {
        try {
            TelephonyManager manager = telephonyManager(context);
            return manager == null ? "Unavailable" : emptyToUnavailable(manager.getSimCountryIso());
        } catch (Exception e) {
            return "Unavailable";
        }
    }

    private static String safeNetworkType(Context context) {
        if (!hasPhoneState(context)) {
            return "Grant phone state permission";
        }
        try {
            TelephonyManager manager = telephonyManager(context);
            if (manager == null) {
                return "Unavailable";
            }
            return networkTypeLabel(manager.getDataNetworkType());
        } catch (Exception e) {
            return "Unavailable";
        }
    }

    private static String safeRoaming(Context context) {
        if (!hasPhoneState(context)) {
            return "Grant phone state permission";
        }
        try {
            TelephonyManager manager = telephonyManager(context);
            if (manager == null) {
                return "Unavailable";
            }
            return manager.isNetworkRoaming() ? "Yes" : "No";
        } catch (Exception e) {
            return "Unavailable";
        }
    }

    private static String safeServiceState(Context context) {
        if (!hasPhoneState(context)) {
            return "Grant phone state permission";
        }
        try {
            TelephonyManager manager = telephonyManager(context);
            ServiceState state = manager == null ? null : manager.getServiceState();
            if (state == null) {
                return "Unavailable";
            }
            switch (state.getState()) {
                case ServiceState.STATE_IN_SERVICE:
                    return "In service";
                case ServiceState.STATE_OUT_OF_SERVICE:
                    return "Out of service";
                case ServiceState.STATE_EMERGENCY_ONLY:
                    return "Emergency only";
                case ServiceState.STATE_POWER_OFF:
                    return "Power off";
                default:
                    return "Unknown";
            }
        } catch (Exception e) {
            return "Unavailable";
        }
    }

    private static String safeSubscriptionInfo(Context context) {
        if (!hasPhoneState(context)) {
            return "Grant phone state permission";
        }
        try {
            SubscriptionManager manager = context.getSystemService(SubscriptionManager.class);
            List<SubscriptionInfo> list = manager == null ? null : manager.getActiveSubscriptionInfoList();
            if (list == null || list.isEmpty()) {
                return "Unavailable";
            }
            SubscriptionInfo info = list.get(0);
            String carrier = info.getCarrierName() == null ? "" : info.getCarrierName().toString();
            String display = info.getDisplayName() == null ? "" : info.getDisplayName().toString();
            String joined = (carrier + (display.isEmpty() ? "" : " / " + display)).trim();
            return joined.isEmpty() ? "Available" : joined;
        } catch (Exception e) {
            return "Unavailable";
        }
    }

    private static String safeCellInfo(Context context) {
        if (!hasFineLocation(context)) {
            return "Grant location permission";
        }
        try {
            TelephonyManager manager = telephonyManager(context);
            List<CellInfo> cells = manager == null ? null : manager.getAllCellInfo();
            if (cells == null || cells.isEmpty()) {
                return "Unavailable";
            }
            CellInfo cell = cells.get(0);
            CellIdentity identity = cell.getCellIdentity();
            if (identity == null) {
                return "Available";
            }
            return identity.toString();
        } catch (Exception e) {
            return "Unavailable";
        }
    }

    private static String emptyToUnavailable(String value) {
        return value == null || value.trim().isEmpty() ? "Unavailable" : value.trim();
    }

    private static String networkTypeLabel(int type) {
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_GPRS: return "GPRS";
            case TelephonyManager.NETWORK_TYPE_EDGE: return "EDGE";
            case TelephonyManager.NETWORK_TYPE_UMTS: return "UMTS";
            case TelephonyManager.NETWORK_TYPE_HSDPA: return "HSDPA";
            case TelephonyManager.NETWORK_TYPE_HSUPA: return "HSUPA";
            case TelephonyManager.NETWORK_TYPE_HSPA: return "HSPA";
            case TelephonyManager.NETWORK_TYPE_LTE: return "LTE";
            case TelephonyManager.NETWORK_TYPE_NR: return "5G NR";
            case TelephonyManager.NETWORK_TYPE_CDMA: return "CDMA";
            case TelephonyManager.NETWORK_TYPE_EVDO_0: return "EVDO 0";
            case TelephonyManager.NETWORK_TYPE_EVDO_A: return "EVDO A";
            case TelephonyManager.NETWORK_TYPE_1xRTT: return "1xRTT";
            case TelephonyManager.NETWORK_TYPE_EHRPD: return "eHRPD";
            case TelephonyManager.NETWORK_TYPE_HSPAP: return "HSPA+";
            case TelephonyManager.NETWORK_TYPE_GSM: return "GSM";
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA: return "TD-SCDMA";
            case TelephonyManager.NETWORK_TYPE_IWLAN: return "IWLAN";
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
            default:
                return "Unknown";
        }
    }
}
