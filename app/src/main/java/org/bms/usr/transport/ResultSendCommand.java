package org.bms.usr.transport;

import org.bms.usr.provision.BmsCommandType;

import java.util.List;

/**
 *  Example:
 * 1. ForList<WifiNetwork>
 * ResultSendCommand<WifiNetwork> wifiResult = new ResultSendCommand<>(BmsCommandType.GET_WIFI_LIST, "Success", wifiNetworksList);
 * 2. Foer List<String>
 * ResultSendCommand<String> stringResult = new ResultSendCommand<>(BmsCommandType.SEND_MESSAGE, "Success", stringList);
 * @param type
 * @param messageError
 * @param payload
 * @param <T>
 */
public record ResultSendCommand<T>(
        BmsCommandType type,
        String messageError,
        List<T> payload
) {}

