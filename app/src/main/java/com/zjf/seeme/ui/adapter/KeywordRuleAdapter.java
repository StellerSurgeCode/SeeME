package com.zjf.seeme.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.zjf.seeme.R;
import com.zjf.seeme.data.entity.KeywordRule;

public class KeywordRuleAdapter extends ListAdapter<KeywordRule, KeywordRuleAdapter.ViewHolder> {

    private OnRuleActionListener listener;

    public interface OnRuleActionListener {
        void onToggle(KeywordRule rule, boolean enabled);
        void onDelete(KeywordRule rule);
    }

    public KeywordRuleAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnRuleActionListener(OnRuleActionListener listener) {
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<KeywordRule> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<KeywordRule>() {
                @Override
                public boolean areItemsTheSame(@NonNull KeywordRule oldItem, @NonNull KeywordRule newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull KeywordRule oldItem, @NonNull KeywordRule newItem) {
                    return oldItem.getKeyword().equals(newItem.getKeyword())
                            && oldItem.isEnabled() == newItem.isEnabled();
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_keyword_rule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvKeyword, tvMatchField, tvBoost, tvAiHint;
        private final MaterialSwitch switchEnabled;
        private final ImageView btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvKeyword = itemView.findViewById(R.id.tvKeyword);
            tvMatchField = itemView.findViewById(R.id.tvMatchField);
            tvBoost = itemView.findViewById(R.id.tvBoost);
            tvAiHint = itemView.findViewById(R.id.tvAiHint);
            switchEnabled = itemView.findViewById(R.id.switchEnabled);
            btnDelete = itemView.findViewById(R.id.btnDeleteRule);
        }

        void bind(KeywordRule rule) {
            tvKeyword.setText("「" + rule.getKeyword() + "」");

            String fieldLabel;
            switch (rule.getMatchField()) {
                case "title": fieldLabel = "标题"; break;
                case "content": fieldLabel = "内容"; break;
                case "sender": fieldLabel = "发送者"; break;
                case "app": fieldLabel = "应用"; break;
                default: fieldLabel = "全部"; break;
            }
            tvMatchField.setText(fieldLabel);

            int boost = rule.getImportanceBoost();
            if (boost > 0) {
                tvBoost.setText("+" + boost);
                tvBoost.setTextColor(0xFF50C878);
            } else if (boost < 0) {
                tvBoost.setText(String.valueOf(boost));
                tvBoost.setTextColor(0xFFE53935);
            } else {
                tvBoost.setText("±0");
                tvBoost.setTextColor(0xFF78909C);
            }

            if (rule.getAiHint() != null && !rule.getAiHint().isEmpty()) {
                tvAiHint.setVisibility(View.VISIBLE);
                tvAiHint.setText("AI提示: " + rule.getAiHint());
            } else {
                tvAiHint.setVisibility(View.GONE);
            }

            switchEnabled.setOnCheckedChangeListener(null);
            switchEnabled.setChecked(rule.isEnabled());
            switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) listener.onToggle(rule, isChecked);
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(rule);
            });
        }
    }
}
