package com.room.checker;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

class ResultView extends View {
    List<Rect> boxes = new ArrayList<>(); List<Integer> cls = new ArrayList<>(); String summary = "";
    Paint txt = new Paint(), bar = new Paint();
    ResultView(Context c) {
        super(c);
        txt.setTextSize(28); txt.setFakeBoldText(true);
    }
    void set(List<Rect> b, List<Integer> k, String s) {
        boxes = b; cls = k; summary = s; invalidate();
    }
    int col(int k) { return k == 0 ? 0xFF00E676 : (k == 1 ? 0xFFFFC107 : 0xFFFF1744); }
    protected void onDraw(Canvas c) {
        int[] loc = new int[2]; getLocationOnScreen(loc); c.translate(-loc[0], -loc[1]);
        for (int i = 0; i < boxes.size(); i++) {
            Rect r = boxes.get(i);
            bar.setColor(col(cls.get(i)));
            c.drawRect(r.left - 16, r.top - 2, r.left - 4, r.bottom + 2, bar);
        }
        bar.setColor(0xDD000000);
        c.drawRect(220, 0, 220 + txt.measureText(summary) + 20, 50, bar);
        txt.setColor(0xFFFFFFFF); c.drawText(summary, 230, 36, txt);
    }
}
