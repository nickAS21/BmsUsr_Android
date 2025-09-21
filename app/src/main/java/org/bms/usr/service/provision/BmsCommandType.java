package org.bms.usr.service.provision;
public enum BmsCommandType {

    CMD_GET_WIFI_LIST(0x01, "req_wifi_list"),
    RSP_WIFI_LIST(0x81, "resp_wifi_list"),
    CMD_UPDATE_SETTINGS(0x02, "req_wifi_config"),
    RSP_UPDATE_SETTINGS(0x82, "resp_wifi_config"),
    RSP_ERRORS(0x101, "resp_wifi_error"),
    UNKNOWN(0x00, "unknown");

    private final int code;
    private final String typeName;

    BmsCommandType(int code, String typeName) {
        this.code = code;
        this.typeName = typeName;
    }

    public int getCode() {
        return code;
    }

    public String getTypeName() {
        return typeName;
    }

    public static BmsCommandType fromCode(int code) {
        for (BmsCommandType c : values()) {
            if (c.code == code) {
                return c;
            }
        }
        return UNKNOWN;
    }
}
