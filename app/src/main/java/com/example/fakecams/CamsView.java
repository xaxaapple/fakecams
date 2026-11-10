package com.example.fakecams;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.View;

import java.util.List;

/**
 * Рисует 4 фейковые фронталки (по 2 с каждой стороны) на одной линии с настоящей.
 * Размер и позиция настоящей берутся из DisplayCutout (cutoutPath -> точные границы),
 * поэтому всё совпадает пиксель в пиксель.
 */
public class CamsView extends View {

    public static final String PREFS = "fakecams";
    public static final String KEY_GAP_PERCENT = "gap_percent";   // зазор в % от диаметра
    public static final int DEFAULT_GAP_PERCENT = 22;

    // Запасные значения для S25 (1080x2340), если система не отдаст cutout
    private static final float FALLBACK_D_PX = 55f;
    private static final float FALLBACK_CY_PX = 56f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Bitmap bmp;

    private float cx, cy, d;

    public CamsView(Context c) {
        super(c);
        bmp = BitmapFactory.decodeResource(c.getResources(), R.drawable.front_camera);
        reloadCutout();
    }

    /** Возвращает высоту, которую должно занять окно оверлея (низ камеры + запас). */
    public int neededHeight() {
        return (int) Math.ceil(cy + d / 2f + 4);
    }

    public void reloadCutout() {
        DisplayManager dm = (DisplayManager) getContext().getSystemService(Context.DISPLAY_SERVICE);
        Display display = dm.getDisplay(Display.DEFAULT_DISPLAY);
        DisplayCutout cutout = display != null ? display.getCutout() : null;

        RectF r = null;
        if (cutout != null) {
            Path p = cutout.getCutoutPath();           // API 31+
            if (p != null && !p.isEmpty()) {
                r = new RectF();
                p.computeBounds(r, true);
            } else {
                List<Rect> rects = cutout.getBoundingRects();
                if (!rects.isEmpty()) r = new RectF(rects.get(0));
            }
        }
        if (r != null && r.width() > 1) {
            cx = r.centerX();
            cy = r.centerY();
            d = Math.min(r.width(), r.height());       // круглая дырка -> диаметр
            if (d < 1) d = Math.max(r.width(), r.height());
        } else {
            int w = getResources().getDisplayMetrics().widthPixels;
            d = FALLBACK_D_PX * w / 1080f;
            cx = w / 2f;
            cy = FALLBACK_CY_PX * w / 1080f;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        SharedPreferences sp = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        float gap = d * sp.getInt(KEY_GAP_PERCENT, DEFAULT_GAP_PERCENT) / 100f;
        float step = d + gap;                          // одинаковый шаг => одинаковые отступы везде

        // Окно оверлея начинается в (0,0) экрана, поэтому координаты экрана = координатам канваса
        int[] loc = new int[2];
        getLocationOnScreen(loc);
        float ox = -loc[0], oy = -loc[1];

        for (int k : new int[]{-2, -1, 1, 2}) {
            float left = cx + k * step - d / 2f + ox;
            float top = cy - d / 2f + oy;
            canvas.drawBitmap(bmp, null, new RectF(left, top, left + d, top + d), paint);
        }
    }
}
