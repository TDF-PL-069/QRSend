
package com.atakmap.android.QRSend;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.atakmap.android.QRSend.plugin.R;
import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;


public class QRSendComponent extends DropDownMapComponent {

    private static final String TAG = "QRSendComponent";


    @Override
    public void onResume(Context context, MapView view) {
        super.onResume(context, view);
   
    }

    public void onCreate(final Context context, Intent intent, final MapView view) {
        context.setTheme(R.style.ATAKPluginTheme);

        QRSendDropDownReceiver dropDown = new QRSendDropDownReceiver(view, context);

        AtakBroadcast.DocumentedIntentFilter ddFilter = new AtakBroadcast.DocumentedIntentFilter();
        ddFilter.addAction(QRSendDropDownReceiver.SHOW_LAYOUT,
                "Show the SARToolkit drop-down");
        this.registerDropDownReceiver(dropDown, ddFilter);


    }


    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        Log.d(TAG, "onDestroyImpl");

    }


}