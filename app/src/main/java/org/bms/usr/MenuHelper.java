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
}