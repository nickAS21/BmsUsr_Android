package org.bms.usr.settings;

import static org.bms.usr.BmsUsrApp.getSharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class HelperBmsSettings {

    private static final String KEY_BMS_WIFI_MAP = "bmsWifiMap";
    public static final String IP_DEF_STA_PREFIX = "10.10.";        // 10.10.100+id.100
    public static final String IP_DEF_AP = "192.168.8.119";         // PC, AWS. other
    public static final String IP_DEF_WIFI_HOME_PREFIX = "192.168.8.";  // 92.168.8.100+id
    public static final int PORT_DEF_AP_BASE = 8890;               // +id
    public static final int PORT_DEF_STA_BASE = 18890;                   // +id

    public static Map<String, WiFiBmsEntity> getBmsWifiMap() {
        Gson gson = new Gson();
        String json = getSharedPreferences().getString(KEY_BMS_WIFI_MAP, null);
        if (json == null) {
            return new HashMap<>();
        }
        Type type = new TypeToken<Map<String, WiFiBmsEntity>>() {}.getType();
        return gson.fromJson(json, type);
    }


    /**
     * Example:
     * StarlinkDachaWifi -> 3e:90:1f:56:1d:28 -> IPv4: 192.168.1.51
     */
    public static void addOrUpdateBmsWifiEntry(int id, String ssid, String ssidBms, String bssid) {
        Map<String, WiFiBmsEntity> map = getBmsWifiMap();
        map.put(bssid, new WiFiBmsEntity(id, ssid, ssidBms, bssid));
        saveBmsWifiMap(map);
    }

    public static void resetBmsWifiMap() {
        saveBmsWifiMap(new HashMap<>());
    }

    public static boolean removeBmsWifiEntry(String bssid) {
        Map<String, WiFiBmsEntity> map = getBmsWifiMap();
        if (map.remove(bssid) != null) {
            saveBmsWifiMap(map);
            return true;
        }
        return false;
    }

    private static void saveBmsWifiMap(Map<String, WiFiBmsEntity> map) {
        Gson gson = new Gson();
        String json = gson.toJson(map);
        getSharedPreferences().edit().putString(KEY_BMS_WIFI_MAP, json).apply();
    }



}
