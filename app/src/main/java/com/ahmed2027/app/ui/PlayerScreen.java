package com.ahmed2027.app.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import com.ahmed2027.app.R;
import com.ahmed2027.app.core.ActivityLog;
import com.ahmed2027.app.learn.YouTubeId;

/** Official youtube-nocookie IFrame embed; navigation away from that host is refused. Logs watch time as learning minutes. */
public final class PlayerScreen extends ScreenBase {
    private final long openedAt = System.currentTimeMillis();
    private final WebView web;

    @SuppressLint("SetJavaScriptEnabled")
    public PlayerScreen(Activity a, Host host, String title, String videoId) {
        super(a, host, R.string.grow_learn);
        col.addView(text(title, 17, R.color.text_primary), wrap(8));
        col.addView(text(a.getString(R.string.player_note), 12, R.color.text_secondary), wrap(4));
        web = new WebView(a);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);          // required by the YouTube IFrame player
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMediaPlaybackRequiresUserGesture(true);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return !YouTubeId.isAllowedHost(r.getUrl().getHost());
            }
            @Override public void onPageStarted(WebView v, String url, Bitmap f) { }
        });
        web.loadDataWithBaseURL("https://www.youtube-nocookie.com", YouTubeId.embedHtml(videoId), "text/html", "utf-8", null);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 240));
        lp.topMargin = Ui.dp(a, 12);
        col.addView(web, lp);
        col.addView(button(R.string.player_done, true, new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); } }), wrap(16));
    }

    private void finish() {
        int min = (int) Math.min(120, (System.currentTimeMillis() - openedAt) / 60000L);
        if (min >= 1) ActivityLog.log(a, ActivityLog.LEARN, min);
        web.stopLoading();
        web.loadUrl("about:blank");
        web.destroy();
        host.pop();
    }
}
