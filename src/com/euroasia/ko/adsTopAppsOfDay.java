package com.euroasia.ko;

import com.euroasiamp3.eula.GUtils;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class adsTopAppsOfDay extends Activity {
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adstopapps);
        GUtils.getGTRACKER(this).trackPageViewEvent("adsTopAppsOfDay");
        
        WebView myWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        myWebView.loadUrl(getString(R.string.topappsurl)); 
    }
}
