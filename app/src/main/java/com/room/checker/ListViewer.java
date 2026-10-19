package com.room.checker;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.io.File;
import java.util.List;

class ListViewer {
    Context ctx; WindowManager wm; TextView btn; ScrollView sv;
    WindowManager.LayoutParams bp;

    ListViewer(Context c, WindowManager w) { ctx = c; wm = w; }

    void start() {
        btn = new TextView(ctx);
        btn.setText("LIST"); btn.setTextColor(0xFFFFFFFF); btn.setTextSize(11); btn.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable(); g.setShape(GradientDrawable.OVAL); g.setColor(0xFF2E7D32);
        btn.setBackground(g);
        bp = new WindowManager.LayoutParams(130, 130, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        bp.gravity = Gravity.TOP | Gravity.LEFT; bp.x = 10; bp.y = 160;
        btn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { toggle(); } });
        wm.addView(btn, bp);
    }

    void toggle() {
        if (sv != null) { close(); return; }
        List<File> files = Shots.list(ctx);
        if (files.isEmpty()) { Toast.makeText(ctx, "Pehle app mein list ke screenshots import karo", Toast.LENGTH_LONG).show(); return; }
        LinearLayout ll = new LinearLayout(ctx);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER_HORIZONTAL);
        for (File f : files) {
            ImageView iv = new ImageView(ctx);
            iv.setImageBitmap(BitmapFactory.decodeFile(f.getAbsolutePath()));
            iv.setAdjustViewBounds(true);
            ll.addView(iv, new LinearLayout.LayoutParams(1100, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        sv = new ScrollView(ctx);
        sv.setBackgroundColor(0xF2000000);
        sv.addView(ll, new ViewGroup.LayoutParams(-1, -2));
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(-1, -1, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        wm.addView(sv, p);
        wm.removeView(btn); wm.addView(btn, bp);
        btn.setText("CLOSE");
    }

    void close() {
        if (sv != null) { try { wm.removeView(sv); } catch (Exception e) { } sv = null; }
        if (btn != null) btn.setText("LIST");
    }

    void stop() {
        close();
        try { wm.removeView(btn); } catch (Exception e) { }
    }
}
