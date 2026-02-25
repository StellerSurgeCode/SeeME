package com.zjf.seeme.ui.adapter;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zjf.seeme.R;
import com.zjf.seeme.data.entity.CategoryEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_CATEGORY = 1;

    private List<CategoryEntity> categories = new ArrayList<>();
    private Map<String, Integer> unreadCounts;
    private Map<String, Integer> totalCounts;
    private OnCategoryClickListener listener;

    private int statTotal, statUnread, statCategories, statTodo;

    public interface OnCategoryClickListener {
        void onCategoryClick(CategoryEntity category);
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void setCategories(List<CategoryEntity> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    public void setUnreadCounts(Map<String, Integer> counts) {
        this.unreadCounts = counts;
        notifyDataSetChanged();
    }

    public void setTotalCounts(Map<String, Integer> counts) {
        this.totalCounts = counts;
        notifyDataSetChanged();
    }

    public void updateStats(int total, int unread, int categoryCount, int todo) {
        this.statTotal = total;
        this.statUnread = unread;
        this.statCategories = categoryCount;
        this.statTodo = todo;
        notifyItemChanged(0);
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_HEADER : TYPE_CATEGORY;
    }

    @Override
    public int getItemCount() {
        return categories.size() + 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_category_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_category, parent, false);
            return new CategoryViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind();
        } else if (holder instanceof CategoryViewHolder) {
            CategoryEntity cat = categories.get(position - 1);
            ((CategoryViewHolder) holder).bind(cat);
        }
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTotal, tvUnread, tvCategories, tvTodo;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTotal = itemView.findViewById(R.id.tvStatTotal);
            tvUnread = itemView.findViewById(R.id.tvStatUnread);
            tvCategories = itemView.findViewById(R.id.tvStatCategories);
            tvTodo = itemView.findViewById(R.id.tvStatTodo);
        }

        void bind() {
            tvTotal.setText(String.valueOf(statTotal));
            tvUnread.setText(String.valueOf(statUnread));
            tvCategories.setText(String.valueOf(statCategories));
            tvTodo.setText(String.valueOf(statTodo));
        }
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivIcon;
        private final View viewIconBg;
        private final TextView tvName, tvUnread, tvBadge, tvTotalCount;
        private final View viewProgress;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivCategoryIcon);
            viewIconBg = itemView.findViewById(R.id.viewIconBg);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            tvUnread = itemView.findViewById(R.id.tvUnreadCount);
            tvBadge = itemView.findViewById(R.id.tvBadge);
            tvTotalCount = itemView.findViewById(R.id.tvTotalCount);
            viewProgress = itemView.findViewById(R.id.viewProgress);
        }

        void bind(CategoryEntity cat) {
            tvName.setText(cat.getName());

            int iconRes = getCategoryIcon(cat.getName());
            ivIcon.setImageResource(iconRes);

            int catColor = getCategoryColor(cat.getName());

            GradientDrawable iconBg = new GradientDrawable();
            iconBg.setShape(GradientDrawable.OVAL);
            iconBg.setColor((catColor & 0x00FFFFFF) | 0x30000000);
            viewIconBg.setBackground(iconBg);
            ivIcon.setColorFilter(catColor);

            int unread = 0;
            if (unreadCounts != null && unreadCounts.containsKey(cat.getName())) {
                unread = unreadCounts.get(cat.getName());
            }

            int total = 0;
            if (totalCounts != null && totalCounts.containsKey(cat.getName())) {
                total = totalCounts.get(cat.getName());
            }

            if (unread > 0) {
                tvUnread.setText(String.format("%d 条未读", unread));
                tvBadge.setVisibility(View.VISIBLE);
                tvBadge.setText(String.valueOf(unread));

                GradientDrawable badgeBg = new GradientDrawable();
                badgeBg.setCornerRadius(20f);
                badgeBg.setColor(catColor);
                tvBadge.setBackground(badgeBg);
            } else {
                tvUnread.setText("暂无未读消息");
                tvBadge.setVisibility(View.GONE);
            }

            tvTotalCount.setText(String.format("共 %d 条", total));

            if (total > 0 && viewProgress != null) {
                float ratio = Math.min(1f, (float) unread / total);
                viewProgress.post(() -> {
                    ViewGroup.LayoutParams lp = viewProgress.getLayoutParams();
                    int parentWidth = ((View) viewProgress.getParent()).getWidth();
                    lp.width = Math.max(0, (int) (parentWidth * ratio));
                    viewProgress.setLayoutParams(lp);

                    GradientDrawable progressBg = new GradientDrawable();
                    progressBg.setCornerRadius(4f);
                    progressBg.setColor(catColor);
                    viewProgress.setBackground(progressBg);
                });
            } else if (viewProgress != null) {
                ViewGroup.LayoutParams lp = viewProgress.getLayoutParams();
                lp.width = 0;
                viewProgress.setLayoutParams(lp);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onCategoryClick(cat);
            });
        }

        private int getCategoryIcon(String name) {
            switch (name) {
                case "工作": return R.drawable.ic_work;
                case "生活": return R.drawable.ic_life;
                case "娱乐": return R.drawable.ic_entertainment;
                case "广告推销": return R.drawable.ic_ads;
                default: return R.drawable.ic_general;
            }
        }

        private int getCategoryColor(String name) {
            switch (name) {
                case "工作": return 0xFF4A90D9;
                case "生活": return 0xFF50C878;
                case "娱乐": return 0xFFE8A838;
                case "广告推销": return 0xFFD94A4A;
                default: return 0xFF7B68AE;
            }
        }
    }
}
