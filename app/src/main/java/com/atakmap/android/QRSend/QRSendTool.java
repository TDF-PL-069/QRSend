package com.atakmap.android.QRSend;

import android.content.Context;

import com.atak.plugins.impl.AbstractPluginTool;
import com.atakmap.android.QRSend.plugin.R;

import static com.atakmap.android.QRSend.QRSendDropDownReceiver.SHOW_LAYOUT;

public class QRSendTool extends AbstractPluginTool {

    public QRSendTool(final Context context) {
        super(context, context.getString(R.string.app_name), context.getString(R.string.app_name),
                context.getResources().getDrawable(R.drawable.ic_qrsend),
                SHOW_LAYOUT);
    }
}