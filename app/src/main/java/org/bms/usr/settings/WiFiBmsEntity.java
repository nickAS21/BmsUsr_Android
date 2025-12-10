package org.bms.usr.settings;

import static org.bms.usr.settings.HelperBmsSettings.NET_A_PORT_DEF;
import static org.bms.usr.settings.HelperBmsSettings.SOCKET_B_PORT_DEF;
import static org.bms.usr.settings.HelperBmsSettings.getNetA_Ip;
import static org.bms.usr.settings.HelperBmsSettings.getNetB_Ip;

import java.util.Map;

public record WiFiBmsEntity(
        int id,             // Number
        String ssid,        // SSID WiFi BMS
        String ssidBms,     // SSID BMS
        String bssid,       // MAC BMS
        String netIpA,// Network A Setting -> IP сервера, куди BMS підключається у STA-режимі як клієнт (ноут / AWS)
        int netAPort,          // Network A Setting Port STA mode Client  -> Обчислюється: 8890 + id
        String netIpB,       // Socket B: =>  IP BMS у STA mode
        int netBPort,         // Socket B: =>  Port BMS у STA mode -> Обчислюється: 18890 + id
        String oui           // Chip manufacturer - > Обчислюється за bssid
) {

    private static final Map<String, String> OUI_DATABASE = Map.of(
    "F4:70:0C", "Jinan USR IOT Technology Limited",
    "9C:A5:25", "Shandong USR IOT Technology Limited",
    "D4:AD:20", "Jinan USR IOT Technology Limited"
    );

    public static String getOuiVendorName(String bssid) {
        if (bssid == null || bssid.length() < 8) {
            return "UNKNOWN";
        }
        String ouiPrefix = bssid.substring(0, 8).toUpperCase();

        return OUI_DATABASE.getOrDefault(ouiPrefix, "UNKNOWN");
    }

    public WiFiBmsEntity(int id, String ssid, String ssidBms, String bssid, String netIpA, String netIpB) {
        this(
            id,
            ssid,
            ssidBms,
            bssid,
            netIpA == null ? getNetA_Ip() : netIpA,         // netIpA -> def = IP_DEF_AP
            NET_A_PORT_DEF + id,                            // netAPort
            netIpB == null ? getNetB_Ip() : netIpB,         // netIpB
            SOCKET_B_PORT_DEF + id,                         // netBPort
            getOuiVendorName(bssid)                         // oui
        );
    }

    public String getOui() {
        return this.oui;
    }}
