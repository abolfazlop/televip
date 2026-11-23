package com.abolfazl.telexvip;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(15, 12, 23);
    private static final int CARD = Color.rgb(30, 24, 43);
    private static final int PURPLE = Color.rgb(167, 139, 250);
    private static final int MUTED = Color.rgb(185, 176, 202);
    private SharedPreferences prefs;
    private LinearLayout content;

    private static final String[][] FEATURES = {
        {"PRIVACY", "Ghost Mode", "HideOnline", "HideTyping", "HideSeen", "HidePhone", "HideStoryView"},
        {"MESSAGES", "Message Details", "Edits History", "Export Deleted Messages", "Hide Pinned Messages"},
        {"MEDIA", "Voice Message Saving", "Download Speed", "Download To Forward"},
        {"APPEARANCE", "Liquid Glass", "Persian Calendar", "Hijri Calendar"},
        {"OTHER", "Block Ads", "Settings Backup"}
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("telexvip_settings", MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(26), dp(18), dp(30));
        content.setBackgroundColor(BG);
        scroll.addView(content);

        TextView eyebrow = text("TELEVIP  /  CONTROL CENTER", 12, PURPLE, true);
        content.addView(eyebrow);
        TextView title = text("Abolfazl TeleX VIP", 27, Color.WHITE, true);
        LinearLayout.LayoutParams tlp = params();
        tlp.topMargin = dp(8);
        content.addView(title, tlp);
        TextView subtitle = text("Telegram enhancement modules", 14, MUTED, false);
        content.addView(subtitle);

        LinearLayout status = card();
        LinearLayout.LayoutParams slp = params();
        slp.topMargin = dp(20);
        content.addView(status, slp);
        addText(status, "MODULE STATUS", 11, PURPLE, true);
        addText(status, "App settings are stored on this device.", 14, Color.WHITE, true);
        addText(status, "Target: Telegram 13.3.1  •  API 102", 12, MUTED, false);
        addText(status, "Important: switches are configuration placeholders until their Telegram hooks are implemented and verified.", 12, MUTED, false);

        for (String[] group : FEATURES) {
            addSection(group[0]);
            LinearLayout box = card();
            LinearLayout.LayoutParams lp = params();
            lp.topMargin = dp(8);
            content.addView(box, lp);
            for (int i = 1; i < group.length; i++) addFeature(box, group[i]);
        }

        TextView footer = text("TELEX VIP • Independent implementation • v0.2.0", 11, MUTED, false);
        LinearLayout.LayoutParams flp = params();
        flp.topMargin = dp(24);
        footer.setGravity(Gravity.CENTER);
        content.addView(footer, flp);
        setContentView(scroll);
    }

    private void addSection(String name) {
        TextView h = text(name, 11, PURPLE, true);
        LinearLayout.LayoutParams lp = params();
        lp.topMargin = dp(22);
        content.addView(h, lp);
    }

    private void addFeature(LinearLayout parent, String name) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(4), dp(10), dp(2), dp(10));
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        TextView label = text(name, 14, Color.WHITE, true);
        info.addView(label);
        TextView state = text("Hook integration pending", 11, MUTED, false);
        info.addView(state);
        row.addView(info, new LinearLayout.LayoutParams(0, -2, 1));

        Switch toggle = new Switch(this);
        toggle.setButtonTintList(null);
        toggle.setChecked(prefs.getBoolean(key(name), false));
        toggle.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(key(name), checked).apply();
            Toast.makeText(this, checked
                ? name + ": saved (hook not active yet)"
                : name + ": disabled", Toast.LENGTH_SHORT).show();
        });
        row.addView(toggle);
        parent.addView(row);
        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(55, 47, 68));
        parent.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private String key(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(15), dp(12), dp(15), dp(12));
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(CARD);
        shape.setCornerRadius(dp(17));
        box.setBackground(shape);
        return box;
    }

    private void addText(LinearLayout parent, String value, int size, int color, boolean bold) {
        TextView v = text(value, size, color, bold);
        LinearLayout.LayoutParams lp = params();
        lp.topMargin = dp(5);
        parent.addView(v, lp);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private LinearLayout.LayoutParams params() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
