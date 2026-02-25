package com.zjf.seeme.ui.adapter;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.zjf.seeme.R;
import com.zjf.seeme.data.entity.TodoEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TodoAdapter extends ListAdapter<TodoEntity, TodoAdapter.ViewHolder> {

    private OnTodoActionListener listener;

    public interface OnTodoActionListener {
        void onToggleDone(TodoEntity todo, boolean done);
        void onDelete(TodoEntity todo);
    }

    public TodoAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnTodoActionListener(OnTodoActionListener listener) {
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<TodoEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<TodoEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull TodoEntity oldItem, @NonNull TodoEntity newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull TodoEntity oldItem, @NonNull TodoEntity newItem) {
                    return oldItem.isDone() == newItem.isDone()
                            && oldItem.getTitle().equals(newItem.getTitle());
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_todo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCheckBox cbDone;
        private final TextView tvTitle, tvContent, tvFrom, tvTime;
        private final ImageView btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbDone = itemView.findViewById(R.id.cbDone);
            tvTitle = itemView.findViewById(R.id.tvTodoTitle);
            tvContent = itemView.findViewById(R.id.tvTodoContent);
            tvFrom = itemView.findViewById(R.id.tvTodoFrom);
            tvTime = itemView.findViewById(R.id.tvTodoTime);
            btnDelete = itemView.findViewById(R.id.btnDeleteTodo);
        }

        void bind(TodoEntity todo) {
            cbDone.setChecked(todo.isDone());
            tvTitle.setText(todo.getTitle());

            if (todo.isDone()) {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvTitle.setAlpha(0.5f);
            } else {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                tvTitle.setAlpha(1.0f);
            }

            if (todo.getContent() != null && !todo.getContent().isEmpty()) {
                tvContent.setVisibility(View.VISIBLE);
                tvContent.setText(todo.getContent());
            } else {
                tvContent.setVisibility(View.GONE);
            }

            if (todo.getFromApp() != null && !todo.getFromApp().isEmpty()) {
                tvFrom.setVisibility(View.VISIBLE);
                tvFrom.setText("来自 " + todo.getFromApp());
            } else {
                tvFrom.setVisibility(View.GONE);
            }

            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
            tvTime.setText(sdf.format(new Date(todo.getCreatedAt())));

            cbDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) listener.onToggleDone(todo, isChecked);
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(todo);
            });
        }
    }
}
