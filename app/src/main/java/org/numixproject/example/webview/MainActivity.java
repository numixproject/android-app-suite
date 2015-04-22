package org.numixproject.example.webview;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import static android.webkit.WebSettings.LOAD_DEFAULT;

public class MainActivity extends Activity {

    // Add your HTML, JavaScript and CSS files under /src/main/assets/www/
    // If you want to use a remote URL, change the value of INDEX
    // You also need to add internet permissions in the manifest file
    private final String INDEX = "file:///android_asset/www/index.html";

    private WebView webView;

    // Handle back button press
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        webView = (WebView) findViewById(R.id.webview);

        WebSettings webSettings = webView.getSettings();

        String appCachePath = getApplicationContext().getCacheDir().getAbsolutePath();

        webSettings.setJavaScriptEnabled(true); // Enable JavaScript
        webSettings.setSupportZoom(false); // Disable Zoom buttons
        webSettings.setDomStorageEnabled(true); // Enable localStorage
        webSettings.setAppCacheEnabled(true); // Enable appCache
        webSettings.setAppCachePath(appCachePath); // Set appCache path (needed)
        webSettings.setAllowFileAccess(true); // Enable import from file URLs
        webSettings.setCacheMode(LOAD_DEFAULT);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            String databasePath = getApplicationContext().getDir("databases", Context.MODE_PRIVATE).getPath();

            webSettings.setDatabaseEnabled(true);
            webSettings.setDatabasePath(databasePath);
        }

        webView.setWebViewClient(new webViewClient());

        // Enable debugging in webview
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        }

        // Load the web page
        webView.loadUrl(INDEX);
    }

    private class webViewClient extends WebViewClient {
        // Show a splash screen until the WebView is ready
        @Override
        public void onPageFinished(WebView view, String url) {
            findViewById(R.id.imageView1).setVisibility(View.GONE);
            findViewById(R.id.webview).setVisibility(View.VISIBLE);
        }
    }

}
