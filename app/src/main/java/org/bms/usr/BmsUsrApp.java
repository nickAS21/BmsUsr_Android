package org.bms.usr;

import static org.bms.usr.provision.CodecBmsProvision.setContext;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;

import org.bms.usr.provision.CodecBmsProvision;

import java.util.Locale;

public class BmsUsrApp extends Application {

    private static final String PREF_NAME = "AppPrefs";
    private static SharedPreferences sharedPreferences;
    private static WifiManager wifiManager;


    @Override
    public void onCreate() {
        super.onCreate();

        setContext(this);

        // Get and hold the SharedPreferences object for the app lifetime
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public static SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public static WifiManager getWifiManager() {
        return wifiManager;
    }
    public static String getDNSPref() {
        if (wifiManager == null) return null;

        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        if (dhcpInfo == null || dhcpInfo.dns1 == 0) return null;
        int dns = dhcpInfo.dns1;
        int b1 = (dns & 0xff);
        int b2 = (dns >> 8) & 0xff;
        int b3 = (dns >> 16) & 0xff;
        // Pref? example: 192.168.28. or 192.168.8.
        return String.format(Locale.US, "%d.%d.%d.", b1, b2, b3);
    }

}