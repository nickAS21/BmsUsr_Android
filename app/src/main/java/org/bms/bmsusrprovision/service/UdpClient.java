package org.bms.bmsusrprovision.service;

import static org.bms.bmsusrprovision.service.BmsCommandType.CMD_GET_WIFI_LIST;
import static org.bms.bmsusrprovision.service.BmsCommandType.CMD_UPDATE_SETTINGS;
import static org.bms.bmsusrprovision.service.BmsCommandType.RSP_UPDATE_SETTINGS;
import static org.bms.bmsusrprovision.service.CodecBms.decodeResponse;
import static org.bms.bmsusrprovision.service.CodecBms.hexToBytes;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class UdpClient {

    private DatagramSocket socket;
    private WifiManager.MulticastLock multicastLock;
    private static final String BROADCAST_IP = "255.255.255.255";
    private static final int TARGET_PORT = 49000;
    private static final int LOCAL_PORT = 26000;
    private static final int TIMEOUT = 5000; // Таймаут очікування відповіді в мс
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final byte[] SEARCH_CODE = new byte[]{(byte) 0xFF, 0x00, 0x01, 0x01, 0x02};

    public interface UdpListener {
        void onDataReceived(byte[] ssids);
        void onError(String message);
    }

    private final UdpListener listener;

    public UdpClient(Context context, UdpListener listener) {
        this.listener = listener;
        try {
            socket = new DatagramSocket((SocketAddress) null);
            socket.setBroadcast(true);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(LOCAL_PORT));

            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                multicastLock = wifiManager.createMulticastLock("BMS_MulticastLock");
                multicastLock.setReferenceCounted(true);
            }
        } catch (SocketException e) {
            mainHandler.post(() -> listener.onError("Помилка ініціалізації сокета: " + e.getMessage()));
            Log.e("UdpClient", "Socket initialization error", e);
        }
    }

    public void startSendCommand(byte[] command) {
        if (socket == null || multicastLock == null) {
            listener.onError("Сокет або MulticastLock не ініціалізовано.");
            return;
        }
        if (command.length == 0) {
            listener.onError("Команда не сформована. Сommand.length == 0");
            return;
        }

        new Thread(() -> {
            try {
                if (multicastLock != null) {
                    multicastLock.acquire();
                }
//                DatagramPacket packet = new DatagramPacket(SEARCH_CODE, SEARCH_CODE.length, InetAddress.getByName(BROADCAST_IP), TARGET_PORT);
                DatagramPacket packet = new DatagramPacket(command, command.length, InetAddress.getByName(BROADCAST_IP), TARGET_PORT);
                socket.send(packet);
                Log.d("UdpClient", "UDP пакет відправлено.");

                List<byte[]> ssids = new ArrayList<>();
                byte[] buffer = new byte[1024];
                DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);

                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < TIMEOUT) {
                    socket.setSoTimeout(TIMEOUT - (int)(System.currentTimeMillis() - startTime));
                    try {
                        socket.receive(responsePacket);
//                        String receivedData = new String(responsePacket.getData(), 0, responsePacket.getLength());
                        // Припускаємо, що відповідь – це SSID у вигляді рядка.
                        // Якщо формат інший, цей парсинг треба буде змінити.
                        ssids.add(responsePacket.getData());
                        Log.d("UdpClient", "Отримано відповідь: " );
                    } catch (IOException e) {
                        break; // Таймаут
                    }
                }

                // TODO Tests
//                if (command[3] == CMD_GET_WIFI_LIST.getCode()) {
//                if (command[3] == CMD_UPDATE_SETTINGS.getCode()) {
    //                String hexData = "FF016C811B31343133413037534C444F504730303930303100640D0A31343133413037534C444F504730303930303100640D0A513030333334313030323032383400640D0A6C6562656400640D0A31343133413037534C444F504730303930303100640D0A31343133413037534C444F504730303930303100640D0A31343133413037534C444F504730303930303100600D0A4B6F766C617232472D31002C0D0A002A0D0A4D696B726F54696B2D39353733424200220D0A6E657469735F322E34475F43424433454300180D0A4761726275686100170D0A54656E64615F7769666900180D0A4B76313800140D0A00140D0A4A61636B000F0D0A000D0D0A456C697A6162657468000D0D0A70617061000D0D0A000D0D0A4D696B726F54696B2D364135444638000D0D0A486F6D652057692D4669000D0D0A54502D4C696E6B5F32454142000A0D0A4D6164726964000A0D0A3232316100050D0A3931316200050D0A464946492D5749464900020D0AA8";
//                    String hexData = "FF000382010187";  // ok - 82
//                    String hexData = "FF000382000186";  // bad ssid - 82
//                    String hexData = "FF000382010086";  // bad psw - 82
//                    String hexData = "FF0003820010085";  // bad ssid + psw - 82
//                    byte[] data = hexToBytes(hexData);
//                    ssids.add(data);
//                }
                if (ssids.isEmpty()) {
                    mainHandler.post(() -> listener.onError("Не отримано відповіді від BMS."));
                } else {
                    mainHandler.post(() -> listener.onDataReceived(ssids.get(0)));
                }

            } catch (IOException e) {
                mainHandler.post(() -> listener.onError("Помилка при відправленні/отриманні: " + e.getMessage()));
                Log.e("UdpClient", "IO error", e);
            } finally {
                if (multicastLock != null && multicastLock.isHeld()) {
                    multicastLock.release();
                }
            }
        }).start();
    }
    public void closeSocket() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}