package com.room.checker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.InputStream;
import java.util.*;

class ListImport {
    interface Done { void onList(String names, String report); }

    static void run(final Context ctx, final List<Uri> uris, final Done done) {
        final TextRecognizer rec = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        final TreeMap<Integer, String> byNum = new TreeMap<>();
        final List<String> loose = new ArrayList<>();
        step(ctx, rec, uris, 0, byNum, loose, done);
    }

    static void step(final Context ctx, final TextRecognizer rec, final List<Uri> uris, final int i,
                     final TreeMap<Integer, String> byNum, final List<String> loose, final Done done) {
        if (i >= uris.size()) { ListParse.finish(byNum, loose, done); return; }
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
                ListParse.collect(t, w, h, byNum, loose);
                step(ctx, rec, uris, i + 1, byNum, loose, done);
            }
        }).addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
            public void onFailure(Exception e) { step(ctx, rec, uris, i + 1, byNum, loose, done); }
        });
    }
}
