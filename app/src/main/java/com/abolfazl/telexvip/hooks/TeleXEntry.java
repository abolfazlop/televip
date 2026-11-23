package com.abolfazl.telexvip.hooks;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class TeleXEntry extends XposedModule {
    private static final String TARGET = "org.telegram.messenger";
    private static final String SETTINGS_CLASS = "org.telegram.ui.SettingsActivity";
    private static final String TAG = "TeleVIP";
    private static volatile boolean hideTlError;
    private static final Set<Toast> SUPPRESSED = Collections.newSetFromMap(new WeakHashMap<Toast, Boolean>());

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
                Class<?>[] args = method.getParameterTypes();
                if (method.getName().equals("createView") && args.length == 1
                        && Context.class.isAssignableFrom(args[0])
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
                            root.post(() -> insertSettingsEntry(root));
                        }
                        return result;
                    });

            for (Method method : Toast.class.getDeclaredMethods()) {
                if (method.getName().equals("makeText") && method.getParameterTypes().length == 3
                        && method.getParameterTypes()[0] == Context.class) {
                    hook(method).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                            .intercept(chain -> {
                                Object result = chain.proceed();
                                Object text = chain.getArg(1);
                                if (result instanceof Toast && text != null
                                        && text.toString().toLowerCase().contains("tl error")) {
                                    synchronized (SUPPRESSED) { SUPPRESSED.add((Toast) result); }
                                }
                                return result;
                            });
                }
            }
            Method show = Toast.class.getDeclaredMethod("show");
            hook(show).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object object = chain.getThisObject();
                        synchronized (SUPPRESSED) {
                            if (hideTlError && object instanceof Toast && SUPPRESSED.contains(object)) return null;
                        }
                        return chain.proceed();
                    });
            log(android.util.Log.INFO, TAG, "Telegram Settings entry and TL-error hook installed.");
        } catch (Throwable t) {
            log(android.util.Log.ERROR, TAG, "TeleVIP hook setup failed: " + t);
        }
    }

    private static void insertSettingsEntry(View root) {
        try {
            if (!(root instanceof ViewGroup) || root.findViewWithTag("televip_settings_row") != null) return;
            View list = findTelegramList(root);
            if (list == null || !(list.getParent() instanceof ViewGroup)) {
                android.util.Log.w(TAG, "Settings list not found; entry not inserted.");
                return;
            }
            ViewGroup parent = (ViewGroup) list.getParent();
            TextView row = new TextView(root.getContext());
            row.setTag("televip_settings_row");
            row.setText("✦  Abolfazl TeleVIP     ›\n     Privacy  ·  Chats  ·  Appearance  ·  Tools");
            row.setTextColor(Color.rgb(42, 132, 205));
            row.setTextSize(16);
            row.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(root.getContext(), 20), dp(root.getContext(), 12),
                    dp(root.getContext(), 16), dp(root.getContext(), 12));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.rgb(247, 249, 252));
            bg.setCornerRadius(dp(root.getContext(), 12));
            row.setBackground(bg);
            row.setOnClickListener(v -> showPanel(root.getContext()));

            if (parent instanceof FrameLayout) {
                FrameLayout.LayoutParams rowParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(root.getContext(), 68), Gravity.TOP);
                rowParams.leftMargin = dp(root.getContext(), 12);
                rowParams.rightMargin = dp(root.getContext(), 12);
                rowParams.topMargin = dp(root.getContext(), 8);
                FrameLayout.LayoutParams listParams = (FrameLayout.LayoutParams) list.getLayoutParams();
                listParams.topMargin += dp(root.getContext(), 84);
                list.setLayoutParams(listParams);
                parent.addView(row, rowParams);
            } else if (parent instanceof LinearLayout) {
                int index = parent.indexOfChild(list);
                parent.addView(row, index, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(root.getContext(), 68)));
            } else {
                android.util.Log.w(TAG, "Unsupported settings container: " + parent.getClass().getName());
            }
        } catch (Throwable t) {
            android.util.Log.e(TAG, "Settings entry insertion failed", t);
        }
    }

    private static View findTelegramList(View view) {
        if (view == null) return null;
        if (view.getClass().getName().contains("RecyclerListView")) return view;
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
        page.setPadding(dp(context, 20), dp(context, 16), dp(context, 20), dp(context, 12));
        page.setBackgroundColor(Color.rgb(247, 249, 252));
        TextView title = new TextView(context);
        title.setText("Abolfazl TeleVIP");
        title.setTextColor(Color.rgb(35, 35, 42));
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        page.addView(title);
        TextView tabs = new TextView(context);
        tabs.setText("PRIVACY     CHATS     APPEARANCE     TOOLS");
        tabs.setTextColor(Color.rgb(42, 132, 205));
        tabs.setTextSize(13);
        tabs.setPadding(0, dp(context, 14), 0, dp(context, 14));
        page.addView(tabs);
        addWorkingToggle(context, page);
        addUnavailable(page, "Ghost Mode", "Requires Telegram last-seen and presence hooks; not enabled in this build.");
        addUnavailable(page, "Hide Typing / Seen", "Requires message action hooks; not enabled in this build.");
        addUnavailable(page, "Deleted Message History", "Not enabled in this build.");
        addUnavailable(page, "Media Download Tools", "Not enabled in this build.");
        addUnavailable(page, "Liquid Glass", "Not enabled in this build.");
        new AlertDialog.Builder(context).setView(page).setPositiveButton("Close", null).show();
    }

    private static void addWorkingToggle(Context context, LinearLayout page) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(context, 10), 0, dp(context, 10));
        TextView title = new TextView(context);
        title.setText("Hide TL error");
        title.setTextColor(Color.rgb(35, 35, 42));
        title.setTextSize(16);
        row.addView(title);
        TextView description = new TextView(context);
        description.setText("Suppresses TL-error Toast messages created through Toast.makeText.");
        description.setTextColor(Color.DKGRAY);
        description.setTextSize(12);
        row.addView(description);
        android.widget.Switch toggle = new android.widget.Switch(context);
        toggle.setText("Enabled");
        toggle.setChecked(hideTlError);
        toggle.setOnCheckedChangeListener((button, checked) -> hideTlError = checked);
        row.addView(toggle);
        page.addView(row);
    }

    private static void addUnavailable(LinearLayout page, String titleText, String detailText) {
        LinearLayout row = new LinearLayout(page.getContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(page.getContext(), 9), 0, dp(page.getContext(), 9));
        TextView title = new TextView(page.getContext());
        title.setText(titleText + "  ·  NOT IMPLEMENTED");
        title.setTextColor(Color.GRAY);
        title.setTextSize(15);
        row.addView(title);
        TextView detail = new TextView(page.getContext());
        detail.setText(detailText);
        detail.setTextColor(Color.DKGRAY);
        detail.setTextSize(12);
        row.addView(detail);
        page.addView(row);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
