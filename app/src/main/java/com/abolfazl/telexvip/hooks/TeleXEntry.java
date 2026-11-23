package com.abolfazl.telexvip.hooks;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class TeleXEntry extends XposedModule {
    private static final String TARGET = "org.telegram.messenger";
    private static final String TAG = "TeleVIP";
    private static volatile boolean hideTlError = false;
    private static volatile boolean menuInstalled = false;

    public TeleXEntry() { super(); }

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        log(android.util.Log.INFO, TAG, "TeleVIP loaded. API=" + getApiVersion()
                + ", framework=" + getFrameworkName() + " " + getFrameworkVersion());
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        if (!TARGET.equals(param.getPackageName())) return;
        try {
            Method resume = Activity.class.getDeclaredMethod("onResume");
            hook(resume).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object object = chain.getThisObject();
                        if (object instanceof Activity) {
                            Activity activity = (Activity) object;
                            if (activity.getClass().getName().contains("LaunchActivity")) {
                                activity.runOnUiThread(() -> installEntry(activity));
                            }
                        }
                        return result;
                    });

            Method toastShow = Toast.class.getDeclaredMethod("show");
            hook(toastShow).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object object = chain.getThisObject();
                        if (hideTlError && object instanceof Toast && isTlError((Toast) object)) {
                            return null;
                        }
                        return chain.proceed();
                    });
            log(android.util.Log.INFO, TAG, "Telegram hooks installed.");
        } catch (Throwable t) {
            log(android.util.Log.ERROR, TAG, "Hook installation failed: " + t);
        }
    }

    private static boolean isTlError(Toast toast) {
        try {
            View view = toast.getView();
            return containsTlError(view);
        } catch (Throwable ignored) { return false; }
    }

    private static boolean containsTlError(View view) {
        if (view == null) return false;
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            if (text != null && text.toString().toLowerCase().contains("tl error")) return true;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (containsTlError(group.getChildAt(i))) return true;
            }
        }
        return false;
    }

    private static void installEntry(Activity activity) {
        try {
            View decor = activity.getWindow().getDecorView();
            if (!(decor instanceof ViewGroup) || decor.findViewWithTag("televip_entry") != null) return;
            TextView chip = new TextView(activity);
            chip.setTag("televip_entry");
            chip.setText("TV");
            chip.setTextColor(Color.WHITE);
            chip.setTextSize(14);
            chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            chip.setGravity(Gravity.CENTER);
            GradientDrawable bg = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{Color.rgb(145, 75, 235), Color.rgb(83, 45, 160)});
            bg.setCornerRadius(dp(activity, 28));
            chip.setBackground(bg);
            chip.setElevation(dp(activity, 8));
            chip.setOnClickListener(v -> showPanel(activity));
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(activity, 48), dp(activity, 48),
                    Gravity.END | Gravity.CENTER_VERTICAL);
            lp.rightMargin = dp(activity, 8);
            ((ViewGroup) decor).addView(chip, lp);
        } catch (Throwable ignored) { }
    }

    private static void showPanel(Activity activity) {
        LinearLayout page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(activity, 18), dp(activity, 12), dp(activity, 18), dp(activity, 8));
        page.setBackgroundColor(Color.rgb(25, 21, 35));

        TextView title = new TextView(activity);
        title.setText("Abolfazl TeleVIP");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        page.addView(title);

        TextView subtitle = new TextView(activity);
        subtitle.setText("Telegram extension  •  0.2.0");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setTextSize(12);
        page.addView(subtitle);

        addSection(activity, page, "PRIVACY");
        addToggle(activity, page, "Hide TL error", "Hides Telegram's TL error toast when its text is available.", hideTlError, value -> hideTlError = value, true);
        addToggle(activity, page, "Hide Online / Typing", "Not implemented in this build.", false, value -> {}, false);
        addToggle(activity, page, "Hide Seen / Story View", "Not implemented in this build.", false, value -> {}, false);

        addSection(activity, page, "CHATS");
        addToggle(activity, page, "Message ID / Details", "Not implemented in this build.", false, value -> {}, false);
        addToggle(activity, page, "Deleted Message History", "Not implemented in this build.", false, value -> {}, false);

        addSection(activity, page, "APPEARANCE & TOOLS");
        addToggle(activity, page, "Liquid Glass", "Not implemented in this build.", false, value -> {}, false);
        addToggle(activity, page, "Media utilities", "Not implemented in this build.", false, value -> {}, false);

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(page);
        new AlertDialog.Builder(activity)
                .setView(scroll)
                .setPositiveButton("Close", null)
                .show();
    }

    private interface Toggle { void set(boolean value); }

    private static void addSection(Activity a, LinearLayout page, String name) {
        TextView t = new TextView(a);
        t.setText(name);
        t.setTextColor(Color.rgb(190, 145, 255));
        t.setTextSize(12);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(0, dp(a, 18), 0, dp(a, 6));
        page.addView(t);
    }

    private static void addToggle(Activity a, LinearLayout page, String name, String description,
                                  boolean checked, Toggle callback, boolean enabled) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(a, 10), 0, dp(a, 10));
        TextView label = new TextView(a);
        label.setText(name + (enabled ? "" : "  ·  SOON"));
        label.setTextColor(enabled ? Color.WHITE : Color.GRAY);
        label.setTextSize(16);
        row.addView(label);
        TextView desc = new TextView(a);
        desc.setText(description);
        desc.setTextColor(Color.LTGRAY);
        desc.setTextSize(12);
        row.addView(desc);
        if (enabled) {
            android.widget.Switch sw = new android.widget.Switch(a);
            sw.setText("Enabled");
            sw.setTextColor(Color.WHITE);
            sw.setChecked(checked);
            sw.setOnCheckedChangeListener((button, value) -> callback.set(value));
            row.addView(sw);
        }
        page.addView(row);
        View divider = new View(a);
        divider.setBackgroundColor(Color.rgb(65, 58, 76));
        page.addView(divider, new LinearLayout.LayoutParams(-1, dp(a, 1)));
    }

    private static int dp(Activity a, int value) {
        return (int) (value * a.getResources().getDisplayMetrics().density + 0.5f);
    }
}
