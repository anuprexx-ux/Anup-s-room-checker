package com.room.checker;

import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import java.nio.ByteBuffer;

class ScreenGrabber {
    interface Callback { void onBitmap(Bitmap b, int w, int h); void onFail(String m); }

    static void grab(WindowManager wm, MediaProjection mp, final Handler hd, final Callback cb) {
        final DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(dm);
        final ImageReader ir = ImageReader.newInstance(dm.widthPixels, dm.heightPixels, PixelFormat.RGBA_8888, 2);
        final VirtualDisplay vd = mp.createVirtualDisplay("rc", dm.widthPixels, dm.heightPixels, dm.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, ir.getSurface(), null, hd);
        final boolean[] done = {false};
        final Runnable timeout = new Runnable() { public void run() {
            if (done[0]) return; done[0] = true; vd.release(); ir.close(); cb.onFail("Screen capture timeout"); } };
        ir.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            public void onImageAvailable(ImageReader r) {
                if (done[0]) return;
                Image im = r.acquireLatestImage();
                if (im == null) return;
                done[0] = true; hd.removeCallbacks(timeout);
                Bitmap b = null;
                try {
                    Image.Plane p = im.getPlanes()[0];
                    ByteBuffer buf = p.getBuffer();
                    int rs = p.getRowStride(), px = p.getPixelStride();
                    Bitmap tmp = Bitmap.createBitmap(rs / px, dm.heightPixels, Bitmap.Config.ARGB_8888);
                    tmp.copyPixelsFromBuffer(buf);
                    b = Bitmap.createBitmap(tmp, 0, 0, dm.widthPixels, dm.heightPixels);
                } catch (Exception e) { }
                im.close(); vd.release(); ir.close();
                if (b == null) cb.onFail("Bitmap error"); else cb.onBitmap(b, dm.widthPixels, dm.heightPixels);
            }
        }, hd);
        hd.postDelayed(timeout, 4000);
    }
}
