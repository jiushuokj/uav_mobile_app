package com.jiushuo.uavRct.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.jiushuo.uavRct.DemoApplication;
import com.jiushuo.uavRct.R;
import com.jiushuo.uavRct.entity.mqtt.RequestUtil;
import com.jiushuo.uavRct.mqtt.MyMqttService;
import com.jiushuo.uavRct.utils.ToastUtils;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.sdk.camera.Camera;
import dji.thirdparty.io.reactivex.disposables.Disposable;

public class ZoomView extends LinearLayout implements View.OnClickListener {
    private static final String TAG = "ZoomView";
    private Button zoomInButton, zoomOutButton, zoomResetButton;
    private TextView magnificationText;
    private Camera camera = DemoApplication.getAircraftInstance().getCameras().get(0);
    /***
     * 总结digitalZoomFactor， opticalZoomFactor， opticalZoomFocalLength三者关系
     * digitalZoomFactor：数字变焦倍数，表示在光学变焦倍数到最大后（这里最大是30），再放大的参数，这样放大后，画面会很不清楚，最大可以再放大6倍
     * opticalZoomFactor：光学变焦倍数，这里Z30相机支持30倍光学变焦，故可以将图像放大30倍
     * opticalZoomFocalLength = opticalZoomFactor * MinFocalLength（为43）
     */
    private float digitalZoomFactor;
    private float opticalZoomFactor;
    private int opticalZoomFocalLength;
    private SettingsDefinitions.OpticalZoomSpec opticalZoomSpec;
    private final static int UPDATE_TEXT = 1;
    private Disposable disposable;
    private Handler handler = new Handler() {
        @SuppressLint("HandlerLeak")
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_TEXT:
                    magnificationText.setText(String.format("%.1f", (float) (opticalZoomFocalLength / opticalZoomSpec.getMinFocalLength())) + "X");
                    Log.d(TAG, "opticalZoomFocalLength：" + opticalZoomFocalLength);
                    break;
                default:
                    break;
            }
        }
    };

    public ZoomView(Context context) {
        this(context, null);
    }

    public ZoomView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAircraftParams();
        initUI(context);
        doUpdateMagnificationText();
    }


    private void doUpdateMagnificationText() {
        Message message = new Message();
        message.what = UPDATE_TEXT;
        handler.sendMessage(message);
    }

    private void initAircraftParams() {

        if (camera.isDigitalZoomSupported()) {
            camera.getDigitalZoomFactor(new CommonCallbacks.CompletionCallbackWith<Float>() {
                @Override
                public void onSuccess(Float aFloat) {
                    digitalZoomFactor = aFloat;
                    Log.i(TAG, "digitalZoomFactor = " + aFloat);
                }

                @Override
                public void onFailure(DJIError djiError) {
                    Log.e(TAG, "onFailure: " + djiError);
                }
            });
        }

        if (camera.isOpticalZoomSupported()) {
            getFocalLengthParameters();
        }

    }

    private void getFocalLengthParameters() {
        camera.getOpticalZoomFactor(new CommonCallbacks.CompletionCallbackWith<Float>() {
            @Override
            public void onSuccess(Float aFloat) {
                opticalZoomFactor = aFloat;
                Log.i(TAG, "opticalZoomFactor = " + aFloat);
            }

            @Override
            public void onFailure(DJIError djiError) {
                Log.e(TAG, "onFailure: " + djiError);
                ToastUtils.showToast(djiError.getDescription());
            }
        });
        camera.getOpticalZoomFocalLength(new CommonCallbacks.CompletionCallbackWith<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                opticalZoomFocalLength = integer;
                Log.i(TAG, "opticalZoomFocalLength = " + integer);
            }

            @Override
            public void onFailure(DJIError djiError) {
                Log.e(TAG, "onFailure: " + djiError);
            }
        });
        camera.getOpticalZoomSpec(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.OpticalZoomSpec>() {
            @Override
            public void onSuccess(SettingsDefinitions.OpticalZoomSpec zoomSpec) {
                opticalZoomSpec = zoomSpec;
                Log.i(TAG, "opticalZoomSpec: FocalLengthStep = " + zoomSpec.getFocalLengthStep());
                Log.i(TAG, "opticalZoomSpec: MaxFocalLength = " + zoomSpec.getMaxFocalLength());
                Log.i(TAG, "opticalZoomSpec: MinFocalLength = " + zoomSpec.getMinFocalLength());
            }

            @Override
            public void onFailure(DJIError djiError) {
                ToastUtils.showToast(djiError.getDescription());
            }
        });
    }

    private void initUI(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_zoom, this, true);
        zoomInButton = findViewById(R.id.btn_zoom_in);
        zoomOutButton = findViewById(R.id.btn_zoom_out);
        zoomResetButton = findViewById(R.id.btn_zoom_reset);
        magnificationText = findViewById(R.id.text_magnification);

        magnificationText.setText(String.format("%.1f", opticalZoomFactor * digitalZoomFactor) + "X");
        zoomInButton.setOnClickListener(this);
        zoomOutButton.setOnClickListener(this);
        zoomResetButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_zoom_in: {
                zoomIn();
                break;
            }
            case R.id.btn_zoom_out: {
                zoomOut();
                break;
            }
            case R.id.btn_zoom_reset: {
                zoomReset();
                break;
            }
            default: {
                break;
            }
        }
    }

    public void zoomReset() {
        setOpticalZoomFocalLength(opticalZoomSpec.getMinFocalLength());
    }

    public void zoomOut() {
        Log.i(TAG, "zoomOut: ");
        if (opticalZoomFocalLength - opticalZoomSpec.getMinFocalLength() > opticalZoomSpec.getMinFocalLength()) {
            setOpticalZoomFocalLength(opticalZoomFocalLength - opticalZoomSpec.getMinFocalLength());
        } else {
            setOpticalZoomFocalLength(opticalZoomSpec.getMinFocalLength());
        }
    }

    private void updateMagnificationText() {
        camera.getOpticalZoomFocalLength(new CommonCallbacks.CompletionCallbackWith<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                opticalZoomFocalLength = integer;
                Log.i(TAG, "opticalZoomFocalLength = " + integer);
                doUpdateMagnificationText();
                mqttBinder.makeZoomResponse(RequestUtil.CODE_SUCCESS,String.format("%.1f", (float) (opticalZoomFocalLength / opticalZoomSpec.getMinFocalLength())));
            }

            @Override
            public void onFailure(DJIError djiError) {
                Log.e(TAG, "onFailure: " + djiError);

            }
        });
    }

    public void zoomIn() {
        Log.i(TAG, "zoomIn: ");

        if (opticalZoomFocalLength + opticalZoomSpec.getMinFocalLength() < opticalZoomSpec.getMaxFocalLength()) {
            setOpticalZoomFocalLength(opticalZoomFocalLength + opticalZoomSpec.getMinFocalLength());
        } else {
            setOpticalZoomFocalLength(opticalZoomSpec.getMaxFocalLength());
        }
    }

    public void setOpticalZoomFocalLength(int focalLength) {
        camera.setOpticalZoomFocalLength(focalLength, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    Log.i(TAG, "setOpticalZoomFocalLength: " + (focalLength));
                } else {
                    Log.e(TAG, "setOpticalZoomFocalLength: " + djiError.getDescription());
                }
                updateMagnificationText();
            }
        });
    }

    public void setMqttBinder(MyMqttService.MqttBinder mqttBinder) {
        this.mqttBinder = mqttBinder;
    }

    private MyMqttService.MqttBinder mqttBinder;


}
