package com.zjf.seeme.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zjf.seeme.R;
import com.zjf.seeme.data.entity.BlacklistApp;

import java.util.ArrayList;
import java.util.List;

public class BlacklistAdapter extends RecyclerView.Adapter<BlacklistAdapter.ViewHolder> {

    private List<BlacklistApp> apps = new ArrayList<>();
    private OnRemoveListener listener;

    public interface OnRemoveListener {
        void onRemove(BlacklistApp app);
    }

    public void setOnRemoveListener(OnRemoveListener listener) {
        this.listener = listener;
    }

    public void setApps(List<BlacklistApp> apps) {
        this.apps = apps;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_whitelist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(apps.get(position));
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName, tvPackage;
        private final ImageView btnRemove;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvWhitelistName);
            tvPackage = itemView.findViewById(R.id.tvWhitelistPackage);
            btnRemove = itemView.findViewById(R.id.btnRemoveWhitelist);
        }

        void bind(BlacklistApp app) {
            tvName.setText(app.getAppName());
            tvPackage.setText(app.getPackageName());
            btnRemove.setOnClickListener(v -> {
                if (listener != null) listener.onRemove(app);
            });
        }
    }
}
