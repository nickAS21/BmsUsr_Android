package org.bms.bmsusrprovision.service;

import static org.bms.bmsusrprovision.service.BmsCommandType.CMD_GET_WIFI_LIST;
import static org.bms.bmsusrprovision.service.BmsCommandType.CMD_UPDATE_SETTINGS;
import static org.bms.bmsusrprovision.service.BmsCommandType.RSP_ERRORS;
import static org.bms.bmsusrprovision.service.BmsCommandType.RSP_UPDATE_SETTINGS;
import static org.bms.bmsusrprovision.service.BmsCommandType.RSP_WIFI_LIST;
import static org.bms.bmsusrprovision.service.BmsCommandType.UNKNOWN;
import static org.bms.bmsusrprovision.service.HelperBms.BYTE_MASK_TO_255;
import static org.bms.bmsusrprovision.service.HelperBms.BYTE_SEPARATOR_0;
import static org.bms.bmsusrprovision.service.HelperBms.BYTE_SEPARATOR_1;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * CodecBms is responsible for encoding and decoding BMS protocol commands.
 */
public class CodecBms {

    static String TAG = "CodecBMs";

    public static byte[] getCommand (BmsCommandType bmsCommandType, String... parameters) {
        switch (bmsCommandType) {
            case CMD_GET_WIFI_LIST: {
                return generateSearchCmd();
            }
            case CMD_UPDATE_SETTINGS: {
                // Перевіряємо, чи є два параметри, щоб уникнути помилки
                if (parameters.length >= 2) {
                    return generateSettingCmd(parameters[0], parameters[1]);
                } else {
                    // Обробка помилки: повертаємо порожній масив або кидаємо виняток
                    return new byte[0];
                }
            }
        }
        return new byte[0];
    }

    /**
     * Creates a searching command packet for the BMS.
     * Example: FF 00 01 01 02
     */
    public static byte[] generateSearchCmd() {
        byte[] payload = {(byte) CMD_GET_WIFI_LIST.getCode()};
        return generatePacket(payload);
    }

    /**
     * TODO test
     * @param hexData
     * @return
     */
//    public static Map<String, Object> decodeResponse(String hexData) {
//        byte[] data = hexToBytes(hexData);
//        return decodeResponse(data);
//    }
    public static <T> ResultSendCommand<T> decodeResponse(byte[] data) {
        int lenDataResponse = byte2int(data[1], data[2]) + 4;
        byte [] dataResponse = new byte[lenDataResponse];
        System.arraycopy(data, 0, dataResponse, 0, lenDataResponse);
        String messageError = null;
        if (lenDataResponse < 4) {
            messageError = "Invalid packet length.";

        }
        if (!validateChecksum(dataResponse)) {
            messageError =  "Invalid checksum";
        }
        Map<String, Object> result = new HashMap<>();
        BmsCommandType type = BmsCommandType.fromCode(dataResponse[3] & BYTE_MASK_TO_255);
        if (dataResponse.length < 6 && (type.equals(RSP_WIFI_LIST) || type.equals(RSP_UPDATE_SETTINGS))) {
            messageError =  "Invalid decode Response. Length < 6";
        }
        if (messageError != null) {
            return new ResultSendCommand<>(RSP_ERRORS, messageError, null);
        }

        result.put("type", type);
        switch (type) {
            case RSP_WIFI_LIST: // список WiFi
                return new ResultSendCommand<>(type, messageError, (List<T>) decodeSearchResponse(dataResponse));
            case RSP_UPDATE_SETTINGS: // результат конфігурації
                return decodeSettingResponse(dataResponse);
            default:
                List<String> hexData = List.of(bytesToHex(dataResponse));
                return new ResultSendCommand<>(UNKNOWN, messageError, (List<T>) hexData);

        }
    }

    /**
     * Decodes a response packet for a searching command (0x81).
     /**
     * Parse response with WiFi networks
     * data[0] == 0xFF
     * data[1], data[2] - length (integer from little-endian byte array.)
     * data[3] = typeRsp (0x81, 0x82)
     * data[4] = if == 0 Code Error 1 - AP num
     * data[5] = if == 0 Code Error 2
     * data[6] = List<WifiNetwork>
     */

    public static List<WifiNetwork> decodeSearchResponse(byte[] bArr) {
        Map<String, WifiNetwork> uniqueNetworks = new HashMap<>();
        int currentPos = 5;
        ByteArrayOutputStream currentEntryBytes = new ByteArrayOutputStream();
        Log.d(TAG, "decodeSearchResponse: " + bytesToHex(bArr));
        while (currentPos < bArr.length) {
            if (currentPos + 1 < bArr.length && bArr[currentPos] == BYTE_SEPARATOR_0 && bArr[currentPos + 1] == BYTE_SEPARATOR_1) {
                if (currentEntryBytes.size() > 0) {
                    byte[] entryData = currentEntryBytes.toByteArray();

                    // Рівень сигналу - останній байт перед 0x0D
                    int signalLevel = -(entryData[entryData.length - 1] & BYTE_MASK_TO_255);
                    Log.d(TAG, "signalLevel: " + signalLevel);
                    Log.d(TAG, "len_entryData: " + entryData.length);
                    // SSID - решта байтів -> <-> (0x00 + signalLevel) = 2
                    int len = entryData.length - 3;

                    if (len > 2) {
                        byte[] ssidBytes = new byte[entryData.length - 3];
                        System.arraycopy(entryData, 0, ssidBytes, 0, ssidBytes.length - 1);
                        String ssid = new String(ssidBytes).trim();
                        Log.d(TAG, "ssid: " + ssid);
                        if (!ssid.isEmpty()) {
                            if (!uniqueNetworks.containsKey(ssid) || (Objects.requireNonNull(uniqueNetworks.get(ssid)).getSignalLevel() < signalLevel)) {
                                WifiNetwork network = new WifiNetwork();
                                network.setSsid(ssid);
                                network.setSignalLevel(signalLevel);
                                uniqueNetworks.put(ssid, network);
                            }
                        }
                    }
                }
                currentPos += 2; // Переходимо до наступної позиції після 0x0A
                currentEntryBytes.reset();
            } else {
                currentEntryBytes.write(bArr[currentPos]);
                currentPos++;
            }
        }

        // Сортуємо унікальні мережі за рівнем сигналу
        List<WifiNetwork> sortedNetworks = new ArrayList<>(uniqueNetworks.values());
        sortedNetworks.sort((a, b) -> Integer.compare(b.getSignalLevel(), a.getSignalLevel()));

        return sortedNetworks;
    }

    /**
     * Generates a setting command packet (0x02).
     */
    public static byte[] generateSettingCmd(String ssid, String pwd) {
        byte[] ssidBytes = ssid.getBytes(StandardCharsets.UTF_8);
        byte[] pwdBytes = pwd.getBytes(StandardCharsets.UTF_8);

        // length = 1-cmd + 1-reserve + ssid.length + 2-separator + password.length
        int payloadLength = 2 + ssidBytes.length + 2 + pwdBytes.length;
        byte[] payload = new byte[payloadLength];

        int offset = 0;
        payload[offset++] = (byte) CMD_UPDATE_SETTINGS.getCode();
        payload[offset++] = (byte) 0x00; // reserve byte
        System.arraycopy(ssidBytes, 0, payload, offset, ssidBytes.length);
        offset += ssidBytes.length;
        payload[offset++] = (byte) 0x0D;
        payload[offset++] = (byte) 0x0A;
        System.arraycopy(pwdBytes, 0, payload, offset, pwdBytes.length);
        return generatePacket(payload);
    }

    /**
     * Decodes a response packet for a setting command (0x82).
     */
    public static <T> ResultSendCommand<T> decodeSettingResponse(byte[] bArr) {
        int ssidCheck = bArr[4] & BYTE_MASK_TO_255;
        int passwordCheck = bArr[5] & BYTE_MASK_TO_255;
        String messageError = null;
        List<Object> payload = null;
        BmsCommandType type;
        if (ssidCheck == 1 && passwordCheck == 1) {
            type = RSP_UPDATE_SETTINGS;
            payload = List.of("ssid - ok", "password - ok");
        } else if (ssidCheck == 0 && passwordCheck == 0) {
            messageError = "ssid_not_found and password_invalid";
            type = RSP_ERRORS;
        } else if (ssidCheck == 0) {
            type = RSP_ERRORS;
            messageError = "ssid_not_found";
        } else if (passwordCheck == 0) {
            type = RSP_ERRORS;
            messageError = "password_invalid";
        }  else {
            type = UNKNOWN;
            payload = List.of(ssidCheck, passwordCheck);
        }
        return new ResultSendCommand<>(type, messageError, (List<T>) payload);
    }


    /**
     * Generates a complete packet with header, length, payload, and checksum.
     */
    private static byte[] generatePacket(byte[] payload) {
        if (payload == null) {
            return null;
        }
        // add 1-head + 2-length + 1-checksum == 4
        int totalLength = payload.length + 4;
        byte[] packet = new byte[totalLength];
        int i = 0;
        // head
        packet[i++] = (byte) BYTE_MASK_TO_255;
        // length (2 bytes, big-endian)
        packet[i++] = (byte) ((payload.length >> 8) & BYTE_MASK_TO_255);
        packet[i++] = (byte) (payload.length & BYTE_MASK_TO_255);
        // copy payload
        System.arraycopy(payload, 0, packet, i, payload.length);
        // checksum
        packet[totalLength - 1] = checkSum(packet);
        return packet;
    }

    /**
     * Convert list of WifiNetwork objects to List<Map> for Flutter
     */
    private static List<Map<String, Object>> serializeNetworks(List<WifiNetwork> networks) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (WifiNetwork net : networks) {
            Map<String, Object> map = new HashMap<>();
            map.put("ssid", net.getSsid());
            map.put("level", net.getSignalLevel());
            list.add(map);
        }
        return list;
    }

    private static boolean validateChecksum(byte[] data) {
        if (data.length < 5) return false;
        return checkSum(data) == data[data.length - 1];
    }


    private static byte checkSum(byte[] data) {
        byte checksum = 0;
        for (int i = 1; i <= data.length - 2; i++) {
            checksum += data[i];
        }
        return checksum;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    public static byte[] int2byte(int i) {
        return new byte[]{(byte) (i & 255), (byte) ((i >> 8) & 255)};
    }

    public static int byte2int(byte b1, byte b2) {
        return (b2 & 0xFF) | ((b1 & 0xFF) << 8);
    }

    static byte[] hexToBytes(String hex) {
        // Якщо довжина рядка непарна, кидаємо виняток або додаємо '0'
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have an even length.");
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            // Перетворення двох символів у байт за допомогою Character.digit
            int high = Character.digit(hex.charAt(i), 16);
            int low = Character.digit(hex.charAt(i + 1), 16);
            // Перевірка на некоректні символи
            if (high == -1 || low == -1) {
                throw new IllegalArgumentException("Invalid hex character in string.");
            }
            // Комбінування двох байтів в один
            data[i / 2] = (byte) ((high << 4) + low);
        }
        return data;
    }
}