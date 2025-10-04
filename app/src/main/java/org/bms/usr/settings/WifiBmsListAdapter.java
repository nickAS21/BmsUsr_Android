package org.bms.usr.settings;

import static org.bms.usr.MenuHelper.getWifiSignalIcon;

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

import org.bms.usr.R;

import java.util.ArrayList;
import java.util.List;

public class WifiBmsListAdapter extends RecyclerView.Adapter<WifiBmsListAdapter.WifiViewHolder> {

    private List<WiFiBmsEntity> wiFiBmsEntities;
    private final OnItemClickListener listener;
    private final Context context;
    private OnInfoClickListener infoClickListener;


    public interface OnItemClickListener {
        void onItemClick(WiFiBmsEntity network);
    }

    public interface OnInfoClickListener {
        void onInfoClick(WiFiBmsEntity network);
    }


    public WifiBmsListAdapter(Context context, OnItemClickListener listener, OnInfoClickListener infoClickListener) {
        this.wiFiBmsEntities =new ArrayList<>();
        this.listener = listener;
        this.context = context;
        this.infoClickListener = infoClickListener;
    }

    @NonNull
    @Override
    public WifiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.wifi_bms_list_item, parent, false);
        return new WifiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WifiViewHolder holder, int position) {
        WiFiBmsEntity network = wiFiBmsEntities.get(position);
        holder.textViewId.setText(String.valueOf(network.id()));
        holder.textViewSsid.setText(network.ssid());
        holder.textViewBSsid.setText(network.bssid());
        holder.textViewSsidBms.setText(network.ssidBms());

//        holder.textViewId.setText(String.valueOf(network.id()));
//        holder.textViewIpWiFiHome.setText(network.ipWiFiHome());
//        holder.textViewPorSta.setText(String.valueOf(network.portSTA()));
//        holder.textViewOui.setText(network.oui());
        holder.imageViewInfo.setVisibility(infoClickListener != null ? View.VISIBLE : View.GONE);
        holder.imageViewInfo.setOnClickListener(v -> {
            if (infoClickListener != null) {
                infoClickListener.onInfoClick(network);
            }
        });

        // Find icon by secured
        int iconRes = getWifiSignalIcon(-40, true);
        holder.imageViewWifiSignal.setImageResource(iconRes);

//        if (network.isCurrentSsidStart()) {
//            holder.textViewSsid.setTypeface(null, Typeface.BOLD);
//            holder.imageViewTick.setColorFilter(context.getColor(R.color.purple_700));
//            holder.imageViewTick.setVisibility(View.VISIBLE);
//        } else if (network.isCurrent()) {
//            holder.textViewSsid.setTypeface(null, Typeface.BOLD);
//            holder.imageViewTick.setColorFilter(context.getColor(R.color.green));
//            holder.imageViewTick.setVisibility(View.VISIBLE);
//        }  else {
            holder.textViewSsid.setTypeface(null, Typeface.NORMAL);
            holder.imageViewTick.setVisibility(View.GONE);
//        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(network));
    }

    @Override
    public int getItemCount() {
        return wiFiBmsEntities.size();
    }

    public static class WifiViewHolder extends RecyclerView.ViewHolder {
        TextView textViewId;
        TextView textViewSsid;
        TextView textViewSsidBms;
        TextView textViewBSsid;
        TextView textViewIpWiFiHome;
        TextView textViewPorSta;
        TextView textViewOui;
        ImageView imageViewTick;
        ImageView imageViewWifiSignal;
        ImageView imageViewInfo;

        public WifiViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewId = itemView.findViewById(R.id.textViewId);
            textViewSsid = itemView.findViewById(R.id.textViewSsid);
            textViewBSsid = itemView.findViewById(R.id.textViewBSsid);
            textViewSsidBms = itemView.findViewById(R.id.textViewSsidBms);

//            textViewId = itemView.findViewById(R.id.textViewId);
//            textViewIpWiFiHome = itemView.findViewById(R.id.textViewIpWiFiHome);
//            textViewPorSta = itemView.findViewById(R.id.textViewPortSta);
//            textViewOui = itemView.findViewById(R.id.textViewOui);
            imageViewTick = itemView.findViewById(R.id.imageViewTick);
            imageViewWifiSignal = itemView.findViewById(R.id.imageViewWifiSignal);
            imageViewInfo = itemView.findViewById(R.id.imageViewInfo);
        }
    }

    public void setWiFiBmsEntities(List<WiFiBmsEntity> newNetworks) {
        final WifiDiffCallback diffCallback = new WifiDiffCallback(this.wiFiBmsEntities, newNetworks);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.wiFiBmsEntities.clear();
        this.wiFiBmsEntities.addAll(newNetworks);
        diffResult.dispatchUpdatesTo(this);
    }

    // Foe compare items
    private static class WifiDiffCallback extends DiffUtil.Callback {
        private final List<WiFiBmsEntity> oldList;
        private final List<WiFiBmsEntity> newList;

        public WifiDiffCallback(List<WiFiBmsEntity> oldList, List<WiFiBmsEntity> newList) {
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
            WiFiBmsEntity oldNetwork = oldList.get(oldItemPosition);
            WiFiBmsEntity newNetwork = newList.get(newItemPosition);
            // Унікальний ідентифікатор для точки доступу - це BSSID
            return oldNetwork.bssid().equals(newNetwork.bssid());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            WiFiBmsEntity oldNetwork = oldList.get(oldItemPosition);
            WiFiBmsEntity newNetwork = newList.get(newItemPosition);

            return oldNetwork.bssid().equals(newNetwork.bssid());
        }
    }
}