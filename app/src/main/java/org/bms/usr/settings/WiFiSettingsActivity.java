package org.bms.usr.settings;

import static org.bms.usr.BmsUsrApp.getWifiManager;
import static org.bms.usr.settings.HelperBmsSettings.addOrUpdateBmsWifiEntry;
import static org.bms.usr.settings.HelperBmsSettings.getBmsWifiMap;
import static org.bms.usr.settings.HelperBmsSettings.removeBmsWifiEntry;
import static org.bms.usr.settings.HelperBmsSettings.resetBmsWifiMap;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.net.wifi.ScanResult;
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
import android.widget.TextView;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class WiFiSettingsActivity extends AppCompatActivity implements WifiBmsListAdapter.OnItemClickListener,
        WifiBmsListAdapter.OnInfoClickListener {
    private RecyclerView recyclerViewWiFiBmsNetworks;
    private WifiBmsListAdapter bmsNetworksAdapter;
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
        recyclerViewWiFiBmsNetworks = findViewById(R.id.recyclerViewWiFiBmsNetworks);
        wifiManager = getWifiManager();
        recyclerViewWiFiBmsNetworks.setLayoutManager(new LinearLayoutManager(this));
        bmsNetworksAdapter = new WifiBmsListAdapter(this, this, this);
        recyclerViewWiFiBmsNetworks.setAdapter(bmsNetworksAdapter);
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
        List<WiFiBmsEntity> list = new ArrayList<>(getBmsWifiMap().values());
        list.sort(Comparator.comparingInt(WiFiBmsEntity::id));
        bmsNetworksAdapter.setWiFiBmsEntities(list);
    }

    private void showAddBmsEntryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_add_bms_entry_title);

        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(32, 32, 32, 32);

        final Spinner spinnerAvailableNetworks = new Spinner(this);
        linearLayout.addView(spinnerAvailableNetworks);

        final EditText inputId = new EditText(this);
        inputId.setHint(R.string.id_input_hint);
        linearLayout.addView(inputId);

        final EditText inputSsid = new EditText(this);
        inputSsid.setHint(R.string.ssid_input_hint);
        linearLayout.addView(inputSsid);

        final EditText inputSsidBms = new EditText(this);
        inputSsidBms.setHint(R.string.ssid_bms_input_hint);
        linearLayout.addView(inputSsidBms);

        final EditText inputBssid = new EditText(this);
        inputBssid.setHint(R.string.bssid_input_hint);
        linearLayout.addView(inputBssid);

        final EditText inputIpSatWiFiHome = new EditText(this);
        inputIpSatWiFiHome.setHint(R.string.ip_sta_wifi_home_input_hint);
        linearLayout.addView(inputIpSatWiFiHome);

        final EditText inputPortSta = new EditText(this);
        inputPortSta.setHint(R.string.port_sta_input_hint);
        linearLayout.addView(inputPortSta);

        builder.setView(linearLayout);

        // Отримати список  збережених мереж
        final List<WiFiBmsEntity> availableNetworks = new ArrayList<>(getBmsWifiMap().values());;
        final List<String> ssidList = new ArrayList<>();
        ssidList.add("Select from list"); // дефолтна опція
        for (WiFiBmsEntity network : availableNetworks) {
            ssidList.add(network.bssid());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ssidList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAvailableNetworks.setAdapter(adapter);

        spinnerAvailableNetworks.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    WiFiBmsEntity selectedNetwork = availableNetworks.get(position - 1);
                    inputId.setText(selectedNetwork.id());
                    inputSsid.setText(selectedNetwork.ssid());
                    inputSsidBms.setText(selectedNetwork.ssidBms());
                    inputBssid.setText(selectedNetwork.bssid());
                    inputIpSatWiFiHome.setText(selectedNetwork.ipWiFiHome());
                    inputPortSta.setText(selectedNetwork.portSTA());
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
                int newId = Integer.parseInt(String.valueOf(inputId.getText()));
                String newSsid = inputSsid.getText().toString().trim();
                String newSsidBms = inputSsidBms.getText().toString().trim();
                String newBssid = inputBssid.getText().toString().trim();

                if (newId != 0 && !newSsid.isEmpty() && !newBssid.isEmpty()) {
                    Map<String, WiFiBmsEntity> bmsWifiMap = getBmsWifiMap();

                    // Перевірка на дублікати
                    String conflictMsg = null;
                    String conflictSsid = null;

                    if (bmsWifiMap.containsKey(newSsid)) {
                        conflictSsid = newSsid;
                        conflictMsg = "SSID \"" + newSsid + "\" вже існує";
                    }

                    for (Map.Entry<String, WiFiBmsEntity> entry : bmsWifiMap.entrySet()) {
                        if (entry.getValue().bssid().equals(newBssid)) {
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
                                    addOrUpdateBmsWifiEntry(newId, newSsid, newSsidBms,  newBssid);
                                    Toast.makeText(this, getString(R.string.entry_updated_toast, newSsid), Toast.LENGTH_SHORT).show();
                                    loadBmsNetworks();
                                }
                        );

                    } else {
                        addOrUpdateBmsWifiEntry(newId, newSsid, newSsidBms,  newBssid);
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

        final EditText inputId = new EditText(this);
        inputId.setHint(R.string.id_input_hint);
        inputId.setEnabled(false);
        linearLayout.addView(inputId);

        final EditText inputSsid = new EditText(this);
        inputSsid.setHint(R.string.ssid_input_hint);
        inputSsid.setEnabled(false);
        linearLayout.addView(inputSsid);

        final EditText inputSsidBms = new EditText(this);
        inputSsidBms.setHint(R.string.ssid_bms_input_hint);
        inputSsidBms.setEnabled(false);
        linearLayout.addView(inputSsidBms);

        final EditText inputBssid = new EditText(this);
        inputBssid.setHint(R.string.bssid_input_hint);
        inputBssid.setEnabled(false);
        linearLayout.addView(inputBssid);

        final EditText inputIpSatWiFiHome = new EditText(this);
        inputIpSatWiFiHome.setHint(R.string.ip_sta_wifi_home_input_hint);
        inputBssid.setEnabled(false);
        linearLayout.addView(inputIpSatWiFiHome);

        final EditText inputPortSta = new EditText(this);
        inputPortSta.setHint(R.string.port_sta_input_hint);
        inputBssid.setEnabled(false);
        linearLayout.addView(inputPortSta);

        builder.setView(linearLayout);

        final Map<String, WiFiBmsEntity> savedNetworksMap = getBmsWifiMap();
        final List<String> ssidList = new ArrayList<>(savedNetworksMap.keySet());
        ssidList.add(0, "Select a network");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ssidList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSavedNetworks.setAdapter(adapter);

        spinnerSavedNetworks.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    String selectedBSsid = parent.getItemAtPosition(position).toString();
                    WiFiBmsEntity values = savedNetworksMap.get(selectedBSsid);
                    originalSsidToUpdate[0] = selectedBSsid;
                    inputSsid.setText(values.ssid());
                    inputBssid.setText(selectedBSsid);
                    inputSsidBms.setText(values.ssidBms());
                    inputId.setText(String.valueOf(values.id()));
                    inputIpSatWiFiHome.setText("http://" + values.ipWiFiHome());
                    inputPortSta.setText(String.valueOf(values.portSTA()));

                    inputId.setEnabled(true);
                    inputSsid.setEnabled(true);
                    inputSsidBms.setEnabled(true);
                    inputBssid.setEnabled(true);
                    inputIpSatWiFiHome.setEnabled(true);
                    inputPortSta.setEnabled(true);
                } else {
                    originalSsidToUpdate[0] = null;
                    inputId.setText("");
                    inputSsid.setText("");
                    inputSsidBms.setText("");
                    inputBssid.setText("");
                    inputIpSatWiFiHome.setText("");
                    inputPortSta.setText("");
                    inputId.setEnabled(false);
                    inputSsid.setEnabled(false);
                    inputSsidBms.setEnabled(false);
                    inputBssid.setEnabled(false);
                    inputIpSatWiFiHome.setEnabled(false);
                    inputPortSta.setEnabled(false);
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
                int updatedId = Integer.parseInt((inputId.getText().toString()));
                String updatedSsid = inputSsid.getText().toString();
                String updatedSsidBms = inputSsidBms.getText().toString();
                String updatedBssid = inputBssid.getText().toString();


                if (originalSsidToUpdate[0] != null && !updatedSsid.isEmpty() && !updatedBssid.isEmpty()) {
                    if (savedNetworksMap.containsKey(updatedSsid) && !updatedSsid.equals(originalSsidToUpdate[0])) {
                        showConflictConfirmationDialog(
                                parentDialog,
                                getString(R.string.dialog_duplicate_entry_message, updatedSsid),
                                () -> performUpdate(originalSsidToUpdate[0], updatedBssid, updatedId, updatedSsid, updatedSsidBms)
                        );

                    } else {
                        performUpdate(originalSsidToUpdate[0], updatedBssid, updatedId, updatedSsid, updatedSsidBms);
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


    private void performUpdate(String oldBssid, String newBssid, int newId, String newSsid, String newSsidBms) {
        removeBmsWifiEntry(oldBssid);
        addOrUpdateBmsWifiEntry(newId, newSsid, newSsidBms, newBssid);
        Toast.makeText(this, getString(R.string.entry_updated_toast, newBssid), Toast.LENGTH_SHORT).show();
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
        final Map<String, WiFiBmsEntity> savedNetworksMap = getBmsWifiMap();
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
        Map<String, WiFiBmsEntity> bmsWifiMap = getBmsWifiMap();
        for (Map.Entry<String, WiFiBmsEntity> entry : bmsWifiMap.entrySet()) {
            if (entry.getKey().equals(ssid)) {
                return entry.getValue().bssid();
            }
        }
        return null;
    }

    private void showNetworkInfoDialog(ScanResult result, String ssid) {
        String security = getSecurityType(result.capabilities);

        String info = "SSID: " + (result.SSID.isEmpty() ? ssid : result.SSID) +
                "\nBSSID: " + result.BSSID +
                "\nFrequency: " + result.frequency + " MHz" +
                "\nSignal level: " + WifiManager.calculateSignalLevel(result.level, 5) + " / 4" +
                "\nRSSI: " + result.level + " dBm" +
                "\nCapabilities: " + result.capabilities +
                "\nSecurity: " + security;

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.network_info_title))
                .setMessage(info)
                .setPositiveButton(R.string.button_ok, null)
                .show();
    }

    // Витягнути тип безпеки з capabilities
    private String getSecurityType(String caps) {
        if (caps.contains("WEP")) return "WEP";
        if (caps.contains("WPA3")) return "WPA3";
        if (caps.contains("WPA2")) return "WPA2";
        if (caps.contains("WPA")) return "WPA";
        return "Open";
    }

    @Override
    public void onInfoClick(WiFiBmsEntity network) {
        @SuppressLint("MissingPermission") List<ScanResult> results = wifiManager.getScanResults();
        for (ScanResult result : results) {
            if (result.SSID.equals(network.ssid()) || result.SSID.equals(network.ssidBms()) ||
                    result.BSSID.equals(network.bssid())) {
                // показати детальну інфу по ScanResult
                showNetworkInfoDialog(result, network.ssid());
                return;
            }
        }
        Toast.makeText(this,
                "Детальна інформація недоступна для цієї мережі.",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemClick(WiFiBmsEntity wiFiBmsEntity) {
        showEntityInfoDialog(wiFiBmsEntity);
    }

    private void showEntityInfoDialog(WiFiBmsEntity entity) {
        if (entity == null) return;

        // Інфлейтимо твій layout detail_entity.xml
        View dialogView = getLayoutInflater().inflate(R.layout.wifi_bms_entity_details, null);

        // Проставляємо значення
        ((TextView) dialogView.findViewById(R.id.detail_id)).setText(String.valueOf(entity.id()));
        ((TextView) dialogView.findViewById(R.id.detail_ssid)).setText(safe(entity.ssid()));
        ((TextView) dialogView.findViewById(R.id.detail_ssid_bms)).setText(safe(entity.ssidBms()));
        ((TextView) dialogView.findViewById(R.id.detail_bssid)).setText(safe(entity.bssid()));
        ((TextView) dialogView.findViewById(R.id.detail_ip_wifi_home)).setText(safe(entity.ipWiFiHome()));
        ((TextView) dialogView.findViewById(R.id.detail_ip_ap)).setText(safe(entity.ipAP()));
        ((TextView) dialogView.findViewById(R.id.detail_port_ap)).setText(String.valueOf(entity.portAp()));
        ((TextView) dialogView.findViewById(R.id.detail_ip_sta)).setText(safe(entity.ipSTA()));
        ((TextView) dialogView.findViewById(R.id.detail_port_sta)).setText(String.valueOf(entity.portSTA()));
        ((TextView) dialogView.findViewById(R.id.detail_oui)).setText(entity.getOui());

        // Створюємо AlertDialog з кастомним вмістом
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.network_info_title))
                .setView(dialogView)
                .setPositiveButton(R.string.button_ok, null)
                .show();
    }

    private String safe(String value) {
        return value == null ? "—" : value;
    }


}