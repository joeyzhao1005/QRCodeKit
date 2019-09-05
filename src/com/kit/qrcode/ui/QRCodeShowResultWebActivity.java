package com.kit.qrcode.ui;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;

import com.kit.extend.qrcode.R;
import com.kit.ui.BaseAppCompatActivity;
import com.kit.utils.ClipboardUtils;
import com.kit.utils.ToastUtils;
import com.kit.utils.WebViewUtils;
import com.kit.utils.log.Zog;

@Deprecated
public class QRCodeShowResultWebActivity extends BaseAppCompatActivity implements OnClickListener {

    private Context mContext;

    //	private LinearLayout llLeft;
    private Toolbar toolbar;

    private WebView wv;

    private String content;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean loadData() {

        return super.loadData();
    }

    @Override
    public void initWidget() {
        mContext = this;
        setContentView(R.layout.browser_activity);
        setToolbar();
//		llLeft = (LinearLayout) findViewById(R.id.llLeft);
        wv = (WebView) findViewById(R.id.wv);

        if (content.startsWith("http://") || content.startsWith("https://"))
            WebViewUtils.loadUrl(mContext, wv, content, true);
        else if (content.startsWith("www."))
            WebViewUtils.loadUrl(mContext, wv, "http://" + content, true);
        else
            WebViewUtils.loadContent(this, wv, content);

        ClipboardUtils.copy(content);

        ToastUtils.mkLongTimeToast(getString(R.string.copy_ok));


    }

    @Override
    protected void getExtra() {
        Bundle bundle = getIntent().getExtras();
        content = bundle.getString("content");
    }

    @Override
    protected void onStart() {

        super.onStart();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;


            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.llLeft) {
            this.finish();
        }

    }

    private void setToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(0x00000000);
        toolbar.setTitle("返回");

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        Zog.i("Build.VERSION.SDK_INT:" + Build.VERSION.SDK_INT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            getSupportActionBar().setHomeActionContentDescription(R.string.back);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.back);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
    }

    /**
     * 按键响应，在WebView中查看网页时，按返回键的时候按浏览历史退回,如果不做此项处理则整个WebView返回退出
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && wv.canGoBack()) {
            // 返回键退回
            wv.goBack();
            return true;
        }
        // If it wasn't the Back key or there's no web page history, bubble up
        // to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event);
    }
}
