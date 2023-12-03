package com.atakmap.android.QRSend.plugin;


import com.atak.plugins.impl.AbstractPlugin;
import gov.tak.api.plugin.IServiceController;
import com.atak.plugins.impl.PluginContextProvider;
import com.atakmap.android.QRSend.QRSendComponent;
import com.atakmap.android.QRSend.QRSendTool;

/**
 *
 * 
 *
 */
public class PluginTemplateLifecycle extends AbstractPlugin {

   public PluginTemplateLifecycle(IServiceController serviceController) {
        super(serviceController, new QRSendTool(serviceController.getService(PluginContextProvider.class).getPluginContext()), new QRSendComponent());
        PluginNativeLoader.init(serviceController.getService(PluginContextProvider.class).getPluginContext());
    }
}

