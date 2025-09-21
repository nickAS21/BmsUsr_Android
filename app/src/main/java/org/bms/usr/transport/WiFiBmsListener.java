// WiFiBmsListener.java
package org.bms.usr.transport;

public interface WiFiBmsListener {
    void onDataReceived(byte[] ssids);
    void onError(String message);
}
