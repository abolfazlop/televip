package com.gamerxt.tgtools;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public class SettingsActivity extends Activity implements XposedServiceHelper.OnServiceListener {
 private SharedPreferences prefs; private TextView status; private final Switch[] switches=new Switch[Keys.ALL.length];
 @Override protected void onCreate(Bundle b){
  super.onCreate(b);int pad=(int)(16*getResources().getDisplayMetrics().density);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(pad,pad,pad,pad);
  TextView title=new TextView(this);title.setText("GamerXt Tools");title.setTextSize(22);root.addView(title);
  TextView by=new TextView(this);by.setText("Made by @U_GamerXt");root.addView(by);
  status=new TextView(this);status.setText("Waiting for LSPosed... enable module for Telegram");status.setPadding(0,pad/2,0,pad/2);root.addView(status);
  for(int i=0;i<Keys.ALL.length;i++){
   final String key=Keys.ALL[i][0];Switch s=new Switch(this);s.setText(Keys.ALL[i][1]+"\n"+Keys.ALL[i][2]);s.setGravity(Gravity.CENTER_VERTICAL);s.setEnabled(false);s.setPadding(0,pad/2,0,pad/2);
   s.setOnCheckedChangeListener((v,checked)->{if(prefs!=null&&v.isPressed())prefs.edit().putBoolean(key,checked).apply();});switches[i]=s;root.addView(s);
  }
  ScrollView sv=new ScrollView(this);sv.addView(root);setContentView(sv);XposedServiceHelper.registerListener(this);
 }
 @Override public void onServiceBind(XposedService service){
  prefs=service.getRemotePreferences(Keys.GROUP);runOnUiThread(()->{status.setText("Connected to "+service.getFrameworkName());for(int i=0;i<switches.length;i++){switches[i].setChecked(prefs.getBoolean(Keys.ALL[i][0],false));switches[i].setEnabled(true);}});
 }
 @Override public void onServiceDied(XposedService service){prefs=null;runOnUiThread(()->status.setText("LSPosed service disconnected."));}
}
