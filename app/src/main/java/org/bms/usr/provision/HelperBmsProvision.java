package org.bms.usr.provision;

import static org.bms.usr.BmsUsrApp.getSharedPreferences;

import android.content.SharedPreferences;

import java.util.Locale;

public class HelperBmsProvision {

    public static final int BYTE_MASK_TO_255 = 0xff;
    public static final int BYTE_SEPARATOR_0 = 0x0D;
    public static final int BYTE_SEPARATOR_1 = 0x0A;
    public static final String BROADCAST_IP = "255.255.255.255";
    public static final int PORT_DEF = 26000;
    public static final int TARGET_PORT_DEF = 49000;
    public static final int TIMEOUT = 5000; // TimeOut response in ms
    public static final String CURRENT_SSID_START_TEXT = "CURRENT_SSID_START";
    public static final String CHOSEN_SSID_TEXT = "CHOSEN_SSID";
    public static final String CHOSEN_BSSID_TEXT = "CHOSEN_BSSID";
    public static final String CHOSEN_ID_TEXT = "CHOSEN_ID";
    public static final String CHOSEN_IP_TEXT = "CHOSEN_IP";
    public static final String WIFI_FILTER_SSID_DEF = "USR-WIFI232-";
    public static final boolean WIFI_FILTER_ENABLED_DEF = true;
    private static final String KEY_WIFI_FILTER = "wifiFilterSsid";
    private static final String KEY_FILTER_ENABLED = "isFilterEnabled";
    public static final int TIME_OUT_WIFI_SEARCH = 2000;

//    public static final byte[] SEARCH_CODE = new byte[]{(byte) 0xFF, 0x00, 0x01, 0x01, 0x02}; // For tests

    public static void saveFilterSettings(String filterSsid, boolean isEnabled) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(KEY_WIFI_FILTER, filterSsid);
        editor.putBoolean(KEY_FILTER_ENABLED, isEnabled);
        editor.apply();
    }

    public static String getWifiFilterSsid() {
        return getSharedPreferences().getString(KEY_WIFI_FILTER, WIFI_FILTER_SSID_DEF);
    }

    public static boolean isFilterEnabled() {
        return getSharedPreferences().getBoolean(KEY_FILTER_ENABLED, WIFI_FILTER_ENABLED_DEF);
    }

    // Конвертація int → IP
    public static String intToIp(int ipAddress) {
        return String.format(Locale.ROOT, "%d.%d.%d.%d",
                (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff),
                (ipAddress >> 24 & 0xff));
    }
}
