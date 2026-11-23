package com.abolfazl.telexvip.hooks;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class TeleXEntry extends XposedModule {
    private static final String TARGET = "org.telegram.messenger";
    private static final String SETTINGS_CLASS = "org.telegram.ui.SettingsActivity";
    private static final String TAG = "TeleVIP";
    private static volatile boolean hideTlError;
    private static final String PREFS = "televip_settings";

    public TeleXEntry() { super(); }

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        log(android.util.Log.INFO, TAG, "TeleVIP loaded; API=" + getApiVersion());
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        if (!TARGET.equals(param.getPackageName())) return;
        try {
            ClassLoader loader = param.getClassLoader();
            Class<?> settings = loader.loadClass(SETTINGS_CLASS);
            Method createView = null;
            for (Method method : settings.getDeclaredMethods()) {
                if (method.getName().equals("createView") && method.getParameterTypes().length == 1
                        && Context.class.isAssignableFrom(method.getParameterTypes()[0])
                        && View.class.isAssignableFrom(method.getReturnType())) {
                    createView = method;
                    break;
                }
            }
            if (createView == null) throw new NoSuchMethodException("SettingsActivity.createView(Context)");
            hook(createView).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (result instanceof View) {
                            View root = (View) result;
                            Context context = root.getContext();
                            root.post(() -> insertSettingsRow(context, root));
                        }
                        return result;
                    });

            Method toastShow = Toast.class.getDeclaredMethod("show");
            hook(toastShow).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object object = chain.getThisObject();
                        if (hideTlError && object instanceof Toast && isTlError((Toast) object)) return null;
                        return chain.proceed();
                    });
            log(android.util.Log.INFO, TAG, "SettingsActivity hook installed.");
        } catch (Throwable t) {
            log(android.util.Log.ERROR, TAG, "TeleVIP settings hook failed: " + t);
        }
    }

    private static void insertSettingsRow(Context context, View root) {
        try {
            if (!(root instanceof ViewGroup) || root.findViewWithTag("televip_settings_row") != null) return;
            View list = findTelegramList(root);
            if (list == null) {
                android.util.Log.w(TAG, "Telegram settings list not found; no floating fallback will be added.");
                return;
            }
            ViewGroup parent = (ViewGroup) list.getParent();
            if (parent == null) return;
            int index = parent.indexOfChild(list);
            ViewGroup.LayoutParams oldParams = list.getLayoutParams();
            parent.removeView(list);

            LinearLayout wrapper = new LinearLayout(context);
            wrapper.setOrientation(LinearLayout.VERTICAL);
            TextView row = new TextView(context);
            row.setTag("televip_settings_row");
            row.setText("✦  Abolfazl TeleVIP\n     Privacy, Chats, Appearance & Tools");
            row.setTextColor(Color.rgb(45, 135, 205));
            row.setTextSize(16);
            row.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            row.setPadding(dp(context, 22), dp(context, 15), dp(context, 18), dp(context, 15));
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.rgb(248, 249, 252));
            bg.setCornerRadius(dp(context, 14));
            row.setBackground(bg);
            row.setOnClickListener(v -> showPanel(context));
            wrapper.addView(row, new LinearLayout.LayoutParams(-1, -2));
            wrapper.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));
            parent.addView(wrapper, index, oldParams);
        } catch (Throwable t) {
            android.util.Log.e(TAG, "Could not insert TeleVIP settings row", t);
        }
    }

    private static View findTelegramList(View view) {
        if (view == null) return null;
        String name = view.getClass().getName();
        if (name.contains("RecyclerListView")) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findTelegramList(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void showPanel(Context context) {
        LinearLayout page = new LinearLayout(context);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(context, 20), dp(context, 14), dp(context, 20), dp(context, 8));
        page.setBackgroundColor(Color.rgb(248, 249, 252));
        TextView title = new TextView(context);
        title.setText("Abolfazl TeleVIP");
        title.setTextColor(Color.rgb(35, 35, 42));
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        page.addView(title);
        TextView tabs = new TextView(context);
        tabs.setText("PRIVACY     CHATS     APPEARANCE     TOOLS");
        tabs.setTextColor(Color.rgb(45, 135, 205));
        tabs.setTextSize(13);
        tabs.setPadding(0, dp(context, 14), 0, dp(context, 14));
        page.addView(tabs);
        addToggle(context, page, "Hide TL error", "Hides the TL error toast.", true);
        addToggle(context, page, "Ghost Mode", "Not implemented yet.", false);
        addToggle(context, page, "Hide Online / Typing", "Not implemented yet.", false);
        addToggle(context, page, "Deleted Message History", "Not implemented yet.", false);
        addToggle(context, page, "Media Tools", "Not implemented yet.", false);
        new AlertDialog.Builder(context).setView(page).setPositiveButton("Close", null).show();
    }

    private static void addToggle(Context context, LinearLayout page, String label, String description, boolean available) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(context, 10), 0, dp(context, 10));
        TextView name = new TextView(context);
        name.setText(label + (available ? "" : "  ·  SOON"));
        name.setTextColor(Color.rgb(35, 35, 42));
        name.setTextSize(16);
        row.addView(name);
        TextView desc = new TextView(context);
        desc.setText(description);
        desc.setTextColor(Color.DKGRAY);
        desc.setTextSize(12);
        row.addView(desc);
        if (available) {
            android.widget.Switch sw = new android.widget.Switch(context);
            sw.setText("Enabled");
            android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sw.setChecked(prefs.getBoolean("hide_tl_error", false));
            sw.setOnCheckedChangeListener((button, checked) -> {
                prefs.edit().putBoolean("hide_tl_error", checked).apply();
                hideTlError = checked;
            });
            row.addView(sw);
        }
        page.addView(row);
        View divider = new View(context);
        divider.setBackgroundColor(Color.LTGRAY);
        page.addView(divider, new LinearLayout.LayoutParams(-1, dp(context, 1)));
    }

    private static boolean isTlError(Toast toast) {
        try { return containsTlError(toast.getView()); } catch (Throwable ignored) { return false; }
    }

    private static boolean containsTlError(View view) {
        if (view == null) return false;
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            if (text != null && text.toString().toLowerCase().contains("tl error")) return true;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) if (containsTlError(group.getChildAt(i))) return true;
        }
        return false;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
