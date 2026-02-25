package com.zjf.seeme.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zjf.seeme.R;
import com.zjf.seeme.data.AppDatabase;
import com.zjf.seeme.data.entity.CategoryEntity;
import com.zjf.seeme.ui.CategoryDetailActivity;
import com.zjf.seeme.ui.adapter.CategoryAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class CategoriesFragment extends Fragment {

    private RecyclerView recyclerView;
    private CategoryAdapter adapter;
    private AppDatabase db;
    private final Map<String, Integer> unreadCountsMap = new HashMap<>();
    private final Map<String, Integer> totalCountsMap = new HashMap<>();

    private int cachedTotal = 0, cachedUnread = 0, cachedCatCount = 0, cachedTodo = 0;

    public static CategoriesFragment newInstance() {
        return new CategoriesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_categories, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerCategories);
        db = AppDatabase.getInstance(requireContext());

        adapter = new CategoryAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnCategoryClickListener(category -> {
            Intent intent = new Intent(requireContext(), CategoryDetailActivity.class);
            intent.putExtra("category_name", category.getName());
            startActivity(intent);
        });

        observeCategories();
        loadStats();
    }

    private void observeCategories() {
        db.categoryDao().getAll().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null) {
                adapter.setCategories(categories);
                loadUnreadCounts(categories);
                loadTotalCounts(categories);
                cachedCatCount = categories.size();
                refreshHeaderStats();
            }
        });
    }

    private void loadUnreadCounts(List<CategoryEntity> categories) {
        for (CategoryEntity cat : categories) {
            db.messageDao().getUnreadCountByCategory(cat.getName())
                    .observe(getViewLifecycleOwner(), count -> {
                        unreadCountsMap.put(cat.getName(), count != null ? count : 0);
                        adapter.setUnreadCounts(new HashMap<>(unreadCountsMap));
                    });
        }
    }

    private void loadTotalCounts(List<CategoryEntity> categories) {
        Executors.newSingleThreadExecutor().execute(() -> {
            for (CategoryEntity cat : categories) {
                int count = db.messageDao().getTotalCountByCategorySync(cat.getName());
                totalCountsMap.put(cat.getName(), count);
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() ->
                        adapter.setTotalCounts(new HashMap<>(totalCountsMap)));
            }
        });
    }

    private void loadStats() {
        db.messageDao().getTotalUnreadCount().observe(getViewLifecycleOwner(), count -> {
            cachedUnread = count != null ? count : 0;
            refreshHeaderStats();
        });

        db.todoDao().getActiveCount().observe(getViewLifecycleOwner(), count -> {
            cachedTodo = count != null ? count : 0;
            refreshHeaderStats();
        });

        Executors.newSingleThreadExecutor().execute(() -> {
            int total = db.messageDao().getTotalCountSync();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    cachedTotal = total;
                    refreshHeaderStats();
                });
            }
        });
    }

    private void refreshHeaderStats() {
        adapter.updateStats(cachedTotal, cachedUnread, cachedCatCount, cachedTodo);
    }
}
