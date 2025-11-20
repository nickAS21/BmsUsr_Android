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
    public static final String SOCKET_B_IP_DEF = "10.10.100.100";        // 10.10.100.100
    public static final String KEY_NET_A_IP_DEF = "netAIpDef";
    public static final String NET_A_IP_DEF = "192.168.8.119";          // IP PC, AWS. other -> // куди BMS підключається у NET A (STA) - mode Client (ноут / AWS)
    public static final int NET_A_PORT_DEF = 8890;                      // +id
    public static final int SOCKET_B_PORT_DEF = 18890;                   // +id

    public static void saveNetA_Ip(String NetA_IpDef) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(KEY_NET_A_IP_DEF, NetA_IpDef);
        editor.apply();
    }
    public static String getNetA_Ip() {
        return getSharedPreferences().getString(KEY_NET_A_IP_DEF, NET_A_IP_DEF);
    }
    public static Map<String, WiFiBmsEntity> getBmsWifiMap() {
        Gson gson = new Gson();
        String json = getSharedPreferences().getString(KEY_BMS_WIFI_MAP, null);
        if (json == null) {
            return new HashMap<>();
        }
        Type type = new TypeToken<Map<String, WiFiBmsEntity>>() {}.getType();
        return gson.fromJson(json, type);
    }
    public static void addOrUpdateBmsWifiEntry(int id, String ssid, String ssidBms, String bssid, String netIp) {
        Map<String, WiFiBmsEntity> map = getBmsWifiMap();
        map.put(bssid, new WiFiBmsEntity(id, ssid, ssidBms, bssid, netIp));
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
