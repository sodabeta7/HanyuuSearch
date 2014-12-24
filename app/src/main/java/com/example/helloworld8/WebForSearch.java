package com.example.helloworld8;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.example.MashapeHello.R;

public class WebForSearch extends Activity {
    private WebView web_view;
    private String url;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_for_search);
        Intent intent = getIntent();
        url = intent.getStringExtra(MainActivity.SEARCH_URL_MESSAGE);
        Log.d("search_url", url);
        web_view = (WebView)findViewById(R.id.web_view);
        web_view.getSettings().setJavaScriptEnabled(true);//JS
        web_view.getSettings().setBuiltInZoomControls(true);//zoom

        web_view.loadUrl(url);
        web_view.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // TODO Auto-generated method stub
                view.loadUrl(url);
                return true;
            }
            @Override   //When error
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {
                // TODO Auto-generated method stub
                Toast.makeText(WebForSearch.this, "Oh no! " + description, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
        if ((keyCode == KeyEvent.KEYCODE_BACK) && web_view.canGoBack()) {
            web_view.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.web_for_search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.open_browser) {
            Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(web_view.getUrl()));
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
