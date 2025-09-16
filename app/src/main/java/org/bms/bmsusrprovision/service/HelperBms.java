package org.bms.bmsusrprovision.service;

public class HelperBms {

    public static final int BYTE_MASK_TO_255 = 0xff;
    public static final int BYTE_SEPARATOR_0 = 0x0D;
    public static final int BYTE_SEPARATOR_1 = 0x0A;
    public static final String BROADCAST_IP = "255.255.255.255";
    public static final int PORT_DEF = 26000;
    public static final int TARGET_PORT_DEF = 49000;
    public static final int TIMEOUT = 5000; // TimeOut response in ms
    public static final String CURRENT_SSID_START_TEXT = "CURRENT_SSID_START";
    public static final String CHOSEN_SSID_TEXT = "CHOSEN_SSID";
    public static final String WIFI_FILTER_SSID_DEF = "USR-WIFI232-";

//    public static final byte[] SEARCH_CODE = new byte[]{(byte) 0xFF, 0x00, 0x01, 0x01, 0x02}; // For tests

}
