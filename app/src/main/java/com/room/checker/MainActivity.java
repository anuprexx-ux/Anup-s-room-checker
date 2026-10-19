package com.room.checker;

import android.app.Activity;
import android.content.*;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.*;
import android.view.View;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    EditText et; TextView st;

    Button btn(LinearLayout p, String t, View.OnClickListener l) {
        Button b = new Button(this); b.setText(t); b.setOnClickListener(l); p.addView(b); return b;
    }

    protected void onCreate(Bundle s) {
        super.onCreate(s);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(24, 24, 24, 24);
        ScrollView sv = new ScrollView(this); sv.addView(root); setContentView(sv);
        st = new TextView(this); st.setTextSize(16); root.addView(st);
        et = new EditText(this); et.setMinLines(6); et.setGravity(android.view.Gravity.TOP);
        et.setText(getSharedPreferences("rc", 0).getString("list", ""));
        root.addView(et);
        et.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable e) { count(); }
            public void beforeTextChanged(CharSequence a, int b, int c, int d) { }
            public void onTextChanged(CharSequence a, int b, int c, int d) { }
        });
        btn(root, "1. Screenshots se list banao", new View.OnClickListener() { public void onClick(View v) {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("image/*"); i.addCategory(Intent.CATEGORY_OPENABLE);
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); startActivityForResult(i, 1); } });
        btn(root, "2. List Save Karo", new View.OnClickListener() { public void onClick(View v) {
            getSharedPreferences("rc", 0).edit().putString("list", et.getText().toString()).commit();
            Toast.makeText(MainActivity.this, "Saved", Toast.LENGTH_SHORT).show(); } });
        btn(root, "3. Start Overlay", new View.OnClickListener() { public void onClick(View v) {
            getSharedPreferences("rc", 0).edit().putString("list", et.getText().toString()).commit();
            if (!Settings.canDrawOverlays(MainActivity.this)) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))); return; }
            MediaProjectionManager m = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            startActivityForResult(m.createScreenCaptureIntent(), 2); } });
        btn(root, "Stop", new View.OnClickListener() { public void onClick(View v) {
            stopService(new Intent(MainActivity.this, CheckerService.class)); } });
        btn(root, "List Khali Karo", new View.OnClickListener() { public void onClick(View v) { et.setText(""); } });
        count();
    }

    void count() {
        int n = NameMatch.parse(et.getText().toString()).size();
        st.setText("List mein players: " + n);
        st.setTextColor(n == 48 ? Color.rgb(0, 130, 0) : Color.rgb(200, 0, 0));
    }

    protected void onActivityResult(int rq, int rs, Intent d) {
        if (rs != RESULT_OK || d == null) return;
        if (rq == 2) {
            Intent i = new Intent(this, CheckerService.class); i.putExtra("code", rs); i.putExtra("data", d);
            startForegroundService(i); moveTaskToBack(true);
        } else if (rq == 1) {
            List<Uri> u = new ArrayList<>();
            if (d.getClipData() != null) for (int k = 0; k < d.getClipData().getItemCount(); k++) u.add(d.getClipData().getItemAt(k).getUri());
            else if (d.getData() != null) u.add(d.getData());
            Toast.makeText(this, "Padh raha hu...", Toast.LENGTH_SHORT).show();
            ListImport.run(this, u, new ListImport.Done() { public void onList(String names, String miss) {
                et.setText(names);
                if (miss.length() > 0) Toast.makeText(MainActivity.this, "Ye numbers miss: " + miss + " (aur ss do ya khud likho)", Toast.LENGTH_LONG).show();
            } });
        }
    }
}
