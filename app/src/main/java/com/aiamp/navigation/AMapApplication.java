package com.aiamp.navigation;

import android.app.Application;

import com.amap.api.maps.MapsInitializer;
import com.amap.api.services.core.ServiceSettings;

public class AMapApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化高德地图
        MapsInitializer.updatePrivacyShow(this, true, true);
        MapsInitializer.updatePrivacyAgree(this, true);

        // 设置搜索隐私
        ServiceSettings.updatePrivacyShow(this, true, true);
        ServiceSettings.updatePrivacyAgree(this, true);

        // 设置语言为中文
        ServiceSettings.getInstance().setLanguage("zh-CN");
    }
}
