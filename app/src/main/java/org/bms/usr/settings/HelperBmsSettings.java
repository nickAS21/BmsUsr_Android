package org.bms.usr.settings;

import static org.bms.usr.BmsUsrApp.getSharedPreferences;

import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class HelperBmsSettings {

    private static final String KEY_BMS_WIFI_MAP = "bmsWifiMap";

    public static Map<String, String> getBmsWifiMap() {
        Gson gson = new Gson();
        String json = getSharedPreferences().getString(KEY_BMS_WIFI_MAP, null);
        if (json == null) {
            return new HashMap<>();
        }
        Type type = new TypeToken<Map<String, String>>() {}.getType();
        return gson.fromJson(json, type);
    }

    /**
     * Example^
     * StarlinkDachaWifi -> 3e:90:1f:56:1d:28 -> IPv4: 192.168.1.51
     */
    public static void addOrUpdateBmsWifiEntry(String ssid, String bssid) {
        Map<String, String> bmsWifiMap = getBmsWifiMap();
        bmsWifiMap.put(ssid, bssid);
        saveBmsWifiMap(bmsWifiMap);
    }

    public static void resetBmsWifiMap() {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.remove(KEY_BMS_WIFI_MAP);
        editor.apply();
    }

    public static boolean removeBmsWifiEntry(String ssid) {
        Map<String, String> bmsWifiMap = getBmsWifiMap();
        boolean wasRemoved = bmsWifiMap.remove(ssid) != null;
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
