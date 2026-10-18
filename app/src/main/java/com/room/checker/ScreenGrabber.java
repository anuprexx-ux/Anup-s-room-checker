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

    interface Callback {
        void onBitmap(Bitmap bmp, int w, int h);

        void onFail(String msg);
    }

    static void grab(WindowManager wm, MediaProjection projection,
                     Handler handler, Callback cb) {
        try {
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getRealMetrics(dm);
            final int w = dm.widthPixels;
            final int h = dm.heightPixels;
            final ImageReader reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2);
            final VirtualDisplay vd = projection.createVirtualDisplay("rc", w, h, dm.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader.getSurface(), null, handler);
            final boolean[] done = {false};
            reader.setOnImageAvailableListener(r -> {
                if (done[0]) return;
                Image img = r.acquireLatestImage();
                if (img == null) return;
                done[0] = true;
                Bitmap bmp = null;
                try {
                    bmp = toBitmap(img, w, h);
                } catch (Exception ignored) {
                }
                img.close();
                vd.release();
                r.close();
                if (bmp == null) {
                    cb.onFail("Screen capture fail");
                } else {
                    cb.onBitmap(bmp, w, h);
                }
            }, handler);
            handler.postDelayed(() -> {
                if (done[0]) return;
                done[0] = true;
                vd.release();
                reader.close();
                cb.onFail("Screen capture nahi hua, dobara try karo");
            }, 4000);
        } catch (Exception ex) {
            cb.onFail("Capture error");
        }
    }

    private static Bitmap toBitmap(Image img, int w, int h) {
        Image.Plane p = img.getPlanes()[0];
        ByteBuffer buf = p.getBuffer();
        int pixelStride = p.getPixelStride();
        int rowStride = p.getRowStride();
        int rowPadding = rowStride - pixelStride * w;
        Bitmap b = Bitmap.createBitmap(w + rowPadding / pixelStride, h, Bitmap.Config.ARGB_8888);
        b.copyPixelsFromBuffer(buf);
        return Bitmap.createBitmap(b, 0, 0, w, h);
    }
}
