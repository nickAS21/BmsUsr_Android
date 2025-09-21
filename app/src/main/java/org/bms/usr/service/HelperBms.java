package org.bms.usr.service;

import static android.content.Context.MODE_PRIVATE;

import static org.bms.usr.BmsUsrApp.getSharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class HelperBms {

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
    public static final String WIFI_FILTER_SSID_DEF = "USR-WIFI232-";
    public static final boolean WIFI_FILTER_ENABLED_DEF = true;
    private static final String KEY_WIFI_FILTER = "wifiFilterSsid";
    private static final String KEY_FILTER_ENABLED = "isFilterEnabled";

    private static final String KEY_BMS_WIFI_MAP = "bmsWifiMap";

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

    public static Map<String, String> getBmsWifiMap() {
        Gson gson = new Gson();
        String json = getSharedPreferences().getString(KEY_BMS_WIFI_MAP, null);
        if (json == null) {
            return new HashMap<>();
        }
        Type type = new TypeToken<Map<String, String>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public static void addOrUpdateBmsWifiEntry(String bssid, String ssid) {
        Map<String, String> bmsWifiMap = getBmsWifiMap();
        bmsWifiMap.put(bssid, ssid);
        saveBmsWifiMap(bmsWifiMap);
    }

    public static void resetBmsWifiMap() {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.remove(KEY_BMS_WIFI_MAP);
        editor.apply();
    }

    public static boolean removeBmsWifiEntry(String bssid) {
        Map<String, String> bmsWifiMap = getBmsWifiMap();
        boolean wasRemoved = bmsWifiMap.remove(bssid) != null;
        if (wasRemoved) {
            saveBmsWifiMap(bmsWifiMap);
        }
        return wasRemoved;
    }

    private static void saveBmsWifiMap(Map<String, String> bmsWifiMap){
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        Gson gson = new Gson();
        String json = gson.toJson(bmsWifiMap);
        editor.putString(KEY_BMS_WIFI_MAP, json);
        editor.apply();
    }

}
