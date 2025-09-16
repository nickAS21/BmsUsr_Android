package org.bms.bmsusrprovision.service;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import org.bms.bmsusrprovision.R;

import java.util.ArrayList;
import java.util.List;

public class WifiListAdapter extends RecyclerView.Adapter<WifiListAdapter.WifiViewHolder> {

    private List<WifiNetwork> wifiNetworks;
    private final OnItemClickListener listener;
    private final Context context;

    public interface OnItemClickListener {
        void onItemClick(WifiNetwork network);
    }

    public WifiListAdapter(Context context, OnItemClickListener listener) {
        this.wifiNetworks =new ArrayList<>();
        this.listener = listener;
        this.context = context;
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

        // Find icon by secured
        int iconRes = getWifiSignalIcon(network.getSignalLevel(), network.isSecured());
        holder.imageViewWifiSignal.setImageResource(iconRes);

        if (network.isCurrentSsidStart()) {
            holder.textViewSsid.setTypeface(null, Typeface.BOLD);
            holder.imageViewTick.setColorFilter(context.getColor(R.color.purple_700));
            holder.imageViewTick.setVisibility(View.VISIBLE);
        } else if (network.isCurrent()) {
            holder.textViewSsid.setTypeface(null, Typeface.BOLD);
            holder.imageViewTick.setColorFilter(context.getColor(R.color.green));
            holder.imageViewTick.setVisibility(View.VISIBLE);
        }  else {
            holder.textViewSsid.setTypeface(null, Typeface.NORMAL);
            holder.imageViewTick.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(network));
    }

    @Override
    public int getItemCount() {
        return wifiNetworks.size();
    }

    // Find icon by Wi-Fi level
    private int getWifiSignalIcon(int signalLevel, boolean secured) {
        int icon;
        if (signalLevel >= -67) { // Full level
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

    public static class WifiViewHolder extends RecyclerView.ViewHolder {
        TextView textViewSsid;
        ImageView imageViewTick;
        ImageView imageViewWifiSignal;

        public WifiViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewSsid = itemView.findViewById(R.id.textViewSsid);
            imageViewTick = itemView.findViewById(R.id.imageViewTick);
            imageViewWifiSignal = itemView.findViewById(R.id.imageViewWifiSignal);
        }
    }

    public void setWifiNetworks(List<WifiNetwork> newNetworks) {
        final WifiDiffCallback diffCallback = new WifiDiffCallback(this.wifiNetworks, newNetworks);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.wifiNetworks.clear();
        this.wifiNetworks.addAll(newNetworks);
        diffResult.dispatchUpdatesTo(this);
    }

    // Foe compare items
    private static class WifiDiffCallback extends DiffUtil.Callback {
        private final List<WifiNetwork> oldList;
        private final List<WifiNetwork> newList;

        public WifiDiffCallback(List<WifiNetwork> oldList, List<WifiNetwork> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getSsid().equals(newList.get(newItemPosition).getSsid());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            final WifiNetwork oldNetwork = oldList.get(oldItemPosition);
            final WifiNetwork newNetwork = newList.get(newItemPosition);
            return oldNetwork.getSignalLevel() == newNetwork.getSignalLevel() &&
                    oldNetwork.isSecured() == newNetwork.isSecured() &&
                    oldNetwork.isCurrent() == newNetwork.isCurrent();
        }
    }
}