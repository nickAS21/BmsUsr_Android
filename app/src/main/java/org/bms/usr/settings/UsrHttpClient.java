package org.bms.usr.settings;

import static org.bms.usr.settings.HelperBmsSettings.NET_A_PORT_DEF;
import static org.bms.usr.settings.HelperBmsSettings.SOCKET_B_PORT_DEF;
import static org.bms.usr.settings.HelperBmsSettings.getNetA_Ip;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.APPLICATION;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.APPLY;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.APP_CONFIG_HTML;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.AP_CLI_ENABLE;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.AP_STA_ENABLE;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.AUTH;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.AUTHORIZATION_KEY;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.CLIENT;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.COUNTRY_CODE;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.DATA_TRANSFOR_MODE;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.NETB_IP;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.NETB_MODE;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.NETB_PORT;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.NET_IP;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.NET_MODE;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.NET_PORT;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.OP_MODE_HTML;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.SET1;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.SET2;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.SET3;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.SET4;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.SET5;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.SET_URL;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.CMD;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.CONTENT_TYPE;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.CONTENT_TYPE_KEY;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.GO;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.LAN;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.ON;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.RESPONSE_BODY_HTTP_ERROR;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.RESPONSE_BODY_OK;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.SET0;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.STA;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.STA_CONFIG_HTML;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.SYS_OPMODE;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.WIFI_MODE;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.WIRELESS_BASIC;
import static org.bms.usr.settings.HelperBmsSettingsHttpClient.WLAN_CLINUM;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import androidx.annotation.NonNull;

import java.io.IOException;

public class UsrHttpClient {

    private final OkHttpClient client = new OkHttpClient();
    private String connectedNetAClientIpToServer;
    private String connectedToId;

    public interface Callback {
        void onSuccess(String response);
        void onError(String error);
    }

    /**
     * 1)
     * CMD=WIRELESS_BASIC&GO=opmode.html&SET0=81002752%3D1&SET1=288162304%3DSTA&SET2=75038976%3D5&SET3=18088192%3D2&SET4=285278720%3D0
     * SET0 81002752=1		=> addCfg('apcli_enable',0x04d40100,'1');	 // STA mode = 1 AP mode = 0
     * SET1 288162304=STA	=> addCfg('wifi_mode',0x112d0200,'STA');
     * SET2 75038976=5		=> addCfg('country_code',0x04790100,'5');
     * SET3 18088192=2		=> addCfg('sys_opmode',0x01140100,'2');		 // STA mode = 2 AP mode = 1
     * SET4 285278720=0	=> addCfg('Data_Transfor_Mode',0x11010200,'0');	 // Transparent mode = 0; Serial Comand Mode = 1; GPIO Mode = 3; HTTP Mode = 4; Modbus TCP<=>Modbus RTU = 5;
     */
    public void postStaMode(Callback callback) {
        FormBody formBody = new FormBody.Builder()
                .add(CMD, WIRELESS_BASIC)
                .add(GO, OP_MODE_HTML)
                .add(SET0, AP_CLI_ENABLE + "=" + 1)  // STA mode = 1 AP mode = 0
                .add(SET1, WIFI_MODE + "=" + STA)
                .add(SET2, COUNTRY_CODE + "=" + 5)
                .add(SET3, SYS_OPMODE + "=" + 2)     // STA mode = 2 AP mode = 1
                .add(SET4, DATA_TRANSFOR_MODE + "=" + 0)     // Transparent mode = 0; Serial Comand Mode = 1; GPIO Mode = 3; HTTP Mode = 4; Modbus TCP<=>Modbus RTU = 5;
                .build();

        postRequest(callback, formBody, SET_URL);
    }

    /**
     * 2)
     * CMD=LAN&GO=sta_config.html&SET0=304677376=on&SET0=304677376=on&SET1=303104512%3D100
     *
     * SET0 304677376=on   => addCfg('apsta_en',0x12290200,'on');
     * SET1 303104512=90   => addCfg('wlan_clinum',0x12110200,'100'); Якщо значення сигналу встановлено на 100, модуль не перемикатиме мережу. Навіть якщо поточний сигнал мережі не відповідає поточній мережі, вона завжди буде шукати, не перевантажуючи інші мережі.
     */
    public void postApStaOn(Callback callback) {
        FormBody formBody = new FormBody.Builder()
                .add(CMD, LAN)
                .add(GO, STA_CONFIG_HTML)
                .add(SET0, AP_STA_ENABLE + "=" + ON)    // SET0: {"304677376": "on"}
                .add(SET1, WLAN_CLINUM + "=" + 100)     // SET1: {"303104512": "100"}
                .build();

        postRequest(callback, formBody, SET_URL);
    }

    /**
     * 3)
     * CMD=Application&GO=app_config.html&SET0=285999616=client&SET1=286064896%3D8901&SET2=286130688%3D192.168.8.119&SET3=286392576%3D18901&SET4=286458368%3D192.168.8.119&SET5=C286327040%3D1
     * SET0 285999616=client			=> addCfg('net_mode',0x110c0200,'client'); / addCfg('net_mode',0x110c0200,'server');
     * SET1 286064896=8901			=> addCfg('net_port',0x110d0100,'8901');
     * SET2 286130688=192.168.8.119		=> addCfg('net_ip',0x110e0200,'192.168.8.119');
     * SET3 286392576=18901			=> addCfg('netb_port',0x11120100,'18901');
     * SET4 286458368=192.168.8.119		=> addCfg('netb_ip',0x11130200,'192.168.8.119');
     * SET5 286327040=1			=> addCfg('netb_mode',0x11110100,'1'); 	// Socket B Setting off = 0; on = 1
     */
    public void postAppSetting(Callback callback) {
        int netPort = NET_A_PORT_DEF + Integer.parseInt(this.connectedToId);
        int netBPort = SOCKET_B_PORT_DEF + Integer.parseInt(this.connectedToId);
        FormBody formBody = new FormBody.Builder()
                .add(CMD, APPLICATION)
                .add(GO, APP_CONFIG_HTML)
                .add(SET0, NET_MODE+ "=" + CLIENT)                              // SET0 285999616=client
                .add(SET1, NET_PORT + "=" + netPort)                            // SET1 286064896=8901
                .add(SET2, NET_IP + "=" + this.getNetworkAClientIpToServer())   // SET2 286130688=192.168.8.119
                .add(SET3, NETB_MODE + "=" + 1)                                 // SET5 286327040=1
                .add(SET4, NETB_PORT + "=" + netBPort)                          // SET3 286392576=18901
                .add(SET5, NETB_IP + "=" + this.getNetworkAClientIpToServer())  // SET4 286458368=192.168.8.119
                .build();

        postRequest(callback, formBody, SET_URL);
    }

    public void postApply(Callback callback) {
        FormBody formBody = new FormBody.Builder()
                .add(CMD, APPLY)
                .build();

        postRequest(callback, formBody, SET_URL);
    }

    public void postRequest(Callback callback, FormBody formBody, String url){
        Request request = new Request.Builder()
                .url(url)
                .addHeader(AUTHORIZATION_KEY, AUTH)
                .addHeader(CONTENT_TYPE_KEY, CONTENT_TYPE)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    if (response.isSuccessful()) {
                        callback.onSuccess(response.body() != null ? response.body().string() : RESPONSE_BODY_OK);
                    } else {
                        callback.onError( RESPONSE_BODY_HTTP_ERROR + response.code());
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void setNetworkAClientIpToServer(String ipTpServer) {
        this.connectedNetAClientIpToServer = ipTpServer;
    }

    public void setConnectedToId(String id) {
        this.connectedToId = id;
    }

    private String getNetworkAClientIpToServer(){
        if (this.connectedNetAClientIpToServer == null){
            this.connectedNetAClientIpToServer = getNetA_Ip();
        }
        return this.connectedNetAClientIpToServer;
    }
}
