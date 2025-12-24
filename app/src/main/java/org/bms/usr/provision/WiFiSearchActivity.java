package org.bms.usr.provision;

import static org.bms.usr.BmsUsrApp.getWifiManager;
import static org.bms.usr.provision.HelperBmsProvision.CHOSEN_BSSID_TEXT;
import static org.bms.usr.provision.HelperBmsProvision.CHOSEN_SSID_TEXT;
import static org.bms.usr.provision.HelperBmsProvision.CURRENT_SSID_START_TEXT;
import static org.bms.usr.provision.HelperBmsProvision.WIFI_FILTER_ENABLED_DEF;
import static org.bms.usr.provision.HelperBmsProvision.WIFI_FILTER_SSID_DEF;
import static org.bms.usr.provision.HelperBmsProvision.getWifiFilterSsid;
import static org.bms.usr.provision.HelperBmsProvision.isFilterEnabled;
import static org.bms.usr.provision.HelperBmsProvision.saveFilterSettings;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.bms.usr.MenuHelper;
import org.bms.usr.R;
import org.bms.usr.service.WifiListAdapter;
import org.bms.usr.service.WifiNetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WiFiSearchActivity extends AppCompatActivity implements WifiListAdapter.OnItemClickListener {

    private static final int PERMISSIONS_REQUEST_CODE = 1001;
    private WifiListAdapter adapter;
//    private List<WifiNetwork> wifiNetworks;
    private WifiManager wifiManager;
    private WifiScanReceiver wifiScanReceiver;
    private ProgressBar progressBar;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isReceiverRegistered = false;
    private boolean isScanCompleted = false;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    // --- added for robust scanning retries and location check
    private static final int MAX_SCAN_RETRIES = 3;
    private int scanRetryCount = 0;
    private static final long RETRY_DELAY_MS = 1000;
    private LocationManager locationManager;

    private String pendingChosenSsid = null;
    private String currentSsidStart = null;
    private Boolean isFilterEnabled = true;

    private String wifiFilterSsid; // Default filter string

    private ActivityResultLauncher<Intent> wifiProvisionLauncher;
    private boolean isFineLocation = false;

    private ActivityResultLauncher<Intent> locationSettingsLauncher;
    private ActivityResultLauncher<Intent> wifiSettingsLauncher;
    private boolean returningFromWifiSettings = false;
    private boolean returningFromLocationSettings = false;
    private boolean returningFromProvision = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_search);

        // Load saved filter settings
        wifiFilterSsid = getWifiFilterSsid();
        isFilterEnabled = isFilterEnabled();

        progressBar = findViewById(R.id.progressBar);

        RecyclerView recyclerView = findViewById(R.id.recyclerViewWifiList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//        wifiNetworks = new ArrayList<>();
        adapter = new WifiListAdapter( this, this, null, false);
        recyclerView.setAdapter(adapter);

        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());
        Button buttonRescan = findViewById(R.id.buttonRescan);
        buttonRescan.setOnClickListener(v -> restartWifiScan());

        wifiManager = getWifiManager();;
        wifiScanReceiver = new WifiScanReceiver();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        locationSettingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // User return from settings
                    if (isLocationEnabled()) {
                        // ✅ Al ok
                        restartWifiScan();
                    } else {
                        // ❌ User not off Location
                        Toast.makeText(this, getString(R.string.location_off_for_scan), Toast.LENGTH_LONG).show();
                    }
                }
        );
        wifiSettingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (wifiManager.isWifiEnabled()) {
                        // ✅ User on Wi-Fi
                        restartWifiScan();
                    } else {
                        // ❌ User not on Wi-Fi
                        Toast.makeText(this, getString(R.string.wifi_off_for_scan), Toast.LENGTH_LONG).show();
                    }
                }
        );

        wifiProvisionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleWifiSettingsResult
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean isFilterEnabledMenu = isFilterEnabled();
        MenuHelper.prepareMenu(menu, this.getClass());
        MenuItem filterItem = menu.findItem(R.id.action_filter);
        if (filterItem != null) {
            filterItem.setEnabled(isFilterEnabledMenu);
        }
        MenuItem toggleItem = menu.findItem(R.id.action_toggle_filter);
        if (toggleItem != null) {
            toggleItem.setTitle(isFilterEnabledMenu ? R.string.disable_filter_text : R.string.enable_filter_text);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_filter) {
            showFilterDialog();
            return true;
        } else if (id == R.id.action_toggle_filter) {
            isFilterEnabled = !isFilterEnabled;
            saveFilterSettings(wifiFilterSsid, isFilterEnabled);

            // Update the menu item title to reflect the new state
            item.setTitle(isFilterEnabled ? R.string.disable_filter_text : R.string.enable_filter_text);
            Toast.makeText(this, isFilterEnabled ? R.string.filter_enabled_toast : R.string.filter_disabled_toast, Toast.LENGTH_SHORT).show();
            // Rescan to show filtered or unfiltered list
            restartWifiScan();
            return true;
        } else if (id == R.id.action_reset_filter) {
            resetFilterToDefault();
            // After resetting, we should probably update the menu item title as well
            Toast.makeText(WiFiSearchActivity.this, getString(R.string.filter_reset_toast, isFilterEnabled, wifiFilterSsid), Toast.LENGTH_SHORT).show();
            restartWifiScan();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 🔹 Гарантовано реєструємо ресівер тут
        if (!isReceiverRegistered) {
            registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            isReceiverRegistered = true;
        }

        if (returningFromProvision) {
            returningFromProvision = false;
            restartWifiScan();   // 🔹 один раз перезапускаємо
            return;
        }

        if (returningFromWifiSettings) {
            returningFromWifiSettings = false;
            if (wifiManager.isWifiEnabled()) {
                restartWifiScan();
            } else {
                Toast.makeText(this, getString(R.string.wifi_off_for_scan), Toast.LENGTH_LONG).show();
            }
            return;
        }

        if (returningFromLocationSettings) {
            returningFromLocationSettings = false;
            if (isLocationEnabled()) {
                restartWifiScan();
            } else {
                Toast.makeText(this, getString(R.string.location_off_for_scan), Toast.LENGTH_LONG).show();
            }
            return;
        }

        if (pendingChosenSsid != null) {
            // check if the connection to the selected network was successful
            WifiInfo currentWifiInfo = wifiManager.getConnectionInfo();
            if (currentWifiInfo != null) {
                String currentSsid = currentWifiInfo.getSSID();
                if (currentSsid != null && currentSsid.length() > 2) {
                    currentSsid = currentSsid.replace("\"", "");
                    if (pendingChosenSsid.equals(currentSsid)) {
                        wifiProvisionLauncher.launch(getIntentProvision(pendingChosenSsid, currentWifiInfo));
                        returningFromProvision = true;
                        pendingChosenSsid = null;
                    } else {
                        Toast.makeText(this, getString(R.string.connection_to_failed, pendingChosenSsid), Toast.LENGTH_SHORT).show();
                        restartWifiScan();
                    }
                }
            }
        } else {
            startWifiScan();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // 🔹 Відписуємо ресівер, щоб не дублювався
        if (isReceiverRegistered) {
            unregisterReceiver(wifiScanReceiver);
            isReceiverRegistered = false;
        }

        if (networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        // Stop the timer
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 🔹 На випадок, якщо ресівер ще підписаний
        if (isReceiverRegistered) {
            unregisterReceiver(wifiScanReceiver);
            isReceiverRegistered = false;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                restartWifiScan();
            } else {
                Toast.makeText(this, getString(R.string.location_permission_denied), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onItemClick(WifiNetwork network) {
        WifiInfo currentWifiInfo = wifiManager.getConnectionInfo();
        String currentSsid = (currentWifiInfo != null) ? currentWifiInfo.getSSID().replace("\"", "") : null;
        pendingChosenSsid = network.getSsid();
        if (pendingChosenSsid.equals(currentSsid)) {
            wifiProvisionLauncher.launch(getIntentProvision(pendingChosenSsid, currentWifiInfo));
            returningFromProvision = true;
            pendingChosenSsid = null;
        } else {
            // Selected a new network -> save and wait for connection confirmation
            Toast.makeText(this, getString(R.string.connection_to_waiting, pendingChosenSsid), Toast.LENGTH_SHORT).show();
            connectToWifi();
        }
    }

    public class WifiScanReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            List<ScanResult> results = getWifiManager().getScanResults();
            isScanCompleted = true;
            progressBar.setVisibility(View.GONE);
            handler.removeCallbacksAndMessages(null);
            if (results != null && !results.isEmpty()) {
                updateWifiList(results, true);
            } else {
                boolean extra = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, true);
                if (!extra || results == null || results.isEmpty()) {
                    if (scanRetryCount < MAX_SCAN_RETRIES) {
                        scanRetryCount++;
                        handler.postDelayed(() -> wifiManager.startScan(), RETRY_DELAY_MS * scanRetryCount);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(WiFiSearchActivity.this, getString(R.string.scan_failed), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    updateWifiList(results, true);
                }
            }
        }
    }
    private void checkWiFiPermissions() {
        if (!isReceiverRegistered) {
            registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            isReceiverRegistered = true;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_CODE);
        } else {
            isFineLocation = true;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            isFineLocation = true;
        }
    }

    private void restartWifiScan() {
        handler.removeCallbacksAndMessages(null); // скидаємо попередні таймери
        scanRetryCount = 0;
        isScanCompleted = false;
        progressBar.setVisibility(View.VISIBLE);
        startWifiScan();
    }
    /**
     * For tests
     * String hexData = "FF016C811B31343133413037534C444F504730303930303100640D0A31343133413037534C444F504730303930303100640D0A513030333334313030323032383400640D0A6c6562656400640D0A31343133413037534C444F504730303930303100640D0A31343133413037534C444F504730303930303100640D0A31343133413037534C444F504730303930303100600D0A4B6F766C617232472D31002C0D0A002A0D0A4D696B726F54696B2D39353733424200220D0A6e657469735F322E34475F43424433454300180D0A4761726275686100170D0A54656E64615F7769666900180D0A4B76313800140D0A00140D0A4A61636B000F0D0A000D0D0A456C697A6162657468000D0D0A70617061000D0D0A000D0D0A4D696B726F54696B2D364135444638000D0D0A486F6D652057692D4669000D0D0A54502D4C696E6B5F32454142000A0D0A4D6164726964000A0D0A3232316100050D0A3931316200050D0A464946492D5749464900020D0AA8";
     *     Map<String, Object> result =  decodeResponse(hexData);
     */
    private void startWifiScan() {
        if (wifiManager == null) {
            Toast.makeText(this, getString(R.string.wifi_manager_not_initialized), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, getString(R.string.wifi_off_for_scan), Toast.LENGTH_LONG).show();
            openWifiSettings();
            return;
        }

        if (!isFineLocation) {
            checkWiFiPermissions();
        }

        if (!isFineLocation || !isLocationEnabled()) {
            Toast.makeText(this, getString(R.string.location_off_for_scan), Toast.LENGTH_LONG).show();
            openLocationSettings();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        isScanCompleted = false;

        // 1) показати кешовані результати без toast
        @SuppressLint("MissingPermission") List<ScanResult> cachedResults = wifiManager.getScanResults();
        if (cachedResults != null && !cachedResults.isEmpty()) {
            updateWifiList(cachedResults, false); // false = не показувати toast
        }

        // 2) запустити реальний скан
        boolean started = wifiManager.startScan();
        if (!started) {
            scheduleRetry();
        } else {
            scheduleEmptyCheck();
        }
    }


    private void connectToWifi() {
        if (!isFineLocation) {
            checkWiFiPermissions();
        }
        if (!isFineLocation || !isLocationEnabled()) {
            Toast.makeText(this, getString(R.string.no_location_permission_connect), Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            WifiNetworkSpecifier.Builder builder = new WifiNetworkSpecifier.Builder()
                    .setSsid(pendingChosenSsid);
            WifiNetworkSpecifier wifiNetworkSpecifier = builder.build();
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(wifiNetworkSpecifier)
                    .build();
            connectToWifiQ(networkRequest);
        } else {
            connectToWifiLegacy();
        }
    }

    private void connectToWifiQ( NetworkRequest networkRequest) {
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                if (pendingChosenSsid != null) {
                    runOnUiThread(() -> {
                        WifiInfo currentWifiInfo = wifiManager.getConnectionInfo();
                        String connectedSsid = currentWifiInfo != null ? currentWifiInfo.getSSID().replace("\"", "") : null;
                        if (pendingChosenSsid.equals(connectedSsid)) {
                            Toast.makeText(WiFiSearchActivity.this, getString(R.string.connected_to, connectedSsid), Toast.LENGTH_SHORT).show();
                            wifiProvisionLauncher.launch(getIntentProvision (connectedSsid, currentWifiInfo));
                            returningFromProvision = true;
                            pendingChosenSsid = null;
                            connectivityManager.unregisterNetworkCallback(this);
                        }
                    });
                }
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                runOnUiThread(() -> Toast.makeText(WiFiSearchActivity.this, getString(R.string.connection_failed_toast), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                if (pendingChosenSsid != null) {
                    runOnUiThread(() -> Toast.makeText(WiFiSearchActivity.this, getString(R.string.connection_lost, pendingChosenSsid), Toast.LENGTH_LONG).show());
                }
            }
        };

        assert networkRequest != null;
        connectivityManager.requestNetwork(networkRequest, networkCallback);
    }

    private void connectToWifiLegacy() {
        String formattedSsid = String.format("\"%s\"", pendingChosenSsid);

        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = formattedSsid;
        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

        int netId = -1;
        @SuppressLint("MissingPermission") List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        if (configuredNetworks != null) {
            for (WifiConfiguration config : configuredNetworks) {
                if (config.SSID != null && config.SSID.equals(formattedSsid)) {
                    netId = config.networkId;
                    wifiManager.updateNetwork(wifiConfig);
                    break;
                }
            }
        }

        if (netId == -1) {
            netId = wifiManager.addNetwork(wifiConfig);
        }

        if (netId == -1) {
            Toast.makeText(this, getString(R.string.cannot_add_or_find_network), Toast.LENGTH_LONG).show();
            return;
        }

        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();

        registerNetworkCallback();
    }

    private void registerNetworkCallback() {
        if (networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }

        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                if (pendingChosenSsid != null) {
                    WifiInfo currentWifiInfo = wifiManager.getConnectionInfo();
                    String connectedSsid = currentWifiInfo != null ? currentWifiInfo.getSSID().replace("\"", "") : null;
                    if (pendingChosenSsid.equals(connectedSsid)) {
                        runOnUiThread(() -> {
                            Toast.makeText(WiFiSearchActivity.this, getString(R.string.connected_to, connectedSsid), Toast.LENGTH_SHORT).show();
                            wifiProvisionLauncher.launch(getIntentProvision(connectedSsid, currentWifiInfo));
                            returningFromProvision = true;
                            pendingChosenSsid = null;
                            if (networkCallback != null) {
                                connectivityManager.unregisterNetworkCallback(networkCallback);
                                networkCallback = null;
                            }
                        });
                    }
                }
            }


            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                if (pendingChosenSsid != null) {
                    runOnUiThread(() -> Toast.makeText(WiFiSearchActivity.this, getString(R.string.connection_lost, pendingChosenSsid), Toast.LENGTH_LONG).show());
                }
            }
        };
        connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
    }
    private void updateWifiList(List<ScanResult> scanResults, boolean showToast) {
        List<WifiNetwork> wifiNetworks = new ArrayList<>();
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String currentSsid = (wifiInfo != null) ? wifiInfo.getSSID().replace("\"", "") : null;

        if (currentSsidStart == null) {
            currentSsidStart = currentSsid;
        }

        HashMap<String, ScanResult> uniqueNetworks = new HashMap<>();
        for (ScanResult scanResult : scanResults) {
            if (isFilterEnabled && !scanResult.SSID.equals(currentSsid)
                    && !scanResult.SSID.toLowerCase().startsWith(wifiFilterSsid.toLowerCase())) {
                continue;
            }
            String key = !scanResult.SSID.isEmpty() ? scanResult.SSID : scanResult.BSSID;
            if (uniqueNetworks.containsKey(key)) {
                if (uniqueNetworks.get(key).level < scanResult.level) {
                    uniqueNetworks.put(key, scanResult);
                }
            } else {
                uniqueNetworks.put(key, scanResult);
            }
        }

        for (Map.Entry<String, ScanResult> entry : uniqueNetworks.entrySet()) {
            ScanResult sr = entry.getValue();
            boolean secured = sr.capabilities.contains("WEP")
                    || sr.capabilities.contains("WPA")
                    || sr.capabilities.contains("WPA2")
                    || sr.capabilities.contains("WPA3");
            wifiNetworks.add(new WifiNetwork(entry.getKey(), sr.BSSID, sr.BSSID, sr.level, sr.level,
                    entry.getKey().equals(currentSsid),
                    entry.getKey().equals(currentSsidStart),
                    secured));
        }

        adapter.setWifiNetworks(wifiNetworks);

        if (showToast) {
            Toast.makeText(this, getString(R.string.scan_completed), Toast.LENGTH_SHORT).show();
        }
    }


    private void scheduleRetry() {
        if (scanRetryCount < MAX_SCAN_RETRIES) {
            scanRetryCount++;
            handler.postDelayed(() -> {
                boolean started = wifiManager.startScan();
                if (!started) {
                    scheduleRetry();
                } else {
                    scheduleEmptyCheck();
                }
            }, RETRY_DELAY_MS);
        } else {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, getString(R.string.cannot_start_wifi_scan), Toast.LENGTH_SHORT).show();
        }
    }

    private void scheduleEmptyCheck() {
        handler.postDelayed(() -> {
            if (!isScanCompleted) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                List<ScanResult> results = wifiManager.getScanResults();
                if (results == null || results.isEmpty()) {
                    scheduleRetry();
                }
            }
        }, 1200);
    }

    private boolean isLocationEnabled() {
        if (locationManager == null) return true; // cannot check - assume enabled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return locationManager.isLocationEnabled();
        } else {
            return !locationManager.getProviders(true).isEmpty();
        }
    }

    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_set_filter_title);

        final EditText input = new EditText(this);
        input.setText(wifiFilterSsid);
        builder.setView(input);

        builder.setPositiveButton(R.string.button_ok, (dialog, which) -> {
            String newFilter = input.getText().toString();
            if (!newFilter.isEmpty()) {
                wifiFilterSsid = newFilter;
                saveFilterSettings(wifiFilterSsid, isFilterEnabled);

                Toast.makeText(this, getString(R.string.filter_updated_toast, newFilter), Toast.LENGTH_SHORT).show();
                restartWifiScan();
            } else {
                Toast.makeText(this, R.string.filter_cannot_be_empty, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void handleWifiSettingsResult(ActivityResult result) {
        if (result.getResultCode() == RESULT_OK) {
            Intent data = result.getData();
            if (data != null) {
                // Return currentSsidStart with result
                String returnedSsid = data.getStringExtra(CURRENT_SSID_START_TEXT);
                if (currentSsidStart == null) {
                    currentSsidStart = returnedSsid;
                }
                returningFromProvision = true;
            }
        }
    }

    private void resetFilterToDefault() {
        wifiFilterSsid = WIFI_FILTER_SSID_DEF;
        isFilterEnabled = WIFI_FILTER_ENABLED_DEF;
        // Save default settings to SharedPreferences
        saveFilterSettings(wifiFilterSsid,  isFilterEnabled);
    }

    private void openWifiSettings() {
        returningFromWifiSettings = true;
        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        startActivity(intent);
    }

    private void openLocationSettings() {
        returningFromLocationSettings = true;
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    private Intent getIntentProvision(String chosenSsid, WifiInfo currentWifiInfo) {
        Intent intent = new Intent(WiFiSearchActivity.this, WiFiProvisionActivity.class);
        intent.putExtra(CHOSEN_SSID_TEXT, chosenSsid);
        intent.putExtra(CHOSEN_BSSID_TEXT, currentWifiInfo.getBSSID());
        intent.putExtra(CURRENT_SSID_START_TEXT, currentSsidStart);
        return intent;
    }

}
