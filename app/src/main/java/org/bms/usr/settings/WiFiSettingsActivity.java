package org.bms.usr.settings;

import static org.bms.usr.BmsUsrApp.getWifiManager;
import static org.bms.usr.settings.HelperBmsSettings.addOrUpdateBmsWifiEntry;
import static org.bms.usr.settings.HelperBmsSettings.getBmsWifiMap;
import static org.bms.usr.settings.HelperBmsSettings.removeBmsWifiEntry;
import static org.bms.usr.settings.HelperBmsSettings.resetBmsWifiMap;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class WiFiSettingsActivity extends AppCompatActivity implements WifiListAdapter.OnItemClickListener,
        WifiListAdapter.OnInfoClickListener {

    private RecyclerView recyclerViewBmsNetworks;
    private WifiListAdapter bmsNetworksAdapter;
    private WifiManager wifiManager;

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
        wifiManager = getWifiManager();
        recyclerViewBmsNetworks.setLayoutManager(new LinearLayoutManager(this));
        bmsNetworksAdapter = new WifiListAdapter(this, this, this);
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
        } else if (id == R.id.action_updated_bms_entry) {
            showUpdatedBmsEntryDialog();
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
            String ssid = entry.getKey();
            String bssid = entry.getValue();
            // Assuming default values, as we only have BSSID and SSID
            bmsNetworksList.add(new WifiNetwork(ssid, bssid, 0, false, false, true));
        }

        bmsNetworksAdapter.setWifiNetworks(bmsNetworksList);
    }

    private void showAddBmsEntryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_add_bms_entry_title);

        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(32, 32, 32, 32);

        final Spinner spinnerAvailableNetworks = new Spinner(this);
        linearLayout.addView(spinnerAvailableNetworks);

        final EditText inputSsid = new EditText(this);
        inputSsid.setHint(R.string.ssid_input_hint);
        linearLayout.addView(inputSsid);

        final EditText inputBssid = new EditText(this);
        inputBssid.setHint(R.string.bssid_input_hint);
        linearLayout.addView(inputBssid);

        builder.setView(linearLayout);

        // Отримати список доступних мереж
        final List<WifiNetwork> availableNetworks = getAvailableNetworks();
        final List<String> ssidList = new ArrayList<>();
        ssidList.add("Select from list"); // дефолтна опція
        for (WifiNetwork network : availableNetworks) {
            ssidList.add(network.getSsid());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ssidList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAvailableNetworks.setAdapter(adapter);

        spinnerAvailableNetworks.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    WifiNetwork selectedNetwork = availableNetworks.get(position - 1);
                    inputSsid.setText(selectedNetwork.getSsid());
                    inputBssid.setText(selectedNetwork.getBSsid());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Кнопку OK перехоплюємо вручну, щоб не закривався діалог при конфлікті
        builder.setPositiveButton(R.string.button_ok, null);
        builder.setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel());

        AlertDialog parentDialog = builder.create();

        parentDialog.setOnShowListener(d -> {
            Button okButton = parentDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            okButton.setOnClickListener(v -> {
                String newSsid = inputSsid.getText().toString().trim();
                String newBssid = inputBssid.getText().toString().trim();

                if (!newSsid.isEmpty() && !newBssid.isEmpty()) {
                    Map<String, String> bmsWifiMap = getBmsWifiMap();

                    // Перевірка на дублікати
                    String conflictMsg = null;
                    String conflictSsid = null;

                    if (bmsWifiMap.containsKey(newSsid)) {
                        conflictSsid = newSsid;
                        conflictMsg = "SSID \"" + newSsid + "\" вже існує";
                    }

                    for (Map.Entry<String, String> entry : bmsWifiMap.entrySet()) {
                        if (entry.getValue().equals(newBssid)) {
                            if (conflictMsg != null) {
                                conflictMsg += " і BSSID \"" + newBssid + "\" теж існує (SSID = " + entry.getKey() + ")";
                            } else {
                                conflictMsg = "BSSID \"" + newBssid + "\" вже існує (SSID = " + entry.getKey() + ")";
                            }
                            break;
                        }
                    }

                    if (conflictMsg != null) {
                       String conflictText = "Конфлікт: " + conflictMsg + "\n\nВсе одно додати/оновити?";
                        final String[] originalSsidToAdd = new String[1];
                        originalSsidToAdd[0] = conflictSsid;
                        showConflictConfirmationDialog(
                                parentDialog,
                                conflictText,
                                () -> {
                                    if (originalSsidToAdd[0] != null) {
                                        removeBmsWifiEntry(originalSsidToAdd[0]);
                                    }
                                    addOrUpdateBmsWifiEntry(newSsid,  newBssid);
                                    Toast.makeText(this, getString(R.string.entry_updated_toast, newSsid), Toast.LENGTH_SHORT).show();
                                    loadBmsNetworks();
                                }
                        );

                    } else {
                        addOrUpdateBmsWifiEntry(newSsid, newBssid);
                        Toast.makeText(this, getString(R.string.entry_added_toast, newSsid), Toast.LENGTH_SHORT).show();
                        loadBmsNetworks();
                        parentDialog.dismiss();
                    }
                }
            });
        });

        parentDialog.show();
    }

    private void showUpdatedBmsEntryDialog() {
        final String[] originalSsidToUpdate = new String[1];
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_updated_bms_entry_title);

        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(32, 32, 32, 32);

        final Spinner spinnerSavedNetworks = new Spinner(this);
        linearLayout.addView(spinnerSavedNetworks);

        final EditText inputSsid = new EditText(this);
        inputSsid.setHint(R.string.ssid_input_hint);
        inputSsid.setEnabled(false);
        linearLayout.addView(inputSsid);

        final EditText inputBssid = new EditText(this);
        inputBssid.setHint(R.string.bssid_input_hint);
        inputBssid.setEnabled(false);
        linearLayout.addView(inputBssid);

        builder.setView(linearLayout);

        final Map<String, String> savedNetworksMap = getBmsWifiMap();
        final List<String> ssidList = new ArrayList<>(savedNetworksMap.keySet());
        ssidList.add(0, "Select a network");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ssidList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSavedNetworks.setAdapter(adapter);

        spinnerSavedNetworks.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    String selectedSsid = parent.getItemAtPosition(position).toString();
                    String bssid = savedNetworksMap.get(selectedSsid);

                    originalSsidToUpdate[0] = selectedSsid;

                    inputSsid.setText(selectedSsid);
                    inputBssid.setText(bssid);

                    inputSsid.setEnabled(true);
                    inputBssid.setEnabled(true);
                } else {
                    originalSsidToUpdate[0] = null;
                    inputSsid.setText("");
                    inputBssid.setText("");
                    inputSsid.setEnabled(false);
                    inputBssid.setEnabled(false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        builder.setPositiveButton(R.string.button_ok, null);
        builder.setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel());

        AlertDialog parentDialog = builder.create();

        parentDialog.setOnShowListener(d -> {
            Button okButton = parentDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            okButton.setOnClickListener(v -> {
                String updatedSsid = inputSsid.getText().toString();
                String updatedBssid = inputBssid.getText().toString();

                if (originalSsidToUpdate[0] != null && !updatedSsid.isEmpty() && !updatedBssid.isEmpty()) {
                    if (savedNetworksMap.containsKey(updatedSsid) && !updatedSsid.equals(originalSsidToUpdate[0])) {
                        showConflictConfirmationDialog(
                                parentDialog,
                                getString(R.string.dialog_duplicate_entry_message, updatedSsid),
                                () -> performUpdate(originalSsidToUpdate[0], updatedSsid, updatedBssid)
                        );

                    } else {
                        performUpdate(originalSsidToUpdate[0], updatedSsid, updatedBssid);
                        parentDialog.dismiss();
                    }
                }
            });
        });

        parentDialog.show();
    }

    private void showConflictConfirmationDialog(final AlertDialog parentDialog,
                                                final String message,
                                                final Runnable onConfirm) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_duplicate_entry_title)
                .setMessage(message)
                .setPositiveButton(R.string.dialog_button_replace, (dialog, which) -> {
                    onConfirm.run();
                    parentDialog.dismiss();
                })
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }


    private void performUpdate(String oldSsid, String newSsid, String newBssid) {
        removeBmsWifiEntry(oldSsid);
        addOrUpdateBmsWifiEntry(newSsid, newBssid);
        Toast.makeText(this, getString(R.string.entry_updated_toast, newSsid), Toast.LENGTH_SHORT).show();
        loadBmsNetworks();
    }
    private void showDeleteBmsEntryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_enter_ssid_title);

        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(32, 32, 32, 32);

        final Spinner spinnerSavedNetworks = new Spinner(this);
        linearLayout.addView(spinnerSavedNetworks);

        builder.setView(linearLayout);

        // Get the list of saved networks from SharedPreferences
        final Map<String, String> savedNetworksMap = getBmsWifiMap();
        final List<String> ssidList = new ArrayList<>(savedNetworksMap.keySet());
        ssidList.add(0, "Select a network");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ssidList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSavedNetworks.setAdapter(adapter);

        builder.setPositiveButton(R.string.button_ok, (dialog, which) -> {
            String ssidToRemove = spinnerSavedNetworks.getSelectedItem().toString();
            if (!ssidToRemove.equals("Select a network")) {
                boolean wasRemoved = removeBmsWifiEntry(ssidToRemove);
                if (wasRemoved) {
                    Toast.makeText(this, getString(R.string.entry_deleted_toast, ssidToRemove), Toast.LENGTH_SHORT).show();
                    loadBmsNetworks();
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

    private List<WifiNetwork> getAvailableNetworks() {
        List<WifiNetwork> availableNetworks = new ArrayList<>();
        WifiManager wifiManager = getWifiManager();
        if (wifiManager == null) {
            return availableNetworks;
        }

        @SuppressLint("MissingPermission") List<ScanResult> results = wifiManager.getScanResults();
        if (results == null || results.isEmpty()) {
            return availableNetworks;
        }

        HashMap<String, ScanResult> uniqueNetworks = new HashMap<>();
        for (ScanResult scanResult : results) {
            String key = scanResult.SSID;
            if (key != null && !key.isEmpty()) {
                if (uniqueNetworks.containsKey(key)) {
                    if (Objects.requireNonNull(uniqueNetworks.get(key)).level < scanResult.level) {
                        uniqueNetworks.put(key, scanResult);
                    }
                } else {
                    uniqueNetworks.put(key, scanResult);
                }
            }
        }

        for (Map.Entry<String, ScanResult> entry : uniqueNetworks.entrySet()) {
            ScanResult scanResult = entry.getValue();
            boolean secured = scanResult.capabilities.contains("WEP")
                    || scanResult.capabilities.contains("WPA")
                    || scanResult.capabilities.contains("WPA2")
                    || scanResult.capabilities.contains("WPA3");
            availableNetworks.add(new WifiNetwork(scanResult.SSID, scanResult.BSSID, scanResult.level, false, false, secured));
        }

        return availableNetworks;
    }

    @Override
    public void onItemClick(WifiNetwork network) {
        // Implement the logic to retrieve settings for the selected network
        // based on network.getSsid() or network.getBSsid()
        // Example: show a dialog or start a new activity
    }

    @Override
    public void onInfoClick(WifiNetwork network) {
        // This method handles clicks on the info icon.
        // Your logic for getting network info goes here.
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null && network.getBSsid().equals(wifiInfo.getBSSID())) {
            showNetworkInfoDialog(wifiInfo);
        } else {
            Toast.makeText(this, "Network information is only available for the connected network.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showNetworkInfoDialog(WifiInfo wifiInfo) {
        // Get DHCP information for network details
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();

        String info = "SSID: " + wifiInfo.getSSID() +
                "\nBSSID: " + wifiInfo.getBSSID() +
                "\nIP Address: " + intToIp(dhcpInfo.ipAddress) +
                "\nGateway: " + intToIp(dhcpInfo.gateway) +
                "\nDNS1: " + intToIp(dhcpInfo.dns1);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.network_info_title))
                .setMessage(info)
                .setPositiveButton(R.string.button_ok, null)
                .show();
    }

    // A helper method to convert integer IP address to string format
    public String intToIp(int ipAddress) {
        return String.format(Locale.ROOT, "%d.%d.%d.%d",
                (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff),
                (ipAddress >> 24 & 0xff));
    }

}