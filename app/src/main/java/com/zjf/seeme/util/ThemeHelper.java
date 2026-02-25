package com.zjf.seeme.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.Log;
import android.util.TypedValue;

import com.zjf.seeme.R;
import com.zjf.seeme.data.PrefsManager;

public class ThemeHelper {

    private static final String TAG = "ThemeHelper";

    public static void applyTheme(Activity activity) {
        PrefsManager prefs = PrefsManager.getInstance(activity);
        String color = prefs.getThemeColor();

        Log.d(TAG, "applyTheme: saved color = " + color);

        if (color == null || color.isEmpty()) {
            activity.setTheme(R.style.Theme_SeeME);
            return;
        }

        switch (color.toUpperCase()) {
            case "#50C878":
                activity.setTheme(R.style.Theme_SeeME_Green);
                break;
            case "#7B68AE":
                activity.setTheme(R.style.Theme_SeeME_Purple);
                break;
            case "#E8A838":
                activity.setTheme(R.style.Theme_SeeME_Orange);
                break;
            case "#E53935":
                activity.setTheme(R.style.Theme_SeeME_Red);
                break;
            case "#4A90D9":
            default:
                activity.setTheme(R.style.Theme_SeeME);
                break;
        }
    }

    public static int getThemeBackground(Context context) {
        TypedValue tv = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorBackground, tv, true);
        return tv.data;
    }

    public static int getThemeSurface(Context context) {
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, tv, true)) {
            return tv.data;
        }
        return getThemeBackground(context);
    }

    public static int getThemeSurfaceVariant(Context context) {
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceVariant, tv, true)) {
            return tv.data;
        }
        int surface = getThemeSurface(context);
        int r = Math.min(255, Color.red(surface) + 12);
        int g = Math.min(255, Color.green(surface) + 12);
        int b = Math.min(255, Color.blue(surface) + 12);
        return Color.rgb(r, g, b);
    }

    public static int getThemePrimary(Context context) {
        TypedValue tv = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorPrimary, tv, true);
        return tv.data;
    }
}
