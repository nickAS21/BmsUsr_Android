package org.bms.bmsusrprovision;

import static org.bms.bmsusrprovision.service.BmsCommandType.CMD_GET_WIFI_LIST;
import static org.bms.bmsusrprovision.service.BmsCommandType.CMD_UPDATE_SETTINGS;
import static org.bms.bmsusrprovision.service.CodecBms.decodeResponse;
import static org.bms.bmsusrprovision.service.CodecBms.decodeSettingResponse;
import static org.bms.bmsusrprovision.service.CodecBms.getCommand;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
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

import org.bms.bmsusrprovision.service.BmsCommandType;
import org.bms.bmsusrprovision.service.ResultSendCommand;
import org.bms.bmsusrprovision.service.UdpClient;
import org.bms.bmsusrprovision.service.WifiListAdapter;
import org.bms.bmsusrprovision.service.WifiNetwork;

import java.util.ArrayList;
import java.util.List;

public class WifiSettingsActivity extends AppCompatActivity {

    private TextView textViewConnectedTo;
    private EditText editTextSsid;
    private EditText editTextPassword;
    private Button buttonOk;
    private ImageButton buttonBack;

    private ImageButton buttonScan;
    private RecyclerView recyclerViewBmsNetworks;
    private WifiListAdapter bmsNetworksAdapter;
    private List<WifiNetwork> bmsNetworksList = new ArrayList<>();
    private UdpClient udpClient;

    private ProgressBar progressBar;

    private static String connectedSsid;
    private static String currentSsidStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_settings);

        textViewConnectedTo = findViewById(R.id.textViewConnectedToSsid);
        editTextSsid = findViewById(R.id.editTextSsid);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonOk = findViewById(R.id.buttonOk);
        buttonOk.setEnabled(false);
        buttonBack = findViewById(R.id.buttonBack);
        buttonScan = findViewById(R.id.buttonScan);
        recyclerViewBmsNetworks = findViewById(R.id.recyclerViewBmsNetworks);
        recyclerViewBmsNetworks.setLayoutManager(new LinearLayoutManager(this));
        progressBar = findViewById(R.id.progressBar);

        bmsNetworksAdapter = new WifiListAdapter(bmsNetworksList, null);
        recyclerViewBmsNetworks.setAdapter(bmsNetworksAdapter);

        connectedSsid = getIntent().getStringExtra("CHOSEN_SSID");
        currentSsidStart = getIntent().getStringExtra("CURRENT_PHONE_SSID");
        if (connectedSsid != null) {
            textViewConnectedTo.setText(connectedSsid);
        }

        // Додаємо TextWatcher до обох полів вводу
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Перевіряємо, чи обидва поля не порожні
                boolean isReady = !editTextSsid.getText().toString().isEmpty() &&
                        !editTextPassword.getText().toString().isEmpty();
                // Активуємо/деактивуємо кнопку OK залежно від перевірки
                buttonOk.setEnabled(isReady);
            }
        };

        editTextSsid.addTextChangedListener(textWatcher);
        editTextPassword.addTextChangedListener(textWatcher);

        buttonOk.setOnClickListener(v -> {
            String ssid = editTextSsid.getText().toString();
            String password = editTextPassword.getText().toString();

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
            startSendCommand(CMD_UPDATE_SETTINGS, ssid, password);
//            }
        });

        buttonBack.setOnClickListener(v -> closeActivity());
        buttonScan.setOnClickListener(v -> {
            if (udpClient != null) {
                startSendCommand(CMD_GET_WIFI_LIST);
            }
        });

        // Створюємо та реєструємо OnBackPressedCallback
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                closeActivity();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        // Ініціалізація клієнта та запуск сканування
        udpClient = new UdpClient(this, new UdpClient.UdpListener() {
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

    private void startSendCommand(BmsCommandType bmsCommandType, String... parameters) {
        udpClient.startSendCommand(getCommand (bmsCommandType, parameters));
        progressBar.setVisibility(View.VISIBLE); // Показуємо ProgressBar
    }

    private <T> void responseSendCommand(ResultSendCommand<T> decodeResult) {
        BmsCommandType type = decodeResult.type();
        switch (type) {
            case RSP_WIFI_LIST: // список WiFi
                List<WifiNetwork> networksFromBms = (List<WifiNetwork>) decodeResult.payload();
                if (networksFromBms != null) {
                    bmsNetworksList.clear();
                    for (WifiNetwork network : networksFromBms) {
                        if (network.getSsid().equals(currentSsidStart)) {
                            network.setIsCurrent(true); // Встановлюємо зелену галочку
                            break;
                        }
                    }
                    bmsNetworksList.addAll(networksFromBms);
                    bmsNetworksAdapter.setWifiNetworks(bmsNetworksList);
                    Toast.makeText(WifiSettingsActivity.this, "Список мереж від BMS оновлено.", Toast.LENGTH_SHORT).show();
                }
                bmsNetworksList = (List<WifiNetwork>) decodeResult.payload();
                bmsNetworksAdapter.setWifiNetworks(bmsNetworksList);
                Toast.makeText(WifiSettingsActivity.this, "Список мереж від BMS оновлено.", Toast.LENGTH_SHORT).show();
                break;
            case RSP_UPDATE_SETTINGS: // результат конфігурації
                Toast.makeText(WifiSettingsActivity.this, "Налаштування BMS WiFi на обранк мережу - оновлено.", Toast.LENGTH_SHORT).show();
                break;
            case RSP_ERRORS:
                errorSendCommand (decodeResult.messageError());
        }
    }

    private void errorSendCommand (String message) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(WifiSettingsActivity.this, "Помилка: " + message, Toast.LENGTH_LONG).show();
        });
    }
    private void closeActivity() {
        if (udpClient != null) {
            udpClient.closeSocket();
        }
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