package org.bms.bmsusrprovision;

import static org.bms.bmsusrprovision.service.CodecBms.decodeResponse;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
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
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.bms.bmsusrprovision.service.WifiListAdapter;
import org.bms.bmsusrprovision.service.WifiNetwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements WifiListAdapter.OnItemClickListener {

    private static final int PERMISSIONS_REQUEST_CODE = 1001;
    private RecyclerView recyclerView;
    private WifiListAdapter adapter;
    private List<WifiNetwork> wifiNetworks = new ArrayList<>();
    private Button buttonRescan;
    private WifiManager wifiManager;
    private WifiScanReceiver wifiScanReceiver;
    private ProgressBar progressBar;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isReceiverRegistered = false;
    private boolean isScanCompleted = false;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private String targetSsidToConnect = null;

    // --- added for robust scanning retries and location check
    private static final int MAX_SCAN_RETRIES = 3;
    private int scanRetryCount = 0;
    private static final long RETRY_DELAY_MS = 1000;
    private LocationManager locationManager;

    private String pendingChosenSsid = null;
    private String currentSsidStart = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progressBar);

        recyclerView = findViewById(R.id.recyclerViewWifiList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new WifiListAdapter(wifiNetworks, this);
        recyclerView.setAdapter(adapter);

        buttonRescan = findViewById(R.id.buttonRescan);
        buttonRescan.setOnClickListener(v -> checkPermissionsAndScan());

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiScanReceiver = new WifiScanReceiver();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // ensure receiver registered before any first scan (avoids race)
        if (!isReceiverRegistered) {
            registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            isReceiverRegistered = true;
        }

        // slight delay to let system components settle, then start permission check + scan
        handler.postDelayed(this::checkPermissionsAndScan, 500);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isReceiverRegistered) {
            registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            isReceiverRegistered = true;
        }
        if (pendingChosenSsid != null) {
            //перевіряємо успіх підключення до обраної мережі
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                String currentSsid = wifiInfo.getSSID();
                if (currentSsid != null && currentSsid.length() > 2) {
                    currentSsid = currentSsid.replace("\"", "");
                    if (pendingChosenSsid.equals(currentSsid)) {
                        Intent intent = new Intent(this, WifiSettingsActivity.class);
                        intent.putExtra("CHOSEN_SSID", pendingChosenSsid);
                        intent.putExtra("CURRENT_PHONE_SSID", currentSsidStart);
                        pendingChosenSsid = null;
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Підключення до " + pendingChosenSsid + " не виконано.", Toast.LENGTH_SHORT).show();
                        startWifiScan();
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
        handler.removeCallbacksAndMessages(null); // Зупиняємо таймер
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // При необхідності, тут можна закривати сокети або інші ресурси
    }

    private void checkPermissionsAndScan() {
        if (!isReceiverRegistered) {
            registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            isReceiverRegistered = true;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startWifiScan();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_CODE);
        }
    }

    private void startWifiScan() {
        // TODO

//        String hexData = "FF016C811B31343133413037534C444F504730303930303100640D0A31343133413037534C444F504730303930303100640D0A513030333334313030323032383400640D0A6C6562656400640D0A31343133413037534C444F504730303930303100640D0A31343133413037534C444F504730303930303100640D0A31343133413037534C444F504730303930303100600D0A4B6F766C617232472D31002C0D0A002A0D0A4D696B726F54696B2D39353733424200220D0A6E657469735F322E34475F43424433454300180D0A4761726275686100170D0A54656E64615F7769666900180D0A4B76313800140D0A00140D0A4A61636B000F0D0A000D0D0A456C697A6162657468000D0D0A70617061000D0D0A000D0D0A4D696B726F54696B2D364135444638000D0D0A486F6D652057692D4669000D0D0A54502D4C696E6B5F32454142000A0D0A4D6164726964000A0D0A3232316100050D0A3931316200050D0A464946492D5749464900020D0AA8";
//        Map<String, Object> result =  decodeResponse(hexData);
        if (wifiManager == null) {
            Toast.makeText(this, "WiFiManager не ініціалізовано", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "Wi-Fi вимкнено. Увімкніть його для сканування.", Toast.LENGTH_LONG).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_CODE);
            return;
        }
        if (!isLocationEnabled()) {
            Toast.makeText(this, "Увімкніть служби локації (Location) для сканування Wi-Fi", Toast.LENGTH_LONG).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        isScanCompleted = false;
        scanRetryCount = 0;

        boolean started = wifiManager.startScan();
        if (!started) {
            scheduleRetry();
        } else {
            scheduleEmptyCheck();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startWifiScan();
            } else {
                Toast.makeText(this, "Дозвіл на розташування відхилено. Сканування Wi-Fi неможливе.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onItemClick(WifiNetwork network) {
        WifiInfo currentWifiInfo = wifiManager.getConnectionInfo();
        String currentSsid = (currentWifiInfo != null) ? currentWifiInfo.getSSID().replace("\"", "") : null;
        pendingChosenSsid = network.getSsid();
        if (pendingChosenSsid.equals(currentSsid)) {
            // 🔹 Вибрали ту ж мережу → одразу в WifiSettingsActivity
            Intent intent = new Intent(this, WifiSettingsActivity.class);
            intent.putExtra("CHOSEN_SSID", pendingChosenSsid);
            intent.putExtra("CURRENT_PHONE_SSID", currentSsid);
            pendingChosenSsid = null;
            startActivity(intent);

        } else {
            // 🔹 Вибрали нову мережу → зберігаємо і чекаємо підтвердження підключення
            Toast.makeText(this, "Підключення до " + pendingChosenSsid + "...", Toast.LENGTH_SHORT).show();
            connectToWifi();
        }
    }

    private void connectToWifi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Немає дозволу на розташування. Неможливо підключитися.", Toast.LENGTH_LONG).show();
                return;
            }
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
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        String connectedSsid = wifiInfo != null ? wifiInfo.getSSID().replace("\"", "") : null;
                        if (pendingChosenSsid.equals(connectedSsid)) {
                            Toast.makeText(MainActivity.this, "Підключено до " + pendingChosenSsid, Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(MainActivity.this, WifiSettingsActivity.class);
                            intent.putExtra("CHOSEN_SSID", connectedSsid);
                            intent.putExtra("CURRENT_PHONE_SSID", connectedSsid);
                            pendingChosenSsid = null;
                            startActivity(intent);
                            connectivityManager.unregisterNetworkCallback(this);
                        }
                    });
                }
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Підключення не вдалося.", Toast.LENGTH_LONG).show());
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                if (pendingChosenSsid != null) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Підключення до " + pendingChosenSsid + " втрачено.", Toast.LENGTH_LONG).show());
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Немає дозволу на розташування. Неможливо підключитися.", Toast.LENGTH_LONG).show();
            return;
        }
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
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
            Toast.makeText(this, "Не вдалося додати або знайти мережу.", Toast.LENGTH_LONG).show();
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
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    String connectedSsid = wifiInfo != null ? wifiInfo.getSSID().replace("\"", "") : null;
                    if (pendingChosenSsid.equals(connectedSsid)) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Підключено до " + connectedSsid, Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(MainActivity.this, WifiSettingsActivity.class);
                            intent.putExtra("CHOSEN_SSID", connectedSsid);
                            intent.putExtra("CURRENT_PHONE_SSID", connectedSsid);
                            pendingChosenSsid = null;
                            startActivity(intent);
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
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Підключення до " + pendingChosenSsid + " не вдалося.", Toast.LENGTH_LONG).show());
                }
            }
        };
        connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
    }

    class WifiScanReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            if (ActivityCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            List<ScanResult> results = wifiManager.getScanResults();
            if (results != null && !results.isEmpty()) {
                isScanCompleted = true;
                progressBar.setVisibility(View.GONE);
                handler.removeCallbacksAndMessages(null);
                updateWifiList(results);
            } else {
                boolean extra = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, true);
                if (!extra || results == null || results.isEmpty()) {
                    if (scanRetryCount < MAX_SCAN_RETRIES) {
                        scanRetryCount++;
                        handler.postDelayed(() -> wifiManager.startScan(), RETRY_DELAY_MS * scanRetryCount);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Сканування не вдалося.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    updateWifiList(results);
                }
            }
        }
    }

    private void updateWifiList(List<ScanResult> scanResults) {
        wifiNetworks.clear();
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String currentBssid = (wifiInfo != null) ? wifiInfo.getBSSID() : null;
        String currentSsid = (wifiInfo != null) ? wifiInfo.getSSID().replace("\"", "") : null;

        if (currentSsid != null && currentSsid.startsWith("\"") && currentSsid.endsWith("\"")) {
            currentSsid = currentSsid.substring(1, currentSsid.length() - 1);
        }

        if (!isReceiverRegistered) {
            currentSsidStart = currentSsid;
        }

        HashMap<String, ScanResult> uniqueNetworks = new HashMap<>();
        for (ScanResult scanResult : scanResults) {
            String key = scanResult.SSID.getBytes().length != 0 ? scanResult.SSID : scanResult.BSSID;
            if (uniqueNetworks.containsKey(key)) {
                if (uniqueNetworks.get(key).level < scanResult.level) {
                    uniqueNetworks.put(key, scanResult);
                }
            } else {
                uniqueNetworks.put(key, scanResult);
            }
        }

        for (ScanResult scanResult : uniqueNetworks.values()) {
            boolean isCurrentNetwork = false;
            if (scanResult.BSSID != null && scanResult.BSSID.equals(currentBssid)) {
                isCurrentNetwork = true;
            } else if (scanResult.SSID != null && scanResult.SSID.equals(currentSsid)) {
                isCurrentNetwork = true;
            }

            boolean secured = scanResult.capabilities.contains("WEP")
                    || scanResult.capabilities.contains("WPA")
                    || scanResult.capabilities.contains("WPA2")
                    || scanResult.capabilities.contains("WPA3");

            wifiNetworks.add(new WifiNetwork(scanResult.SSID, scanResult.BSSID, scanResult.level, isCurrentNetwork, secured));
        }

        Collections.sort(wifiNetworks, (a, b) -> Boolean.compare(b.isCurrent(), a.isCurrent()));

        adapter.setWifiNetworks(wifiNetworks);
        Toast.makeText(this, "Сканування завершено.", Toast.LENGTH_SHORT).show();
    }

    private void scheduleRetry() {
        if (scanRetryCount < MAX_SCAN_RETRIES) {
            scanRetryCount++;
            handler.postDelayed(() -> {
                boolean started = wifiManager.startScan();
                if (!started && scanRetryCount < MAX_SCAN_RETRIES) {
                    scheduleRetry();
                } else {
                    scheduleEmptyCheck();
                }
            }, RETRY_DELAY_MS * scanRetryCount);
        } else {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Не вдалося почати сканування Wi-Fi", Toast.LENGTH_SHORT).show();
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
        if (locationManager == null) return true; // не можемо перевірити — припускаємо ввімкнено
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return locationManager.isLocationEnabled();
        } else {
            return !locationManager.getProviders(true).isEmpty();
        }
    }

}