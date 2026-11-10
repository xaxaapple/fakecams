package com.example.fakecams;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity {

    private boolean askedPermission = false;
    private TextView gapLabel;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);

        SharedPreferences sp = getSharedPreferences(CamsView.PREFS, MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad * 3, pad, pad);

        TextView title = new TextView(this);
        title.setText("Fake Cams");
        title.setTextSize(26);
        root.addView(title);

        gapLabel = new TextView(this);
        gapLabel.setPadding(0, pad, 0, 0);
        root.addView(gapLabel);

        SeekBar gap = new SeekBar(this);
        gap.setMax(60);
        gap.setProgress(sp.getInt(CamsView.KEY_GAP_PERCENT, CamsView.DEFAULT_GAP_PERCENT));
        updateLabel(gap.getProgress());
        gap.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                sp.edit().putInt(CamsView.KEY_GAP_PERCENT, p).apply();
                updateLabel(p);
                startService(new Intent(MainActivity.this, OverlayService.class)
                        .setAction(OverlayService.ACTION_REFRESH));
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        root.addView(gap);

        Button stop = new Button(this);
        stop.setText("Выключить");
        stop.setOnClickListener(v -> {
            sp.edit().putBoolean("enabled", false).apply();
            stopService(new Intent(this, OverlayService.class));
        });
        root.addView(stop);

        Button start = new Button(this);
        start.setText("Включить");
        start.setOnClickListener(v -> startOverlay());
        root.addView(start);

        setContentView(root);
    }

    private void updateLabel(int p) {
        gapLabel.setText("Зазор между камерами: " + p + "% диаметра");
    }

    @Override
    protected void onResume() {
        super.onResume();
        startOverlay();   // при запуске: выдаём разрешение (если нет) и сразу показываем
    }

    private void startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            if (!askedPermission) {
                askedPermission = true;
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            }
            return;
        }
        getSharedPreferences(CamsView.PREFS, Context.MODE_PRIVATE).edit().putBoolean("enabled", true).apply();
        startForegroundService(new Intent(this, OverlayService.class));
    }
}
