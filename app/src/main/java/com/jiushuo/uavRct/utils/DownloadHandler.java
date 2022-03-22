package com.jiushuo.uavRct.utils;

import android.graphics.Bitmap;

import dji.common.error.DJIError;
import dji.sdk.media.DownloadListener;

public class DownloadHandler<B> implements DownloadListener<B> {

    @Override
    public void onStart() {

    }

    @Override
    public void onRateUpdate(long total, long current, long arg2) {

    }

    @Override
    public void onRealtimeDataUpdate(byte[] bytes, long l, boolean b) {

    }

    @Override
    public void onProgress(long total, long current) {

    }

    @Override
    public void onSuccess(B obj) {
        if (obj instanceof Bitmap) {
            Bitmap bitmap = (Bitmap) obj;
            ToastUtils.setResultToToast("Success! The bitmap's byte count is: " + bitmap.getByteCount());
        } else if (obj instanceof String) {
            ToastUtils.setResultToToast("The file has been stored, its path is " + obj.toString());
        }
    }

    @Override
    public void onFailure(DJIError djiError) {

    }
}
