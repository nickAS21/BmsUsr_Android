package org.bms.usr.settings;

import static org.bms.usr.settings.HelperBmsSettings.addOrUpdateBmsWifiEntry;
import static org.bms.usr.settings.HelperBmsSettings.getBmsWifiMap;
import static org.bms.usr.settings.HelperBmsSettings.removeBmsWifiEntry;
import static org.bms.usr.settings.HelperBmsSettings.resetBmsWifiMap;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.bms.usr.MenuHelper;
import org.bms.usr.R;
import org.bms.usr.service.WifiListAdapter;
import org.bms.usr.service.WifiNetwork;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WiFiSettingsActivity extends AppCompatActivity implements WifiListAdapter.OnItemClickListener {

    private RecyclerView recyclerViewBmsNetworks;
    private WifiListAdapter bmsNetworksAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_wifi_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        // Setup RecyclerView to display BMS networks
        recyclerViewBmsNetworks = findViewById(R.id.recyclerViewBmsNetworks);
        recyclerViewBmsNetworks.setLayoutManager(new LinearLayoutManager(this));
        bmsNetworksAdapter = new WifiListAdapter(this, this);
        recyclerViewBmsNetworks.setAdapter(bmsNetworksAdapter);

        // Load and display networks from SharedPreferences
        loadBmsNetworks();
    }

    // New methods for menu integration
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuHelper.prepareMenu(menu, this.getClass());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_reset_bms_wifi_map) {
            // Reset and refresh the list
            resetBmsWifiMap();
            loadBmsNetworks();
            Toast.makeText(this, R.string.reset_bms_wifi_map_text, Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_add_bms_entry) {
            showAddBmsEntryDialog();
            return true;
        } else if (id == R.id.action_delete_bms_entry) {
            showDeleteBmsEntryDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Method to load and display the BMS networks
    private void loadBmsNetworks() {
        Map<String, String> bmsWifiMap = getBmsWifiMap();
        List<WifiNetwork> bmsNetworksList = new ArrayList<>();

        // Convert the map to a list of WifiNetwork objects
        for (Map.Entry<String, String> entry : bmsWifiMap.entrySet()) {
            String bssid = entry.getKey();
            String ssid = entry.getValue();
            // Assuming default values, as we only have BSSID and SSID
            bmsNetworksList.add(new WifiNetwork(ssid, bssid, 0, false, false, true));
        }

        bmsNetworksAdapter.setWifiNetworks(bmsNetworksList);
    }

    private void showAddBmsEntryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_add_bms_entry_title);

        // Create a layout for the dialog with two EditText fields
        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(32, 32, 32, 32);

        final EditText inputSsid = new EditText(this);
        inputSsid.setHint(R.string.ssid_input_hint);
        linearLayout.addView(inputSsid);

        final EditText inputBssid = new EditText(this);
        inputBssid.setHint(R.string.bssid_input_hint);
        linearLayout.addView(inputBssid);

        builder.setView(linearLayout);

        builder.setPositiveButton(R.string.button_ok, (dialog, which) -> {
            String newSsid = inputSsid.getText().toString();
            String newBssid = inputBssid.getText().toString();

            if (!newSsid.isEmpty() && !newBssid.isEmpty()) {
                // The existing helper method addOrUpdateBmsWifiEntry handles this
                addOrUpdateBmsWifiEntry(newBssid, newSsid);
                Toast.makeText(this, getString(R.string.entry_added_toast, newSsid), Toast.LENGTH_SHORT).show();
                loadBmsNetworks(); // Refresh the list
            }
        });

        builder.setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showDeleteBmsEntryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_enter_ssid_title);
        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton(R.string.button_ok, (dialog, which) -> {
            String ssidToRemove = input.getText().toString();
            if (!ssidToRemove.isEmpty()) {
                // Find BSSID by SSID
                String bssidToRemove = findBssidBySsid(ssidToRemove);
                if (bssidToRemove != null) {
                    boolean wasRemoved = removeBmsWifiEntry(bssidToRemove);
                    if (wasRemoved) {
                        Toast.makeText(this, getString(R.string.entry_deleted_toast, ssidToRemove), Toast.LENGTH_SHORT).show();
                        loadBmsNetworks(); // Refresh the list
                    }
                } else {
                    Toast.makeText(this, getString(R.string.entry_not_found_toast, ssidToRemove), Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // Helper method to find BSSID based on SSID from the current list
    private String findBssidBySsid(String ssid) {
        Map<String, String> bmsWifiMap = getBmsWifiMap();
        for (Map.Entry<String, String> entry : bmsWifiMap.entrySet()) {
            if (entry.getValue().equals(ssid)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public void onItemClick(WifiNetwork network) {
        // Implement the logic to retrieve settings for the selected network
        // based on network.getSsid() or network.getBssid()
        // Example: show a dialog or start a new activity
    }
}