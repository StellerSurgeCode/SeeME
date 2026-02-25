package com.zjf.seeme.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zjf.seeme.R;
import com.zjf.seeme.data.AppDatabase;
import com.zjf.seeme.data.entity.TodoEntity;
import com.zjf.seeme.ui.adapter.TodoAdapter;

import java.util.concurrent.Executors;

public class TodoFragment extends Fragment {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private TodoAdapter adapter;
    private AppDatabase db;

    public static TodoFragment newInstance() {
        return new TodoFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_todo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerTodos);
        emptyState = view.findViewById(R.id.emptyState);
        db = AppDatabase.getInstance(requireContext());

        adapter = new TodoAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnTodoActionListener(new TodoAdapter.OnTodoActionListener() {
            @Override
            public void onToggleDone(TodoEntity todo, boolean done) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    db.todoDao().setDone(todo.getId(), done);
                });
            }

            @Override
            public void onDelete(TodoEntity todo) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    db.todoDao().delete(todo);
                });
            }
        });

        db.todoDao().getAllTodos().observe(getViewLifecycleOwner(), todos -> {
            if (todos == null || todos.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.submitList(todos);
            }
        });
    }
}
