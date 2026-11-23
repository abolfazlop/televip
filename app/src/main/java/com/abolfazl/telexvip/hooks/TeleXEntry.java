package com.abolfazl.telexvip.hooks;

import android.util.Log;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class TeleXEntry extends XposedModule {
    private static final String TARGET = "org.telegram.messenger";
    private static final String TAG = "TeleVIP";

    public TeleXEntry() {
        super();
    }

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        log(Log.INFO, TAG, "Abolfazl TeleX VIP bootstrap loaded; API=" + getApiVersion()
                + ", framework=" + getFrameworkName()
                + ", version=" + getFrameworkVersion());
    }

    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam param) {
        if (TARGET.equals(param.getPackageName())) {
            log(Log.INFO, TAG, "Telegram package detected: " + param.getPackageName());
        }
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        if (TARGET.equals(param.getPackageName())) {
            log(Log.INFO, TAG, "Telegram class loader ready; no Telegram methods hooked in this bootstrap.");
        }
    }
}
