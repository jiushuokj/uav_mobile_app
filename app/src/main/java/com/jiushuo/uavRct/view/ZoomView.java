package com.jiushuo.uavRct.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.jiushuo.uavRct.DemoApplication;
import com.jiushuo.uavRct.R;
import com.jiushuo.uavRct.entity.CameraTypeEnum;
import com.jiushuo.uavRct.entity.mqtt.RequestUtil;
import com.jiushuo.uavRct.mqtt.MyMqttService;
import com.jiushuo.uavRct.utils.Common;
import com.jiushuo.uavRct.utils.ToastUtils;

import java.util.List;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.Lens;
import dji.thirdparty.io.reactivex.disposables.Disposable;

public class ZoomView extends LinearLayout implements View.OnClickListener, View.OnTouchListener {
    private static final String TAG = "ZoomView";
    private Button zoomInButton, zoomOutButton, zoomResetButton;
    private TextView magnificationText;
    private Camera camera;
    private Lens lens;
    /**
     * 光学变焦按钮点击时长
     */
    private int zoomTouchTime;
    /**
     * 光学变焦速度
     */
    private SettingsDefinitions.ZoomSpeed zoomSpeed = SettingsDefinitions.ZoomSpeed.SLOWEST;
    /**
     * 光学变焦方向
     */
    private SettingsDefinitions.ZoomDirection zoomDirection;
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
    /**
     * 更新变焦
     */
    private final static int UPDATE_TEXT = 1;
    /**
     * 更新变焦
     */
    private final static int UPDATE_ZOOMSCALE = 2;
    /**
     * 停止变焦
     */
    private final static int STOP_ZOOM = 3;
    private Disposable disposable;
    private Handler handler = new Handler() {
        @SuppressLint("HandlerLeak")
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_TEXT:
                    if (opticalZoomSpec != null) {
                        magnificationText.setText(String.format("%.1f", (float) (opticalZoomFocalLength / opticalZoomSpec.getMinFocalLength())) + "X");
                        Log.d(TAG, "opticalZoomFocalLength：" + opticalZoomFocalLength);
                    }
                    break;
                case UPDATE_ZOOMSCALE:
                    magnificationText.setText(msg.arg1/100 + "mm");
                    break;
                case STOP_ZOOM:
                    zoomStop();
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

        if (Common.CAMERA_TYPE == CameraTypeEnum.H20T) {
            lens = DemoApplication.getProductInstance().getCamera().getLens(0);
            lens.getHybridZoomFocalLength(new CommonCallbacks.CompletionCallbackWith<Integer>() {
                @Override
                public void onSuccess(Integer integer) {
                    Message message = new Message();
                    message.what = UPDATE_ZOOMSCALE;
                    message.arg1 = integer;
                    handler.sendMessage(message);
                }

                @Override
                public void onFailure(DJIError djiError) {

                }
            });

        } else {
            camera = DemoApplication.getAircraftInstance().getCameras().get(0);
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
        }
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
        zoomInButton.setOnTouchListener(this);
        zoomOutButton.setOnTouchListener(this);
    }


    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            switch (view.getId()) {
                case R.id.btn_zoom_in: {
                    zoomDirection = SettingsDefinitions.ZoomDirection.ZOOM_IN;
                    lensZoom();
                    break;
                }
                case R.id.btn_zoom_out: {
                    zoomDirection = SettingsDefinitions.ZoomDirection.ZOOM_OUT;
                    lensZoom();
                    break;
                }

                default: {
                    break;
                }
            }
        } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE){
            zoomTouchTime ++;
            if (zoomTouchTime == 10) {
                zoomSpeed = SettingsDefinitions.ZoomSpeed.SLOW;
            } else if (zoomTouchTime == 20) {
                zoomSpeed = SettingsDefinitions.ZoomSpeed.MODERATELY_SLOW;
            } else if (zoomTouchTime == 30) {
                zoomSpeed = SettingsDefinitions.ZoomSpeed.NORMAL;
            } else if (zoomTouchTime == 40) {
                zoomSpeed = SettingsDefinitions.ZoomSpeed.MODERATELY_FAST;
            } else if (zoomTouchTime == 50) {
                zoomSpeed = SettingsDefinitions.ZoomSpeed.FAST;
            } else if (zoomTouchTime == 60) {
                zoomSpeed = SettingsDefinitions.ZoomSpeed.FASTEST;
            }

            lensZoom();
        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP){
            zoomStop();
            zoomSpeed = SettingsDefinitions.ZoomSpeed.SLOWEST;
            zoomTouchTime = 0;
        }
        return true;
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

    /**
     * 开始光学变焦
     */
    public void lensZoom() {
        lens.startContinuousOpticalZoom(zoomDirection, zoomSpeed, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                lens.getHybridZoomFocalLength(new CommonCallbacks.CompletionCallbackWith<Integer>() {
                    @Override
                    public void onSuccess(Integer integer) {
                        Message message = new Message();
                        message.what = UPDATE_ZOOMSCALE;
                        message.arg1 = integer;
                        handler.sendMessage(message);
                    }

                    @Override
                    public void onFailure(DJIError djiError) {

                    }
                });
            }
        });
    }

    /**
     * 停止光学变焦
     */
    public void zoomStop() {
        lens.stopContinuousOpticalZoom(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                lens.getHybridZoomFocalLength(new CommonCallbacks.CompletionCallbackWith<Integer>() {
                    @Override
                    public void onSuccess(Integer integer) {
                        Message message = new Message();
                        message.what = UPDATE_ZOOMSCALE;
                        message.arg1 = integer;
                        handler.sendMessage(message);
                    }

                    @Override
                    public void onFailure(DJIError djiError) {

                    }
                });
            }
        });
    }


    public void zoomReset() {
        if (Common.CAMERA_TYPE == CameraTypeEnum.H20T) {
            lens.startContinuousOpticalZoom(SettingsDefinitions.ZoomDirection.ZOOM_OUT, SettingsDefinitions.ZoomSpeed.FASTEST, null);
        } else {
            setOpticalZoomFocalLength(opticalZoomSpec.getMinFocalLength());
        }
    }

    public void zoomOut() {
        Log.i(TAG, "zoomOut: ");
        if (Common.CAMERA_TYPE == CameraTypeEnum.H20T) {
            lens.startContinuousOpticalZoom(SettingsDefinitions.ZoomDirection.ZOOM_OUT, SettingsDefinitions.ZoomSpeed.FASTEST,  new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    lens.getHybridZoomFocalLength(new CommonCallbacks.CompletionCallbackWith<Integer>() {
                        @Override
                        public void onSuccess(Integer integer) {
                            Message message = new Message();
                            message.what = UPDATE_ZOOMSCALE;
                            message.arg1 = integer;
                            handler.sendMessage(message);
                        }

                        @Override
                        public void onFailure(DJIError djiError) {

                        }
                    });
                }
            });
            handler.sendEmptyMessageDelayed(STOP_ZOOM, 1000);
        } else {
            if (opticalZoomFocalLength - opticalZoomSpec.getMinFocalLength() > opticalZoomSpec.getMinFocalLength()) {
                setOpticalZoomFocalLength(opticalZoomFocalLength - opticalZoomSpec.getMinFocalLength());
            } else {
                setOpticalZoomFocalLength(opticalZoomSpec.getMinFocalLength());
            }
        }
    }

    public void zoomIn() {
        Log.i(TAG, "zoomIn: ");
        if (Common.CAMERA_TYPE == CameraTypeEnum.H20T) {
            lens.startContinuousOpticalZoom(SettingsDefinitions.ZoomDirection.ZOOM_IN, SettingsDefinitions.ZoomSpeed.FASTEST,  new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    lens.getHybridZoomFocalLength(new CommonCallbacks.CompletionCallbackWith<Integer>() {
                        @Override
                        public void onSuccess(Integer integer) {
                            Message message = new Message();
                            message.what = UPDATE_ZOOMSCALE;
                            message.arg1 = integer;
                            handler.sendMessage(message);
                        }

                        @Override
                        public void onFailure(DJIError djiError) {

                        }
                    });
                }
            });
            handler.sendEmptyMessageDelayed(STOP_ZOOM, 1000);
        } else {
            if (opticalZoomFocalLength + opticalZoomSpec.getMinFocalLength() < opticalZoomSpec.getMaxFocalLength()) {
                setOpticalZoomFocalLength(opticalZoomFocalLength + opticalZoomSpec.getMinFocalLength());
            } else {
                setOpticalZoomFocalLength(opticalZoomSpec.getMaxFocalLength());
            }
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
        });
    }

    public void setMqttBinder(MyMqttService.MqttBinder mqttBinder) {
        this.mqttBinder = mqttBinder;
    }

    private MyMqttService.MqttBinder mqttBinder;


}
