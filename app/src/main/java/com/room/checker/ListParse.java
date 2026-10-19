package com.room.checker;

import android.graphics.Rect;
import com.google.mlkit.vision.text.Text;
import java.util.*;
import java.util.regex.*;

class ListParse {
    static final Pattern NUM_ONLY = Pattern.compile("^(\\d{1,2})\\s*[.)\\-:]?$");
    static final Pattern NUM_PRE = Pattern.compile("^(\\d{1,2})\\s*[.)\\-:]?\\s*(.+)$");

    static boolean skip(String t) {
        String l = t.toLowerCase(Locale.ROOT);
        return l.contains("match details") || l.contains("entry fee") || l.startsWith("join")
                || l.matches(".*\\d{1,2}:\\d{2}.*") || l.matches("^[\\u20b9\\s\\d.,]+$");
    }

    static void collect(Text t, int w, int h, TreeMap<Integer, String> byNum, List<String> loose) {
        List<Rect> nb = new ArrayList<>(); List<Integer> nv = new ArrayList<>();
        List<Rect> tb = new ArrayList<>(); List<String> tv = new ArrayList<>();
        for (Text.TextBlock b : t.getTextBlocks())
            for (Text.Line ln : b.getLines()) {
                Rect r = ln.getBoundingBox();
                if (r == null) continue;
                String s = ln.getText().trim();
                if (r.top < 0.095 * h || r.bottom > 0.87 * h || s.isEmpty() || skip(s)) continue;
                Matcher m = NUM_ONLY.matcher(s);
                if (m.matches() && r.left < 0.12 * w) { nb.add(r); nv.add(Integer.parseInt(m.group(1))); continue; }
                tb.add(r); tv.add(s);
            }
        for (int i = 0; i < tv.size(); i++) {
            String s = tv.get(i); Rect r = tb.get(i);
            int num = -1;
            Matcher m = NUM_PRE.matcher(s);
            if (r.left < 0.12 * w && m.matches()) { num = Integer.parseInt(m.group(1)); s = m.group(2).trim(); }
            else {
                int bd = Integer.MAX_VALUE;
                for (int k = 0; k < nb.size(); k++) {
                    Rect q = nb.get(k);
                    int d = Math.abs(q.centerY() - r.centerY());
                    if (d < 0.035 * h && q.left < r.left && d < bd) { bd = d; num = nv.get(k); }
                }
            }
            if (s.isEmpty()) continue;
            boolean added = false;
            if (num >= 1 && num <= 99) {
                String old = byNum.get(num);
                if (old == null) { byNum.put(num, s); added = true; }
                else if (NameMatch.lev(NameMatch.norm(old), NameMatch.norm(s)) >= 0.6) {
                    if (s.length() > old.length()) byNum.put(num, s);
                    added = true;
                }
            }
            if (!added) {
                boolean dup = false;
                for (String o : loose) if (NameMatch.lev(NameMatch.norm(o), NameMatch.norm(s)) >= 0.78) dup = true;
                if (!dup) loose.add(s);
            }
        }
    }

    static void finish(TreeMap<Integer, String> byNum, List<String> loose, ListImport.Done done) {
        StringBuilder sb = new StringBuilder();
        for (String s : byNum.values()) sb.append(s).append('\n');
        for (String s : loose) {
            boolean dup = false;
            for (String o : byNum.values()) if (NameMatch.lev(NameMatch.norm(o), NameMatch.norm(s)) >= 0.78) dup = true;
            if (!dup) sb.append(s).append('\n');
        }
        StringBuilder miss = new StringBuilder();
        if (!byNum.isEmpty())
            for (int n = 1; n <= byNum.lastKey(); n++) if (!byNum.containsKey(n)) miss.append(n).append(' ');
        done.onList(sb.toString(), miss.toString().trim());
    }
}
