package org.bms.usr;

import android.app.Application;
import android.content.SharedPreferences;

import org.bms.usr.service.CodecBms;

public class BmsUsrApp extends Application {

    private static final String PREF_NAME = "AppPrefs";
    private static SharedPreferences sharedPreferences;


    @Override
    public void onCreate() {
        super.onCreate();
        // Init the CodecBms one time
        CodecBms.setContext(this);

        // Get and hold the SharedPreferences object for the app lifetime
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
    }

    public static SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }
}