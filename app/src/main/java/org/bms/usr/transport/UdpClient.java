package org.bms.usr.transport;

import static org.bms.usr.provision.HelperBmsProvision.BROADCAST_IP;
import static org.bms.usr.provision.HelperBmsProvision.PORT_DEF;
import static org.bms.usr.provision.HelperBmsProvision.TARGET_PORT_DEF;
import static org.bms.usr.provision.HelperBmsProvision.TIMEOUT;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.bms.usr.R;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class UdpClient {

    private DatagramSocket socket;
    private WifiManager.MulticastLock multicastLock;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Context context;

//    public interface UdpListener {
//        void onDataReceived(byte[] ssids);
//        void onError(String message);
//    }

    private final WiFiBmsListener listener;

    public UdpClient(Context context, WiFiBmsListener listener) {
        this.listener = listener;
        this.context = context;
        try {
            socket = new DatagramSocket( null);
            socket.setBroadcast(true);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(PORT_DEF));

            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                multicastLock = wifiManager.createMulticastLock("BMS_MulticastLock");
                multicastLock.setReferenceCounted(true);
            }
        } catch (SocketException e) {
            mainHandler.post(() -> listener.onError(context.getString(R.string.socket_init_error, e.getMessage())));
            Log.e("UdpClient", "Socket initialization error", e);
        }
    }

    public void startSendCommand(byte[] command) {
        if (socket == null || multicastLock == null) {
            listener.onError(context.getString(R.string.socket_not_initialized));
            return;
        }
        if (command.length == 0) {
            listener.onError(context.getString(R.string.command_not_formed));
            return;
        }

        new Thread(() -> {
            try {
                if (multicastLock != null) {
                    multicastLock.acquire();
                }
                DatagramPacket packet = new DatagramPacket(command, command.length, InetAddress.getByName(BROADCAST_IP), TARGET_PORT_DEF);
                socket.send(packet);
                Log.d("UdpClient", context.getString(R.string.udp_packet_sent));

                List<byte[]> ssids = new ArrayList<>();
                byte[] buffer = new byte[1024];
                DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);

                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < TIMEOUT) {
                    socket.setSoTimeout(TIMEOUT - (int)(System.currentTimeMillis() - startTime));
                    try {
                        socket.receive(responsePacket);
//                        String receivedData = new String(responsePacket.getData(), 0, responsePacket.getLength());
                        // We assume the response is a list of SSIDs
                        // If the format is different, this parsing will need to be changed.
                        ssids.add(responsePacket.getData());
                        Log.d("UdpClient", context.getString(R.string.response_received));
                    } catch (IOException e) {
                        break; // Timeout
                    }
                }

                // TODO Tests
//                if (command[3] == CMD_GET_WIFI_LIST.getCode()) {
//                if (command[3] == CMD_UPDATE_SETTINGS.getCode()) {
//                    String hexData = "FF016C811B31343133413037534C444F504730303930303100640D0A31343133413037534C444F504730303930303100640D0A513030333334313030323032383400640D0A6c6562656400640D0A31343133413037534C444F504730303930303100640D0A31343133413037534C444F504730303930303100640D0A31343133413037534C444F504730303930303100600D0A4B6F766C617232472D31002C0D0A002A0D0A4D696B726F54696B2D39353733424200220D0A6E657469735F322E34475F43424433454300180D0A4761726275686100170D0A54656E64615F7769666900180D0A4B76313800140D0A00140D0A4A61636B000F0D0A000D0D0A456c697a6162657468000D0D0A70617061000D0D0A000D0D0A4D696b726f54696b2d364135444638000D0D0A486f6d652057692d4669000D0D0A54502d4c696e6b5f32454142000A0D0A4d6164726964000A0D0A3232316100050D0A3931316200050D0A464946492d5749464900020D0AA8";
//                    String hexData = "FF000382010187";  // ok - 82
//                    String hexData = "FF000382000186";  // bad ssid - 82
//                    String hexData = "FF000382010086";  // bad psw - 82
//                    String hexData = "FF0003820010085";  // bad ssid + psw - 82
//                    byte[] data = hexToBytes(hexData);
//                    ssids.add(data);
//                }
                if (ssids.isEmpty()) {
                    mainHandler.post(() -> listener.onError(context.getString(R.string.no_response_from_bms)));
                } else {
                    mainHandler.post(() -> listener.onDataReceived(ssids.get(0)));
                }

            } catch (IOException e) {
                mainHandler.post(() -> listener.onError(context.getString(R.string.send_receive_error, e.getMessage())));
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