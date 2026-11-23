package com.abolfazl.telexvip;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(16, 13, 24));
        getWindow().setNavigationBarColor(Color.rgb(16, 13, 24));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 56, 48, 40);
        root.setBackgroundColor(Color.rgb(16, 13, 24));

        TextView title = new TextView(this);
        title.setText("Abolfazl TeleX VIP");
        title.setTextSize(26);
        title.setTextColor(Color.WHITE);
        root.addView(title);

        TextView status = new TextView(this);
        status.setText("\nTELEVIP  /  v0.1.0\n\nCore bootstrap\nTarget: Telegram 13.3.1\nFramework: libxposed API 102\nPackage: org.telegram.messenger\n\nThis is a diagnostic starter. Telegram features are not active yet.");
        status.setTextSize(15);
        status.setTextColor(Color.rgb(190, 180, 210));
        root.addView(status);
        setContentView(root);
    }
}
