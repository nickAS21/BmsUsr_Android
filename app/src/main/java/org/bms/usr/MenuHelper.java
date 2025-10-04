// MenuHelper.java
package org.bms.usr;

import android.view.Menu;

import org.bms.usr.provision.WiFiSearchActivity;
import org.bms.usr.settings.WiFiSettingsActivity;

public class MenuHelper {

    public static void prepareMenu(Menu menu, Class<?> activityClass) {
        // Hide all items by default to start clean
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setVisible(false);
        }

        // Use a switch statement based on the activity class
        if (activityClass.equals(MainActivity.class)) {
            menu.findItem(R.id.action_search).setVisible(true);
            menu.findItem(R.id.action_settings).setVisible(true);
            menu.findItem(R.id.action_about).setVisible(true);
            menu.findItem(R.id.action_exit).setVisible(true);
        } else if (activityClass.equals(WiFiSearchActivity.class)) {
            menu.findItem(R.id.action_filter).setVisible(true);
            menu.findItem(R.id.action_toggle_filter).setVisible(true);
            menu.findItem(R.id.action_reset_filter).setVisible(true);
        } else if (activityClass.equals(WiFiSettingsActivity.class)) {
            menu.findItem(R.id.action_reset_bms_wifi_map).setVisible(true);
            menu.findItem(R.id.action_add_bms_entry).setVisible(true);
            menu.findItem(R.id.action_updated_bms_entry).setVisible(true);
            menu.findItem(R.id.action_delete_bms_entry).setVisible(true);
        }
    }

    // Find icon by Wi-Fi level
     public static int getWifiSignalIcon(int signalLevel, boolean secured) {
        int icon;
        if (signalLevel >= -67) { // Full level
            icon = secured ? R.drawable.ic_wifi_signal_4_lock : R.drawable.ic_wifi_signal_4;
        } else if (signalLevel >= -70) {
            icon = secured ? R.drawable.ic_wifi_signal_3_lock : R.drawable.ic_wifi_signal_3;
        } else if (signalLevel >= -80) {
            icon = secured ? R.drawable.ic_wifi_signal_2_lock : R.drawable.ic_wifi_signal_2;
        } else if (signalLevel > -90) {
            icon = secured ? R.drawable.ic_wifi_signal_1_lock : R.drawable.ic_wifi_signal_1;
        } else {
            icon = secured ? R.drawable.ic_wifi_signal_0_lock : R.drawable.ic_wifi_signal_0;
        }
        return icon;
    }
}