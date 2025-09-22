package org.bms.usr.service;

public class WifiNetwork {
    private String ssid;
    private String bssid;
    private int signalLevel;
    private boolean isCurrent;
    private boolean isCurrentSsidStart;
    private boolean secured;

    public WifiNetwork() {
    }

    public WifiNetwork(String ssid, String bssid, int signalLevel, boolean isCurrent, boolean isCurrentStart, boolean secured) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.signalLevel = signalLevel;
        this.isCurrent = isCurrent;
        this.isCurrentSsidStart = isCurrentStart;
        this.secured = secured;
    }

    public String getSsid() {
        return this.ssid;
    }
    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getBSsid() {
        return this.bssid;
    }

    public int getSignalLevel() {
        return this.signalLevel;
    }

    public void setSignalLevel(int signalLevel) {
        this.signalLevel = signalLevel;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setIsCurrent(boolean isCurrent) {
        this.isCurrent = isCurrent;
    }

    public boolean isCurrentSsidStart() {
        return isCurrentSsidStart;
    }

    public void setIsdCurrentSsidStart(boolean isCurrentSsidStart) {
        this.isCurrentSsidStart = isCurrentSsidStart;
    }

    public boolean isSecured() {
        return secured;
    }
    public void setSecured(boolean secured) {
        this.secured = secured;
    }
}