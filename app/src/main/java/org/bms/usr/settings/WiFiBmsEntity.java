package org.bms.usr.settings;

import static org.bms.usr.settings.HelperBmsSettings.IP_DEF_STA_PREFIX;
import static org.bms.usr.settings.HelperBmsSettings.IP_DEF_WIFI_HOME_PREFIX;
import static org.bms.usr.settings.HelperBmsSettings.PORT_DEF_AP_BASE;
import static org.bms.usr.settings.HelperBmsSettings.PORT_DEF_STA_BASE;

import java.util.Map;

public record WiFiBmsEntity(
        int id,             // Number
        String ssid,        // SSID WiFi BMS
        String ssidBms,     // SSID BMS
        String bssid,       // MAC BMS
        String ipWiFiHome,  // IP BMS у домашній мережі (Static/DHCP, для HTTP доступу)-> Обчислюється: "192.168.8.<100 + id>"
        String ipAP,        // IP сервера, куди BMS підключається у AP-режимі (твій ноут / AWS) -> встановити статичний IP сервера
        int portAp,          // Порт у AP-режимі  -> Обчислюється: 8890 + id
        String ipSTA,        // IP BMS у STA-режимі (Socket B: Server → куди підключаєшся по TCP) -> Обчислюється: "10.10.<100 + id>.100"
        int portSTA,         // Порт у STA-режимі -> Обчислюється: 18890 + id
        String oui           // Chip manufacturer - > Обчислюється за bssid
) {

    private static final Map<String, String> OUI_DATABASE = Map.of(
            "F4:70:0C", "G.S.D GROUP INC.",
            "9C:A5:25", "Shandong USR IOT Technology Limited"
    );


    public static String getOuiVendorName(String bssid) {
        if (bssid == null || bssid.length() < 8) {
            return "UNKNOWN";
        }
        String ouiPrefix = bssid.substring(0, 8).toUpperCase();

        return OUI_DATABASE.getOrDefault(ouiPrefix, "UNKNOWN");
    }

    public WiFiBmsEntity(int id, String ssid, String ssidBms, String bssid) {
        this(
                id,
                ssid,
                ssidBms,
                bssid,
                IP_DEF_WIFI_HOME_PREFIX + (100 + id), // ipWiFiHome
                null,
                PORT_DEF_AP_BASE + id,                   // portAp
                "10.10." + (100 + id) + ".100",                 // ipSTA
                PORT_DEF_STA_BASE + id,                         // portSTA
                getOuiVendorName(bssid)                         // oui
        );
    }

    public String getOui() {
        return this.oui;
    }}
