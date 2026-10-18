package com.room.checker;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ListImport {

    interface Done {
        void onList(String names);
    }

    private static class Item {
        int top;
        String s;
    }

    private final TextRecognizer recognizer =
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    void run(Context ctx, List<Uri> uris, String old, Done done) {
        final int count = uris.size();
        final List<List<String>> parts = new ArrayList<>();
        for (int i = 0; i < count; i++) parts.add(new ArrayList<String>());
        final int[] left = {count};
        for (int i = 0; i < count; i++) {
            final int index = i;
            try {
                InputImage image = InputImage.fromFilePath(ctx, uris.get(i));
                final int h = image.getHeight();
                recognizer.process(image)
                        .addOnSuccessListener(text -> {
                            parts.set(index, names(text, h));
                            finish(old, parts, left, done);
                        })
                        .addOnFailureListener(e -> finish(old, parts, left, done));
            } catch (Exception ex) {
                finish(old, parts, left, done);
            }
        }
    }

    private void finish(String old, List<List<String>> parts, int[] left, Done done) {
        left[0]--;
        if (left[0] > 0) return;
        List<String> keys = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (String s : old.split("\n")) add(s.trim(), keys, sb);
        for (List<String> part : parts) {
            for (String s : part) add(s, keys, sb);
        }
        done.onList(sb.toString());
    }

    private void add(String s, List<String> keys, StringBuilder sb) {
        if (s.isEmpty()) return;
        String k = NameMatch.norm(s);
        String key = k.length() > 0 ? k : "raw:" + s;
        if (keys.contains(key)) return;
        keys.add(key);
        sb.append(s).append('\n');
    }

    private List<String> names(Text text, int h) {
        List<Item> items = new ArrayList<>();
        for (Text.TextBlock blk : text.getTextBlocks()) {
            for (Text.Line l : blk.getLines()) {
                Rect r = l.getBoundingBox();
                if (r == null) continue;
                if (r.top < h * 0.095 || r.bottom > h * 0.875) continue;
                String s = clean(l.getText());
                if (s == null) continue;
                Item it = new Item();
                it.top = r.top;
                it.s = s;
                items.add(it);
            }
        }
        Collections.sort(items, (x, y) -> Integer.compare(x.top, y.top));
        List<String> out = new ArrayList<>();
        for (Item it : items) out.add(it.s);
        return out;
    }

    private String clean(String raw) {
        String s = raw.trim();
        String low = s.toLowerCase();
        if (low.matches(".*\\d{1,2}:\\d{2}.*")) return null;
        if (low.contains("match details") || low.contains("entry fee")) return null;
        s = s.replaceFirst("^\\d{1,2}\\s*[.)\\-:]\\s*", "");
        s = s.replaceFirst("^\\d{1,2}\\s+(?=[^\\d\\s])", "");
        s = s.trim();
        if (s.matches("^\\d{1,2}$")) return null;
        int cnt = 0;
        for (char ch : s.toCharArray()) {
            if (Character.isLetterOrDigit(ch)) cnt++;
        }
        if (cnt < 2) return null;
        return s;
    }
}
