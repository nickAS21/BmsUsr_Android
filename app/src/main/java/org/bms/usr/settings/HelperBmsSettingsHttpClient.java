package org.bms.usr.settings;

import okhttp3.Credentials;

public class HelperBmsSettingsHttpClient {

    // html
    public static final String RESTART_HTML = "restart.html";
    public static final String DO_CMD_HTML = "do_cmd.html";
    public static final String STA_CONFIG_HTML = "sta_config.html";
    public static final String APP_CONFIG_HTML = "app_config.html";
    public static final String OP_MODE_HTML = "opmode.html";
    public static final String FAST_HTML = "fast.html";

    // main
    public static final String BASE_URL = "http://10.10.100.254/EN/";
    public static final String RESTART_URL = BASE_URL + RESTART_HTML;
    public static final String SET_URL = BASE_URL + DO_CMD_HTML;
    public static final String AUTHORIZATION_KEY = "Authorization";
    public static final String CONTENT_TYPE_KEY = "Content-Type";

    public static final String AUTH = Credentials.basic("admin", "admin"); // admin:admin -> base64
    public static final String RESPONSE_BODY_OK = "OK";
    public static final String RESPONSE_BODY_HTTP_ERROR = "HTTP error ";

    // commands main
    public static final String CMD = "CMD";
    public static final String GO = "GO";

    // type commands
    public static final String LAN = "LAN";
    public static final String WIRELESS_BASIC = "WIRELESS_BASIC";
    public static final String APPLICATION = "Application";

    public static final String APPLY = "APPLY";


    // keys set
    public static final String SET0 = "SET0";
    public static final String SET1 = "SET1";
    public static final String SET2 = "SET2";
    public static final String SET3 = "SET3";
    public static final String SET4 = "SET4";
    public static final String SET5 = "SET5";

    // fields
    public static final String AP_CLI_ENABLE = "81002752";
    public static final String WIFI_MODE = "288162304";
    public static final String COUNTRY_CODE = "288162304";
    public static final String SYS_OPMODE = "18088192";
    public static final String DATA_TRANSFOR_MODE = "285278720";
    public static final String AP_STA_ENABLE = "304677376";
    public static final String WLAN_CLINUM = "303104512";
    public static final String NET_MODE = "285999616";
    public static final String NET_PORT = "286064896";
    public static final String NET_IP = "286130688";
    public static final String NETB_PORT = "286392576";
    public static final String NETB_IP = "286458368";
    public static final String NETB_MODE = "286327040";

    // values
    public static final String ON = "on";
    public static final String STA = "STA";
    public static final String CLIENT = "client";
    public static final String CONTENT_TYPE = "application/x-www-form-urlencoded";
}
