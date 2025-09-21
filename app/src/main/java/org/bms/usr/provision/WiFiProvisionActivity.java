package org.bms.usr.provision;

import static org.bms.usr.provision.BmsCommandType.CMD_GET_WIFI_LIST;
import static org.bms.usr.provision.BmsCommandType.CMD_UPDATE_SETTINGS;
import static org.bms.usr.provision.CodecBmsProvision.decodeResponse;
import static org.bms.usr.provision.CodecBmsProvision.getCommand;
import static org.bms.usr.provision.HelperBmsProvision.CHOSEN_BSSID_TEXT;
import static org.bms.usr.provision.HelperBmsProvision.CHOSEN_SSID_TEXT;
import static org.bms.usr.provision.HelperBmsProvision.CURRENT_SSID_START_TEXT;
import static org.bms.usr.settings.HelperBmsSettings.addOrUpdateBmsWifiEntry;

import androidx.activity.OnBackPressedCallback;
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
import org.bms.usr.transport.ResultSendCommand;
import org.bms.usr.transport.UdpClient;
import org.bms.usr.service.WifiListAdapter;
import org.bms.usr.service.WifiNetwork;
import org.bms.usr.transport.WiFiBmsListener;

import java.util.ArrayList;
import java.util.List;

public class WiFiProvisionActivity extends AppCompatActivity implements WifiListAdapter.OnItemClickListener {


    private EditText editTextSsid;
    private EditText editTextPassword;
    private Button buttonOk;

    private WifiListAdapter bmsNetworksAdapter;
//    private List<WifiNetwork> bmsNetworksList;
    private UdpClient udpClient;

    private ProgressBar progressBar;

    private String currentSsidStart;
    private String connectedSsid;
    private String connectedBSsid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_provision);

        TextView textViewConnectedTo = findViewById(R.id.textViewConnectedToSsid);
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
            startSendCommand(CMD_UPDATE_SETTINGS, ssid, password);
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
        bmsNetworksAdapter = new WifiListAdapter(this, this);
        recyclerViewBmsNetworks.setAdapter(bmsNetworksAdapter);

        connectedSsid = getIntent().getStringExtra(CHOSEN_SSID_TEXT);
        connectedBSsid = getIntent().getStringExtra(CHOSEN_BSSID_TEXT);
        currentSsidStart = getIntent().getStringExtra(CURRENT_SSID_START_TEXT);
        textViewConnectedTo.setText(connectedSsid);

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
                        !editTextPassword.getText().toString().isEmpty();
                // Activate/deactivate the OK button based on the check
                buttonOk.setEnabled(isReady);
            }
        };

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
//                    bmsNetworksList.addAll(networksFromBms);
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
                addOrUpdateBmsWifiEntry(connectedBSsid, connectedSsid);
                break;
            case RSP_ERRORS:
                errorSendCommand (decodeResult.messageError());
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
}