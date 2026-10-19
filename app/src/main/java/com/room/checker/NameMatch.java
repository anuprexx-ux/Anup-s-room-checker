package com.room.checker;

import android.graphics.Rect;
import com.google.mlkit.vision.text.Text;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

class NameMatch {
    static class Result {
        List<Rect> boxes = new ArrayList<>();
        List<Integer> cls = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        Set<Integer> hit = new HashSet<>();
        int red, yel, grn, room = -1;
    }

    static String norm(String s) {
        if (s == null) return "";
        s = Normalizer.normalize(s, Normalizer.Form.NFKD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '0': case '\u043e': case '\u03bf': c = 'o'; break;
                case '1': case 'i': case '|': case '!': case '\u0456': c = 'l'; break;
                case '5': c = 's'; break;
                case '8': c = 'b'; break;
                case '2': c = 'z'; break;
                case '6': case '9': c = 'g'; break;
                case '4': case '\u0430': c = 'a'; break;
                case '3': case '\u0435': c = 'e'; break;
                case '7': c = 't'; break;
                case '\u0440': c = 'p'; break;
                case '\u0441': c = 'c'; break;
                case '\u0445': c = 'x'; break;
                case '\u0443': c = 'y'; break;
                default: break;
            }
            if (c >= 'a' && c <= 'z') b.append(c);
        }
        return b.toString();
    }

    static double sim(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        if (a.equals(b)) return 1;
        int mn = Math.min(a.length(), b.length()), mx = Math.max(a.length(), b.length());
        if (mn >= 4 && (a.contains(b) || b.contains(a))) return mn * 2 >= mx ? 1.0 : 0.7;
        return lev(a, b);
    }

    static double lev(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        int mx = Math.max(a.length(), b.length());
        int[] p = new int[b.length() + 1], q = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) p[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            q[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int c = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                q[j] = Math.min(Math.min(q[j - 1] + 1, p[j] + 1), p[j - 1] + c);
            }
            int[] t = p; p = q; q = t;
        }
        return 1.0 - (double) p[b.length()] / mx;
    }

    static List<List<String>> parse(String listText) {
        List<List<String>> out = new ArrayList<>();
        for (String line : listText.split("\n")) {
            List<String> al = new ArrayList<>();
            for (String part : line.split("/")) {
                String n = norm(part);
                if (n.length() >= 2) al.add(n);
            }
            if (!al.isEmpty()) out.add(al);
        }
        return out;
    }

    static boolean junk(String n) {
        return n.contains("spectator") || n.contains("invitefriend") || n.contains("waiting")
                || n.contains("notinteam") || n.contains("taptoreply") || n.contains("notenough")
                || n.contains("minimumlevel") || n.contains("changedto");
    }

        static final Pattern HDR = Pattern.compile("(\\d{1,2})\\s*/\\s*(\\d{2})(?!\\d)");
    static final Pattern CNT = Pattern.compile("^[(\\[]?\\s*(\\d{1,2})\\s*/\\s*(\\d{1,2})\\s*[)\\]]?$");

    static Result build(Text text, List<List<String>> reg, int rx, int ry, float scale,
                        int edgeTop, int edgeBottom) {
        Result r = new Result();
        List<String> names = new ArrayList<>();
        List<Rect> boxes = new ArrayList<>();
        for (Text.TextBlock tb : text.getTextBlocks()) {
            for (Text.Line ln : tb.getLines()) {
                Rect b0 = ln.getBoundingBox();
                if (b0 == null) continue;
                Rect b = new Rect(rx + (int) (b0.left / scale), ry + (int) (b0.top / scale),
                        rx + (int) (b0.right / scale), ry + (int) (b0.bottom / scale));
                String t = ln.getText().trim();
                                if (b.bottom < edgeTop) {
                    Matcher hm = HDR.matcher(t);
                    if (hm.find() && r.room < 0) r.room = Integer.parseInt(hm.group(1));
                    continue;
                }
                if (t.matches("[0-9]{1,2}")) continue;
                Matcher cm = CNT.matcher(t);
                if (cm.matches()) {
                    if (Integer.parseInt(cm.group(2)) >= 10 && r.room < 0) r.room = Integer.parseInt(cm.group(1));
                    continue;
                }
                if (junk(norm(t))) continue;
                names.add(t);
                boxes.add(b);
            }
        }
        boolean again = true;
        while (again) {
            again = false;
            for (int i = 0; i < names.size() && !again; i++)
                for (int j = i + 1; j < names.size() && !again; j++) {
                    Rect a = boxes.get(i), c = boxes.get(j);
                    int hh = Math.max(a.height(), c.height());
                    if (Math.abs(a.centerY() - c.centerY()) > hh * 0.6) continue;
                    Rect l = a.left <= c.left ? a : c, rr = a.left <= c.left ? c : a;
                    int gap = rr.left - l.right;
                    if (gap < -hh || gap > hh * 1.2) continue;
                    String tl = a.left <= c.left ? names.get(i) : names.get(j);
                    String tr = a.left <= c.left ? names.get(j) : names.get(i);
                    Rect u = new Rect(Math.min(a.left, c.left), Math.min(a.top, c.top), Math.max(a.right, c.right), Math.max(a.bottom, c.bottom));
                    names.set(i, tl + " " + tr); boxes.set(i, u);
                    names.remove(j); boxes.remove(j);
                    again = true;
                }
        }
        int n = names.size();
        double[] best = new double[n];
        int[] bi = new int[n];
        for (int i = 0; i < n; i++) {
            String nn = norm(names.get(i));
            best[i] = 0; bi[i] = -1;
            for (int k = 0; k < reg.size(); k++)
                for (String al : reg.get(k)) {
                    double s = sim(nn, al);
                    if (s > best[i]) { best[i] = s; bi[i] = k; }
                }
        }
        int[] use = new int[reg.size()];
        for (int i = 0; i < n; i++) if (bi[i] >= 0 && best[i] >= 0.5) use[bi[i]]++;
        for (int i = 0; i < n; i++) {
            String raw = names.get(i), nn = norm(raw);
            Rect b = boxes.get(i);
            boolean edge = b.top < edgeTop || b.bottom > edgeBottom;
            int c;
            if (nn.length() < 3) c = 1;
            else if (best[i] >= 0.78) c = 0;
            else if (best[i] >= 0.5) c = 1;
            else c = edge ? 1 : 2;
            if (c == 0 && bi[i] >= 0 && use[bi[i]] > 1) c = 1;
            if (c <= 1 && bi[i] >= 0 && best[i] >= 0.5) r.hit.add(bi[i]);
            r.boxes.add(b);
            r.cls.add(c);
            r.labels.add(c == 0 ? "" : (c == 1 ? "? " : "X ") + raw);
            if (c == 0) r.grn++; else if (c == 1) r.yel++; else r.red++;
        }
        return r;
    }
}
