package com.littlemann766.mybills;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        // Always request the newest GitHub Pages version.
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.clearCache(true);

        webView.setWebViewClient(new WebViewClient());

        // Cache-busting query keeps the APK synced with the live website.
        String url = "https://littlemann766.github.io/MyBills/?app=" + System.currentTimeMillis();
        webView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
