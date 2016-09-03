package com.kit.qrcode.ui;

import com.kit.extend.qrcode.R;
import com.kit.extend.ui.web.WebActivity;
import com.kit.utils.ClipboardUtils;
import com.kit.utils.ToastUtils;

/**
 * 二维码扫描结果
 */
public class QRResultWebActivity extends WebActivity {

    @Override
    public void initWidgetWithExtra() {

        super.initWidgetWithExtra();

        ClipboardUtils.copy(content);

        ToastUtils.mkLongTimeToast(getString(R.string.copy_ok));

    }
}
