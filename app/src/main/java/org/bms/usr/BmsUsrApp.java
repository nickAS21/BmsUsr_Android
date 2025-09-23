package org.bms.usr;

import static org.bms.usr.provision.CodecBmsProvision.setContext;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;

import org.bms.usr.provision.CodecBmsProvision;

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
}