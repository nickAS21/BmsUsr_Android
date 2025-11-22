package org.bms.usr.provision;

import static org.bms.usr.provision.BmsCommandType.CMD_GET_WIFI_LIST;
import static org.bms.usr.provision.BmsCommandType.CMD_UPDATE_SETTINGS;
import static org.bms.usr.provision.CodecBmsProvision.decodeResponse;
import static org.bms.usr.provision.CodecBmsProvision.getCommand;
import static org.bms.usr.provision.HelperBmsProvision.CHOSEN_BSSID_TEXT;
import static org.bms.usr.provision.HelperBmsProvision.CHOSEN_SSID_TEXT;
import static org.bms.usr.provision.HelperBmsProvision.CURRENT_SSID_START_TEXT;
import static org.bms.usr.settings.HelperBmsSettings.addOrUpdateBmsWifiEntry;
import static org.bms.usr.settings.HelperBmsSettings.getNetA_Ip;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.bms.usr.R;
import org.bms.usr.settings.RequestChain;
import org.bms.usr.settings.UsrHttpClient;
import org.bms.usr.transport.ResultSendCommand;
import org.bms.usr.transport.UdpClient;
import org.bms.usr.service.WifiListAdapter;
import org.bms.usr.service.WifiNetwork;
import org.bms.usr.transport.WiFiBmsListener;

import java.util.ArrayList;
import java.util.List;

public class WiFiProvisionActivity extends AppCompatActivity implements WifiListAdapter.OnItemClickListener {

    private EditText editTextId;
    private EditText editTextIp;
    private EditText editTextSsid;
    private EditText editTextPassword;
    private Button buttonOk;

    private WifiListAdapter bmsNetworksAdapter;
    private UdpClient udpClient;

    private ProgressBar progressBar;

    private String currentSsidStart;
    private String connectedSsid;
    private String connectedToId;
    private String connectedNetAClientIpToServer;
    private String connectedBSsid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_provision);

        TextView textViewConnectedTo = findViewById(R.id.textViewConnectedToSsid);
        editTextId = findViewById(R.id.textViewConnectedToId);
        editTextIp = findViewById(R.id.textViewConnectedIp);
        editTextSsid = findViewById(R.id.editTextSsid);
        editTextPassword = findViewById(R.id.editTextPassword);

        buttonOk = findViewById(R.id.buttonOk);
        buttonOk.setEnabled(false);
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        ImageButton buttonScan = findViewById(R.id.buttonScan);
        buttonOk.setOnClickListener(v -> {
            String ssid = editTextSsid.getText().toString();
            String password = editTextPassword.getText().toString();

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
            sendSettingsBeforeProvision(ssid, password);
        });
        buttonBack.setOnClickListener(v -> closeActivity());
        buttonScan.setOnClickListener(v -> {
            if (udpClient != null) {
                startSendCommand(CMD_GET_WIFI_LIST);
            }
        });

        RecyclerView recyclerViewBmsNetworks = findViewById(R.id.recyclerViewBmsNetworks);
        recyclerViewBmsNetworks.setLayoutManager(new LinearLayoutManager(this));
        progressBar = findViewById(R.id.progressBar);
        bmsNetworksAdapter = new WifiListAdapter(this, this, null, false);
        recyclerViewBmsNetworks.setAdapter(bmsNetworksAdapter);

        connectedSsid = getIntent().getStringExtra(CHOSEN_SSID_TEXT);
        connectedBSsid = getIntent().getStringExtra(CHOSEN_BSSID_TEXT);
        currentSsidStart = getIntent().getStringExtra(CURRENT_SSID_START_TEXT);
        connectedToId = null;
        connectedNetAClientIpToServer = getNetA_Ip();
        textViewConnectedTo.setText(connectedSsid);
        editTextId.setText(connectedToId);
        editTextIp.setText(connectedNetAClientIpToServer);

        // Add TextWatcher to both input fields
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Check if both fields are not empty
                boolean isReady = !editTextSsid.getText().toString().isEmpty() &&
                        !editTextPassword.getText().toString().isEmpty() &&
                        !editTextId.getText().toString().isEmpty();
//                        !editTextIp.getText().toString().isEmpty();
                // Activate/deactivate the OK button based on the check
                buttonOk.setEnabled(isReady);
                connectedToId = editTextId.getText().toString();
                connectedNetAClientIpToServer = editTextIp.getText().toString();
            }
        };

        editTextId.addTextChangedListener(textWatcher);
        editTextIp.addTextChangedListener(textWatcher);
        editTextSsid.addTextChangedListener(textWatcher);
        editTextPassword.addTextChangedListener(textWatcher);

        // Create and register OnBackPressedCallback
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                closeActivity();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        // Initialize client and start scan
        udpClient = new UdpClient(this, new WiFiBmsListener() {
            @Override
            public void onDataReceived(byte[] ssids) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    ResultSendCommand<Object> decodeResult = decodeResponse(ssids);
                    responseSendCommand(decodeResult);
                });
            }

            @Override
            public void onError(String message) {
                errorSendCommand (message);
            }
        });
        startSendCommand (CMD_GET_WIFI_LIST);
    }

    @Override
    public void onItemClick(WifiNetwork network) {
        editTextSsid.setText(network.getSsid());
    }

    private void startSendCommand(BmsCommandType bmsCommandType, String... parameters) {
        udpClient.startSendCommand(getCommand (bmsCommandType, parameters));
        // Show ProgressBar
        progressBar.setVisibility(View.VISIBLE);
    }

    private <T> void responseSendCommand(ResultSendCommand<T> decodeResult) {
        BmsCommandType type = decodeResult.type();
        switch (type) {
            case RSP_WIFI_LIST: // WiFi list
                List<WifiNetwork> networksFromBms = (List<WifiNetwork>) decodeResult.payload();
                if (networksFromBms != null) {
//                    bmsNetworksList.clear();
                    for (WifiNetwork network : networksFromBms) {
                        if (network.getSsid().equals(currentSsidStart)) {
                            // Set green check mark
                            network.setIsdCurrentSsidStart(true);
                            break;
                        }
                    }
                    // bmsNetworksList.addAll(networksFromBms);
                    bmsNetworksAdapter.setWifiNetworks(networksFromBms);
                    Toast.makeText(WiFiProvisionActivity.this, getString(R.string.bms_networks_updated), Toast.LENGTH_SHORT).show();
                } else {
                    bmsNetworksAdapter.setWifiNetworks(new ArrayList<>());
                    errorSendCommand (getString(R.string.bms_networks_updated_zero));
                }

                break;
            case RSP_UPDATE_SETTINGS: // configuration saved result ok
                Toast.makeText(WiFiProvisionActivity.this, getString(R.string.update_settings_success), Toast.LENGTH_SHORT).show();
                // === UPDATE MAP IN SHARED PREFERENCES ===
                addOrUpdateBmsWifiEntry(Integer.parseInt(connectedToId), connectedSsid, null,  connectedBSsid, connectedNetAClientIpToServer);
                break;
            case RSP_ERRORS:
                errorSendCommand (decodeResult.messageError());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (udpClient != null) {
            udpClient.closeSocket();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (udpClient != null) {
            udpClient.closeSocket();
        }
    }

    private void errorSendCommand (String message) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(WiFiProvisionActivity.this, getString(R.string.error_with_message, message), Toast.LENGTH_LONG).show();
        });
    }
    private void closeActivity() {
        if (udpClient != null) {
            udpClient.closeSocket();
        }
        Intent resultIntent = new Intent();
        resultIntent.putExtra(CURRENT_SSID_START_TEXT, currentSsidStart);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
    public void sendSettingsBeforeProvision(String ssid, String password) {
        UsrHttpClient http = new UsrHttpClient();
        http.setNetworkAClientIpToServer(this.connectedNetAClientIpToServer);
        http.setConnectedToId(this.connectedToId);

        RequestChain chain = new RequestChain(
            () -> runOnUiThread(() -> {
                    Toast.makeText(WiFiProvisionActivity.this, getString(R.string.update_settings_sta_success), Toast.LENGTH_SHORT).show();
                    startSendCommand(CMD_UPDATE_SETTINGS, ssid, password);
                }
            ),
            error -> runOnUiThread(() -> new AlertDialog.Builder(WiFiProvisionActivity.this)
                .setTitle(getString(R.string.error_with_message, error))
                .setMessage(getString(R.string.update_settings_sta_error_continue))
                .setPositiveButton(getString(R.string.update_settings_sta_error_continue_yes), (dialog, which) -> {
                    startSendCommand(CMD_UPDATE_SETTINGS, ssid, password);
                })
                .setNegativeButton(getString(R.string.update_settings_sta_error_continue_no), (dialog, which) -> {
                    // ► We do nothing - the user stopped
                })
                .setCancelable(true)
                .show()
            )
        );

        chain.add(() -> http.postStaMode(chain.wrap(new UsrHttpClient.Callback() {
            @Override public void onSuccess(String r) {}
            @Override public void onError(String e) {}
        })));

        chain.add(() -> http.postApStaOn(chain.wrap(new UsrHttpClient.Callback() {
            @Override public void onSuccess(String r) {}
            @Override public void onError(String e) {}
        })));

        chain.add(() -> http.postAppSetting(chain.wrap(new UsrHttpClient.Callback() {
            @Override public void onSuccess(String r) {}
            @Override public void onError(String e) {}
        })));

        chain.start();
    }
}