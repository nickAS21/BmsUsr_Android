package org.bms.bmsusrprovision.service;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.bms.bmsusrprovision.R;

import java.util.List;

public class WifiListAdapter extends RecyclerView.Adapter<WifiListAdapter.WifiViewHolder> {

    private List<WifiNetwork> wifiNetworks;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(WifiNetwork network);
    }

    public WifiListAdapter(List<WifiNetwork> wifiNetworks, OnItemClickListener listener) {
        this.wifiNetworks = wifiNetworks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WifiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.wifi_list_item, parent, false);
        return new WifiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WifiViewHolder holder, int position) {
        WifiNetwork network = wifiNetworks.get(position);
        holder.textViewSsid.setText(network.getSsid());

        // Вибір іконки з урахуванням secured
        int iconRes = getWifiSignalIcon(network.getSignalLevel(), network.isSecured());
        holder.imageViewWifiSignal.setImageResource(iconRes);

        if (network.isCurrent()) {
            holder.textViewSsid.setTypeface(null, Typeface.BOLD);
            holder.imageViewTick.setVisibility(View.VISIBLE);
        } else {
            holder.textViewSsid.setTypeface(null, Typeface.NORMAL);
            holder.imageViewTick.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(network));
    }

    @Override
    public int getItemCount() {
        return wifiNetworks.size();
    }

    public void setWifiNetworks(List<WifiNetwork> newNetworks) {
        this.wifiNetworks = newNetworks;
        notifyDataSetChanged();
    }

    // Метод для вибору іконки Wi-Fi сигналу
    private int getWifiSignalIcon(int signalLevel, boolean secured) {
        int icon;
        if (signalLevel >= -67) { // Повний сигнал
            icon = secured ? R.drawable.ic_wifi_signal_4_lock : R.drawable.ic_wifi_signal_4;
        } else if (signalLevel >= -70) {
            icon = secured ? R.drawable.ic_wifi_signal_3_lock : R.drawable.ic_wifi_signal_3;
        } else if (signalLevel >= -80) {
            icon = secured ? R.drawable.ic_wifi_signal_2_lock : R.drawable.ic_wifi_signal_2;
        } else if (signalLevel > -90) {
            icon = secured ? R.drawable.ic_wifi_signal_1_lock : R.drawable.ic_wifi_signal_1;
        } else {
            icon = secured ? R.drawable.ic_wifi_signal_0_lock : R.drawable.ic_wifi_signal_0;
        }
        return icon;
    }

    static class WifiViewHolder extends RecyclerView.ViewHolder {
        TextView textViewSsid;
        ImageView imageViewTick;
        ImageView imageViewWifiSignal; // Додано ImageView для сигналу

        public WifiViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewSsid = itemView.findViewById(R.id.textViewSsid);
            imageViewTick = itemView.findViewById(R.id.imageViewTick);
            imageViewWifiSignal = itemView.findViewById(R.id.imageViewWifiSignal);
        }
    }
}