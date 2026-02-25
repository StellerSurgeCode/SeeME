package com.zjf.seeme.ui.adapter;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.zjf.seeme.R;
import com.zjf.seeme.data.entity.MessageEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MessageAdapter extends ListAdapter<MessageEntity, MessageAdapter.ViewHolder> {

    private OnMessageClickListener listener;

    public interface OnMessageClickListener {
        void onMessageClick(MessageEntity message);
        void onMessageLongClick(MessageEntity message);
        default void onMessageDelete(MessageEntity message) {}
        default void onOpenApp(MessageEntity message) {}
    }

    public MessageAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnMessageClickListener(OnMessageClickListener listener) {
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<MessageEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<MessageEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull MessageEntity oldItem, @NonNull MessageEntity newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull MessageEntity oldItem, @NonNull MessageEntity newItem) {
                    return oldItem.getTitle().equals(newItem.getTitle())
                            && oldItem.getContent().equals(newItem.getContent())
                            && oldItem.getImportance() == newItem.getImportance()
                            && oldItem.isRead() == newItem.isRead();
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MessageEntity msg = getItem(position);
        holder.bind(msg);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final View importanceBar;
        private final TextView tvAppName, tvCategory, tvTime, tvTitle, tvContent, tvImportance, tvSender;
        private final ImageView btnDelete, btnOpenApp;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            importanceBar = itemView.findViewById(R.id.importanceBar);
            tvAppName = itemView.findViewById(R.id.tvAppName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvImportance = itemView.findViewById(R.id.tvImportance);
            tvSender = itemView.findViewById(R.id.tvSender);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnOpenApp = itemView.findViewById(R.id.btnOpenApp);
        }

        void bind(MessageEntity msg) {
            tvAppName.setText(msg.getAppName());
            tvTitle.setText(msg.getTitle());
            tvContent.setText(msg.getContent());
            tvCategory.setText(msg.getCategory());
            tvTime.setText(formatTime(msg.getTimestamp()));
            tvImportance.setText(String.format(Locale.getDefault(), "重要性 %d/10", msg.getImportance()));

            if (msg.getSenderName() != null && !msg.getSenderName().isEmpty()) {
                tvSender.setVisibility(View.VISIBLE);
                tvSender.setText(String.format("来自 %s", msg.getSenderName()));
            } else {
                tvSender.setVisibility(View.GONE);
            }

            int barColor = getImportanceColor(msg.getImportance());
            importanceBar.setBackgroundColor(barColor);

            GradientDrawable categoryBg = new GradientDrawable();
            categoryBg.setCornerRadius(12f);
            categoryBg.setColor(getCategoryColor(msg.getCategory()));
            tvCategory.setBackground(categoryBg);

            float alpha = msg.isRead() ? 0.6f : 1.0f;
            itemView.setAlpha(alpha);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onMessageClick(msg);
            });
            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onMessageLongClick(msg);
                return true;
            });
            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onMessageDelete(msg);
            });
            btnOpenApp.setOnClickListener(v -> {
                if (listener != null) listener.onOpenApp(msg);
            });
        }

        private int getImportanceColor(int importance) {
            if (importance >= 8) return 0xFFE53935;
            if (importance >= 6) return 0xFFFB8C00;
            if (importance >= 4) return 0xFFFDD835;
            return 0xFF78909C;
        }

        private int getCategoryColor(String category) {
            if (category == null) return 0xFF7B68AE;
            switch (category) {
                case "工作": return 0xFF4A90D9;
                case "生活": return 0xFF50C878;
                case "娱乐": return 0xFFE8A838;
                case "广告推销": return 0xFFD94A4A;
                default: return 0xFF7B68AE;
            }
        }

        private String formatTime(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;

            if (diff < 60_000) return "刚刚";
            if (diff < 3_600_000) return (diff / 60_000) + "分钟前";
            if (diff < 86_400_000) return (diff / 3_600_000) + "小时前";

            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }
}
