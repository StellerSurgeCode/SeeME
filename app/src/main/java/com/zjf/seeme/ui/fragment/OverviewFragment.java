package com.zjf.seeme.ui.fragment;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.zjf.seeme.R;
import com.zjf.seeme.data.AppDatabase;
import com.zjf.seeme.data.entity.MessageEntity;
import com.zjf.seeme.data.entity.TodoEntity;
import com.zjf.seeme.ui.MainActivity;
import com.zjf.seeme.service.NotificationListener;
import com.zjf.seeme.ui.adapter.GroupedMessageAdapter;

import java.util.List;
import java.util.concurrent.Executors;

public class OverviewFragment extends Fragment {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyState;
    private GroupedMessageAdapter adapter;
    private AppDatabase db;
    private LiveData<List<MessageEntity>> currentLiveData;

    public static OverviewFragment newInstance() {
        return new OverviewFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_overview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        emptyState = view.findViewById(R.id.emptyState);

        db = AppDatabase.getInstance(requireContext());

        adapter = new GroupedMessageAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnMessageClickListener(new GroupedMessageAdapter.OnMessageClickListener() {
            @Override
            public void onMessageClick(MessageEntity message) {
                markAsRead(message);
            }

            @Override
            public void onMessageLongClick(MessageEntity message) {
                showMessageOptions(message);
            }

            @Override
            public void onMessageDelete(MessageEntity message) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("删除消息")
                        .setMessage("确定要删除这条消息吗？")
                        .setPositiveButton("删除", (d, w) ->
                                Executors.newSingleThreadExecutor().execute(() ->
                                        db.messageDao().delete(message)))
                        .setNegativeButton("取消", null)
                        .show();
            }

            @Override
            public void onOpenApp(MessageEntity message) {
                launchOriginalApp(message);
            }
        });

        swipeRefresh.setColorSchemeColors(getResources().getColor(R.color.accent, null));
        swipeRefresh.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.surface, null));
        swipeRefresh.setOnRefreshListener(() -> swipeRefresh.setRefreshing(false));

        observeMessages(null);
        setupSearchListener();
    }

    private void launchOriginalApp(MessageEntity message) {
        PendingIntent cached = NotificationListener.getCachedIntent(message.getId());
        if (cached != null) {
            try {
                cached.send();
            } catch (PendingIntent.CanceledException e) {
                Log.d("OverviewFragment", "Cached PendingIntent expired, falling back");
                launchAppByPackage(message);
            }
        } else {
            launchAppByPackage(message);
        }
    }

    private void launchAppByPackage(MessageEntity message) {
        try {
            String packageName = message.getPackageName();
            if (packageName == null || packageName.isEmpty()) return;

            PackageManager pm = requireContext().getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "无法打开应用", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupSearchListener() {
        if (getActivity() instanceof MainActivity) {
            EditText etSearch = getActivity().findViewById(R.id.etSearch);
            if (etSearch != null) {
                etSearch.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        String query = s.toString().trim();
                        observeMessages(query.isEmpty() ? null : query);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });
            }
        }
    }

    private void observeMessages(@Nullable String searchQuery) {
        if (currentLiveData != null) {
            currentLiveData.removeObservers(getViewLifecycleOwner());
        }

        if (searchQuery != null && !searchQuery.isEmpty()) {
            currentLiveData = db.messageDao().search(searchQuery);
        } else {
            currentLiveData = db.messageDao().getAllExcludeAds();
        }

        currentLiveData.observe(getViewLifecycleOwner(), messages -> {
            if (messages == null || messages.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.submitList(messages);
            }
        });
    }

    private void markAsRead(MessageEntity message) {
        if (!message.isRead()) {
            Executors.newSingleThreadExecutor().execute(() ->
                    db.messageDao().markAsRead(message.getId()));
        }
    }

    private void showMessageOptions(MessageEntity message) {
        String[] options = {"打开原始应用", "标记已读", "转为待办", "删除消息"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(message.getTitle())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            launchOriginalApp(message);
                            break;
                        case 1:
                            Executors.newSingleThreadExecutor().execute(() ->
                                    db.messageDao().markAsRead(message.getId()));
                            break;
                        case 2:
                            Executors.newSingleThreadExecutor().execute(() -> convertToTodo(message));
                            break;
                        case 3:
                            Executors.newSingleThreadExecutor().execute(() ->
                                    db.messageDao().delete(message));
                            break;
                    }
                })
                .show();
    }

    private void convertToTodo(MessageEntity message) {
        TodoEntity todo = new TodoEntity();
        todo.setMessageId(message.getId());
        todo.setTitle(message.getTitle());
        todo.setContent(message.getContent());
        todo.setFromApp(message.getAppName());
        todo.setCreatedAt(System.currentTimeMillis());
        todo.setDone(false);
        todo.setPriority(Math.min(5, Math.max(1, message.getImportance() / 2)));

        db.todoDao().insert(todo);

        if (getActivity() != null) {
            getActivity().runOnUiThread(() ->
                    Snackbar.make(requireView(), "已添加到待办", Snackbar.LENGTH_SHORT).show());
        }
    }
}
