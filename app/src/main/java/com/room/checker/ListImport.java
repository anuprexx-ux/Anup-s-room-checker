package com.room.checker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.InputStream;
import java.util.*;
import java.util.regex.*;

class ListImport {
    interface Done { void onList(String names, String report); }

    static final Pattern NUM_ONLY = Pattern.compile("^(\\d{1,2})\\s*[.)\\-:]?$");
    static final Pattern NUM_PRE = Pattern.compile("^(\\d{1,2})\\s*[.)\\-:]?\\s*(.+)$");

    static void run(final Context ctx, final List<Uri> uris, final Done done) {
        final TextRecognizer rec = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        final TreeMap<Integer, String> byNum = new TreeMap<>();
        final List<String> loose = new ArrayList<>();
        step(ctx, rec, uris, 0, byNum, loose, done);
    }

    static void step(final Context ctx, final TextRecognizer rec, final List<Uri> uris, final int i,
                     final TreeMap<Integer, String> byNum, final List<String> loose, final Done done) {
        if (i >= uris.size()) { finish(byNum, loose, done); return; }
        Bitmap bmp = null;
        try {
            InputStream in = ctx.getContentResolver().openInputStream(uris.get(i));
            bmp = BitmapFactory.decodeStream(in);
            in.close();
        } catch (Exception e) { }
        if (bmp == null) { step(ctx, rec, uris, i + 1, byNum, loose, done); return; }
        final int h = bmp.getHeight(), w = bmp.getWidth();
        rec.process(InputImage.fromBitmap(bmp, 0)).addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Text>() {
            public void onSuccess(Text t) {
                collect(t, w, h, byNum, loose);
                step(ctx, rec, uris, i + 1, byNum, loose, done);
            }
        }).addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
            public void onFailure(Exception e) { step(ctx, rec, uris, i + 1, byNum, loose, done); }
        });
    }

    static boolean skip(String t) {
        String l = t.toLowerCase(Locale.ROOT);
        return l.contains("match details") || l.contains("entry fee") || l.startsWith("join")
                || l.matches(".*\\d{1,2}:\\d{2}.*") || l.matches("^[\\u20b9\\s\\d.,]+$");
    }

    static void collect(Text t, int w, int h, TreeMap
