package com.zjf.seeme.service;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.zjf.seeme.data.PrefsManager;

import java.util.concurrent.TimeUnit;

public class WorkScheduler {

    private static final String SUMMARY_WORK_TAG = "seeme_summary_work";

    public static void scheduleSummaryWork(Context context) {
        PrefsManager prefs = PrefsManager.getInstance(context);
        int intervalMinutes = prefs.getPollInterval();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                SummaryNotificationWorker.class,
                Math.max(15, intervalMinutes),
                TimeUnit.MINUTES
        ).build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SUMMARY_WORK_TAG,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
        );
    }

    public static void cancelSummaryWork(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(SUMMARY_WORK_TAG);
    }
}
