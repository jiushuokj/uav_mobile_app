package com.jiushuo.uavRct;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 添加Activity到堆栈
        DemoApplication.getInstance().addActivity(this);
//        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//        int memory = manager.getMemoryClass();
//        int largememory = manager.getLargeMemoryClass();
//        Toast.makeText(this, "" + memory + "   " + largememory, Toast.LENGTH_SHORT).show();
    }
}