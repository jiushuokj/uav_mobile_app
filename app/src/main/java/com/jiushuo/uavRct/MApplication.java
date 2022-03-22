package com.jiushuo.uavRct;

import android.content.Context;

import com.facebook.stetho.Stetho;
import com.jiushuo.uavRct.utils.SysCrashHandler;
import com.secneo.sdk.Helper;

import org.litepal.LitePal;

public class MApplication extends BaseApplication {

    private DemoApplication demoApplication;

    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(MApplication.this);
        if (demoApplication == null) {
            demoApplication = new DemoApplication();
            demoApplication.setContext(this);
        }


    }

    @Override
    public void onCreate() {
        super.onCreate();
        //内存泄露检测工具安装
//        LeakCanary.install(this);
        LitePal.initialize(this);
        Stetho.initializeWithDefaults(this);
        demoApplication.onCreate();
//        Thread.setDefaultUncaughtExceptionHandler(new SysCrashHandler());
    }

}
