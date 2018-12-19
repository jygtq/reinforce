package com.example.reinforce.source;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

/**
 * 作者：created by wujin on 2018/12/11
 * 邮箱：jin.wu@geely.com
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "source apk onCreate", Toast.LENGTH_SHORT).show();
    }
}
