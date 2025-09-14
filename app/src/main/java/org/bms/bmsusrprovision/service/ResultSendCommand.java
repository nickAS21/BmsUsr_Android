package org.bms.bmsusrprovision.service;

import java.util.List;

/**
 *  Приклади використання:
 * 1. Для List<WifiNetwork>
 * ResultSendCommand<WifiNetwork> wifiResult = new ResultSendCommand<>(BmsCommandType.GET_WIFI_LIST, "Success", wifiNetworksList);
 *
 * 2. Для List<String>
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

