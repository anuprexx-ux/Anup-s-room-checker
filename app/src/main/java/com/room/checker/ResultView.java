package com.room.checker;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.List;

class ResultView extends View {
    List<Rect> boxes; List<Integer> cls; List<String> labels; String summary;
    Paint fill = new Paint(), line = new Paint(), txt = new Paint(), bar = new Paint();
    ResultView(Context c, List<Rect> b, List<Integer> k, List<String> l, String s) {
        super(c); boxes = b; cls = k; labels = l; summary = s;
        line.setStyle(Paint.Style.STROKE); line.setStrokeWidth(5);
        txt.setTextSize(34); txt.setFakeBoldText(true); bar.setColor(0xDD000000);
    }
    int col(int k) { return k == 0 ? 0xFF00C853 : (k == 1 ? 0xFFFFC107 : 0xFFFF1744); }
    protected void onDraw(Canvas c) {
        for (int i = 0; i < boxes.size(); i++) {
            Rect r = boxes.get(i); int k = cls.get(i);
            fill.setColor(col(k)); fill.setAlpha(70); c.drawRect(r, fill);
            line.setColor(col(k)); c.drawRect(r, line);
            String s = labels.get(i);
            if (s.length() > 0) {
                float y = Math.max(r.top - 8, 150);
                float w = txt.measureText(s);
                bar.setColor(col(k)); c.drawRect(r.left, y - 34, r.left + w + 8, y + 8, bar);
                txt.setColor(k == 1 ? 0xFF000000 : 0xFFFFFFFF); c.drawText(s, r.left + 4, y, txt);
            }
        }
        bar.setColor(0xDD000000); c.drawRect(150, 0, 150 + txt.measureText(summary) + 20, 60, bar);
        txt.setColor(0xFFFFFFFF); c.drawText(summary, 160, 42, txt);
    }
}
