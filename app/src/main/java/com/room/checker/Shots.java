package com.room.checker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.*;

class Shots {
    static File dir(Context c) {
        File d = new File(c.getFilesDir(), "reg");
        d.mkdirs();
        return d;
    }

    static void save(Context c, List<Uri> uris) {
        File d = dir(c);
        File[] old = d.listFiles();
        if (old != null) for (File f : old) f.delete();
        int i = 0;
        for (Uri u : uris) {
            try {
                InputStream in = c.getContentResolver().openInputStream(u);
                Bitmap b = BitmapFactory.decodeStream(in);
                in.close();
                if (b == null) continue;
                FileOutputStream o = new FileOutputStream(new File(d, String.format("p%02d.jpg", i++)));
                b.compress(Bitmap.CompressFormat.JPEG, 90, o);
                o.close();
            } catch (Exception e) { }
        }
    }

    static List<File> list(Context c) {
        File[] f = dir(c).listFiles();
        List<File> l = new ArrayList<>();
        if (f != null) { Arrays.sort(f); l.addAll(Arrays.asList(f)); }
        return l;
    }
}
