package com.zjf.seeme.ui.adapter;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zjf.seeme.R;
import com.zjf.seeme.data.entity.MessageEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GroupedMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_GROUP_HEADER = 0;
    private static final int TYPE_MESSAGE = 1;
    private static final int TYPE_CHILD_MESSAGE = 2;

    private final List<Object> displayItems = new ArrayList<>();
    private final java.util.Set<Integer> childPositions = new java.util.HashSet<>();
    private final Map<String, Boolean> expandedGroups = new LinkedHashMap<>();
    private OnMessageClickListener listener;

    public interface OnMessageClickListener {
        void onMessageClick(MessageEntity message);
        void onMessageLongClick(MessageEntity message);
        void onMessageDelete(MessageEntity message);
        void onOpenApp(MessageEntity message);
    }

    public void setOnMessageClickListener(OnMessageClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<MessageEntity> messages) {
        Map<String, List<MessageEntity>> groups = new LinkedHashMap<>();

        for (MessageEntity msg : messages) {
            String key = buildGroupKey(msg);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(msg);
        }

        List<MessageGroup> sortedGroups = new ArrayList<>();
        for (Map.Entry<String, List<MessageEntity>> entry : groups.entrySet()) {
            List<MessageEntity> groupMsgs = entry.getValue();
            int maxImportance = 0;
            long latestTime = 0;
            for (MessageEntity m : groupMsgs) {
                maxImportance = Math.max(maxImportance, m.getImportance());
                latestTime = Math.max(latestTime, m.getTimestamp());
            }
            sortedGroups.add(new MessageGroup(entry.getKey(), groupMsgs, maxImportance, latestTime));
        }

        Collections.sort(sortedGroups, (a, b) -> {
            if (a.maxImportance != b.maxImportance) return b.maxImportance - a.maxImportance;
            return Long.compare(b.latestTimestamp, a.latestTimestamp);
        });

        displayItems.clear();
        childPositions.clear();
        for (MessageGroup group : sortedGroups) {
            if (!expandedGroups.containsKey(group.key)) {
                expandedGroups.put(group.key, false);
            }

            displayItems.add(group);

            boolean expanded = Boolean.TRUE.equals(expandedGroups.get(group.key));
            if (expanded && group.messages.size() > 1) {
                for (MessageEntity msg : group.messages) {
                    childPositions.add(displayItems.size());
                    displayItems.add(msg);
                }
            }
        }

        notifyDataSetChanged();
    }

    private String buildGroupKey(MessageEntity msg) {
        String title = msg.getTitle() != null ? msg.getTitle() : "";
        String appName = msg.getAppName() != null ? msg.getAppName() : "";
        String sender = msg.getSenderName() != null ? msg.getSenderName() : "";

        if (!sender.isEmpty()) {
            return appName + "|" + normalizeGroupName(sender);
        }
        return appName + "|" + normalizeGroupName(title);
    }

    private String normalizeGroupName(String name) {
        if (name == null || name.isEmpty()) return "";
        return name.replaceAll("[\\(（]\\s*\\d+\\s*[条个]?\\s*(未读|消息|未读消息)?\\s*[\\)）]", "").trim();
    }

    @Override
    public int getItemViewType(int position) {
        if (displayItems.get(position) instanceof MessageGroup) return TYPE_GROUP_HEADER;
        if (childPositions.contains(position)) return TYPE_CHILD_MESSAGE;
        return TYPE_MESSAGE;
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_GROUP_HEADER) {
            View view = inflater.inflate(R.layout.item_message_group, parent, false);
            return new GroupViewHolder(view);
        } else if (viewType == TYPE_CHILD_MESSAGE) {
            View view = inflater.inflate(R.layout.item_message_child, parent, false);
            return new ChildMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message, parent, false);
            return new MessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof GroupViewHolder) {
            ((GroupViewHolder) holder).bind((MessageGroup) displayItems.get(position));
        } else if (holder instanceof ChildMessageViewHolder) {
            ((ChildMessageViewHolder) holder).bind((MessageEntity) displayItems.get(position));
        } else if (holder instanceof MessageViewHolder) {
            ((MessageViewHolder) holder).bind((MessageEntity) displayItems.get(position));
        }
    }

    class GroupViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvGroupName, tvGroupCount, tvGroupImportance, tvGroupTime, tvGroupPreview;
        private final ImageView ivExpand, btnGroupOpenApp, btnGroupDelete;
        private final View importanceBar;
        private final LinearLayout layoutGroupContent;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupName = itemView.findViewById(R.id.tvGroupName);
            tvGroupCount = itemView.findViewById(R.id.tvGroupCount);
            tvGroupImportance = itemView.findViewById(R.id.tvGroupImportance);
            tvGroupTime = itemView.findViewById(R.id.tvGroupTime);
            tvGroupPreview = itemView.findViewById(R.id.tvGroupPreview);
            ivExpand = itemView.findViewById(R.id.ivExpand);
            importanceBar = itemView.findViewById(R.id.importanceBar);
            layoutGroupContent = itemView.findViewById(R.id.layoutGroupContent);
            btnGroupOpenApp = itemView.findViewById(R.id.btnGroupOpenApp);
            btnGroupDelete = itemView.findViewById(R.id.btnGroupDelete);
        }

        void bind(MessageGroup group) {
            MessageEntity latest = group.messages.get(0);
            String appName = latest.getAppName() != null ? latest.getAppName() : "";
            String sender = latest.getSenderName() != null ? latest.getSenderName() : "";
            String title = latest.getTitle() != null ? latest.getTitle() : "";

            String displayName = !sender.isEmpty() ? sender : title;
            tvGroupName.setText(appName + " · " + displayName);

            tvGroupImportance.setText("重要性 " + group.maxImportance + "/10");
            tvGroupTime.setText(formatTime(group.latestTimestamp));

            String preview = latest.getContent() != null ? latest.getContent() : "";
            if (group.messages.size() == 1 && preview.isEmpty()) {
                preview = title;
            }
            if (preview.length() > 50) preview = preview.substring(0, 50) + "...";
            tvGroupPreview.setText(preview);

            int barColor = getImportanceColor(group.maxImportance);
            importanceBar.setBackgroundColor(barColor);

            boolean expanded = Boolean.TRUE.equals(expandedGroups.get(group.key));
            ivExpand.setRotation(expanded ? 180 : 0);

            if (group.messages.size() <= 1) {
                ivExpand.setVisibility(View.GONE);
                tvGroupCount.setVisibility(View.GONE);
            } else {
                ivExpand.setVisibility(View.VISIBLE);
                tvGroupCount.setVisibility(View.VISIBLE);
                tvGroupCount.setText(group.messages.size() + "条消息");
            }

            itemView.setOnClickListener(v -> {
                if (group.messages.size() == 1) {
                    if (listener != null) listener.onMessageClick(group.messages.get(0));
                    return;
                }
                boolean nowExpanded = !Boolean.TRUE.equals(expandedGroups.get(group.key));
                expandedGroups.put(group.key, nowExpanded);
                rebuildDisplayItems();
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null && !group.messages.isEmpty()) {
                    listener.onMessageLongClick(group.messages.get(0));
                }
                return true;
            });

            btnGroupOpenApp.setOnClickListener(v -> {
                if (listener != null && !group.messages.isEmpty()) {
                    listener.onOpenApp(group.messages.get(0));
                }
            });

            btnGroupDelete.setOnClickListener(v -> {
                if (listener != null && !group.messages.isEmpty()) {
                    listener.onMessageDelete(group.messages.get(0));
                }
            });
        }
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        private final View importanceBar;
        private final TextView tvAppName, tvCategory, tvTime, tvTitle, tvContent, tvImportance, tvSender;
        private final ImageView btnDelete, btnOpenApp;

        MessageViewHolder(@NonNull View itemView) {
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
    }

    class ChildMessageViewHolder extends RecyclerView.ViewHolder {
        private final View importanceBar;
        private final TextView tvTitle, tvCategory, tvTime, tvContent, tvImportance;
        private final ImageView btnDelete, btnOpenApp;

        ChildMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            importanceBar = itemView.findViewById(R.id.importanceBar);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvImportance = itemView.findViewById(R.id.tvImportance);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnOpenApp = itemView.findViewById(R.id.btnOpenApp);
        }

        void bind(MessageEntity msg) {
            tvTitle.setText(msg.getTitle());
            tvContent.setText(msg.getContent());
            tvTime.setText(formatTime(msg.getTimestamp()));
            tvImportance.setText(String.format(Locale.getDefault(), "重要性 %d/10", msg.getImportance()));

            tvCategory.setText(msg.getCategory());
            GradientDrawable categoryBg = new GradientDrawable();
            categoryBg.setCornerRadius(10f);
            categoryBg.setColor(getCategoryColor(msg.getCategory()));
            tvCategory.setBackground(categoryBg);

            int barColor = getImportanceColor(msg.getImportance());
            importanceBar.setBackgroundColor(barColor);

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
    }

    private void rebuildDisplayItems() {
        List<MessageGroup> groups = new ArrayList<>();
        for (Object item : displayItems) {
            if (item instanceof MessageGroup) {
                groups.add((MessageGroup) item);
            }
        }

        displayItems.clear();
        childPositions.clear();
        for (MessageGroup group : groups) {
            displayItems.add(group);
            boolean expanded = Boolean.TRUE.equals(expandedGroups.get(group.key));
            if (expanded && group.messages.size() > 1) {
                for (MessageEntity msg : group.messages) {
                    childPositions.add(displayItems.size());
                    displayItems.add(msg);
                }
            }
        }
        notifyDataSetChanged();
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

    static class MessageGroup {
        final String key;
        final List<MessageEntity> messages;
        final int maxImportance;
        final long latestTimestamp;

        MessageGroup(String key, List<MessageEntity> messages, int maxImportance, long latestTimestamp) {
            this.key = key;
            this.messages = messages;
            this.maxImportance = maxImportance;
            this.latestTimestamp = latestTimestamp;
        }
    }
}
