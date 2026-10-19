package com.room.checker;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.*;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.*;

public class CheckerService extends Service {
    WindowManager wm; MediaProjection mp; TextView fab; ResultView rv;
    Handler hd = new Handler(Looper.getMainLooper());
    TextRecognizer rec; List<List<String>> reg = new ArrayList<>();
    Set<Integer> seen = new HashSet<>(); boolean busy = false;
    WindowManager.LayoutParams fp;

    public IBinder onBind(Intent i) { return null; }

    public int onStartCommand(Intent in, int f, int id) {
        String ch = "rc";
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(ch, "Room Checker", NotificationManager.IMPORTANCE_LOW));
        startForeground(1, new Notification.Builder(this, ch).setContentTitle("Room Checker chal raha hai")
                .setSmallIcon(android.R.drawable.ic_menu_search).build());
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        MediaProjectionManager m = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mp = m.getMediaProjection(in.getIntExtra("code", 0), (Intent) in.getParcelableExtra("data"));
        mp.registerCallback(new MediaProjection.Callback() { }, hd);
        rec = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        String lt = getSharedPreferences("rc", 0).getString("list", "");
        reg = NameMatch.parse(lt);
        addButton();
        return START_NOT_STICKY;
    }

    void addButton() {
        fab = new TextView(this);
        fab.setText("CHECK"); fab.setTextColor(0xFFFFFFFF); fab.setTextSize(11); fab.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable(); g.setShape(GradientDrawable.OVAL); g.setColor(0xFF1565C0);
        fab.setBackground(g);
        fp = new WindowManager.LayoutParams(130, 130, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        fp.gravity = Gravity.TOP | Gravity.LEFT; fp.x = 10; fp.y = 10;
        fab.setOnTouchListener(new View.OnTouchListener() {
            float dx, dy; int sx, sy; boolean moved; long down;
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN: dx = e.getRawX(); dy = e.getRawY(); sx = fp.x; sy = fp.y; moved = false; down = System.currentTimeMillis(); return true;
                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(e.getRawX() - dx) > 15 || Math.abs(e.getRawY() - dy) > 15) moved = true;
                        if (moved) { fp.x = sx + (int) (e.getRawX() - dx); fp.y = sy + (int) (e.getRawY() - dy); wm.updateViewLayout(fab, fp); }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) {
                            if (System.currentTimeMillis() - down > 900) { seen.clear(); Toast.makeText(CheckerService.this, "Tally reset", Toast.LENGTH_SHORT).show(); }
                            else onTap();
                        }
                        return true;
                }
                return false;
            }
        });
        wm.addView(fab, fp);
    }

    void onTap() {
        if (busy) return;
        if (rv != null) { clearResult(); return; }
        busy = true;
        fab.setVisibility(View.INVISIBLE);
        hd.postDelayed(new Runnable() { public void run() {
            ScreenGrabber.grab(wm, mp, hd, new ScreenGrabber.Callback() {
                public void onBitmap(Bitmap b, int w, int h) { analyze(b, w, h); }
                public void onFail(String m) { finish(m); }
            });
        } }, 250);
    }

    void analyze(Bitmap b, final int w, final int h) {
        final int rx = (int) (0.45 * w), ry = (int) (0.19 * h), rw = (int) (0.52 * w), rh = (int) (0.56 * h);
        final float sc = 2f;
        Bitmap crop = Bitmap.createBitmap(b, rx, ry, Math.min(rw, w - rx), Math.min(rh, h - ry));
        Bitmap big = Bitmap.createScaledBitmap(crop, (int) (crop.getWidth() * sc), (int) (crop.getHeight() * sc), true);
        rec.process(InputImage.fromBitmap(big, 0)).addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Text>() {
            public void onSuccess(Text t) {
                NameMatch.Result r = NameMatch.build(t, reg, rx, ry, sc, (int) (0.205 * h), (int) (0.71 * h));
                seen.addAll(r.hit);
                String sum = "Unreg:" + r.red + "  Check:" + r.yel + "  OK:" + r.grn + "  | Mile:" + seen.size() + "/" + reg.size();
                show(r, sum);
            }
        }).addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
            public void onFailure(Exception e) { finish("OCR fail"); }
        });
    }

    void show(NameMatch.Result r, String sum) {
        rv = new ResultView(this, r.boxes, r.cls, r.labels, sum);
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(-1, -1, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT);
        wm.addView(rv, p);
        wm.removeView(fab); wm.addView(fab, fp);
        fab.setText("CLEAR"); fab.setVisibility(View.VISIBLE); busy = false;
    }

    void clearResult() {
        if (rv != null) { try { wm.removeView(rv); } catch (Exception e) { } rv = null; }
        fab.setText("CHECK");
    }

    void finish(String m) {
        busy = false; fab.setVisibility(View.VISIBLE);
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }

    public void onDestroy() {
        clearResult();
        try { wm.removeView(fab); } catch (Exception e) { }
        if (mp != null) mp.stop();
        super.onDestroy();
    }
}
