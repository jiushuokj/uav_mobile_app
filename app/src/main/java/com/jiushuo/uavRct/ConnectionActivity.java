package com.jiushuo.uavRct;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.jiushuo.uavRct.entity.CameraTypeEnum;
import com.jiushuo.uavRct.utils.Common;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.keysdk.ProductKey;
import dji.keysdk.callback.KeyListener;
import dji.log.DJILog;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;

@RequiresApi(api = Build.VERSION_CODES.M)
public class ConnectionActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "连接活动";

    private TextView mTextConnectionStatus;
    private TextView mTextProduct;
    private TextView mTextModelAvailable;
    private TextView mVersionTv;

    private EditText mBridgeModeEditText;

    private Button mBtnOpen, mBtnExit;
    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,

    };
    private List<String> missingPermission = new ArrayList<>();

    /**
     * 授权是否成功
     */
    private boolean isPermissionSucc = false;
    /**
     * 登录是否成功
     */
    private boolean isLoginSucc = false;
    /**
     * 注册是否成功
     */
    private boolean isRegisterSucc = false;
    /**
     * 连接是否成功
     */
    private boolean isConnectionSucc = false;

    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private static final int REQUEST_PERMISSION_CODE = 12345;
    private DJIKey firmwareKey;
    private KeyListener firmwareVersionUpdater;
    private boolean hasStartedFirmVersionListener = false;

    private static Activity mActivity;
    private static final int LOGIN_SUCC = 0;
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case LOGIN_SUCC:
                    Intent intent = new Intent(mActivity, MainActivity.class);
                    mActivity.startActivity(intent);
                    mActivity.finish();
                    break;
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        checkAndRequestPermissions();
//        loginAccount();
        setContentView(R.layout.activity_connection);
        initUI();

        // Register the broadcast receiver for receiving the device connection's changes.
        IntentFilter filter = new IntentFilter();
        filter.addAction(DemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);
    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
                for (String s : missingPermission) {
                    Log.e(TAG, "checkAndRequestPermissions: 动态申请权限之前的missingPermission " + s);
                }
            }
        }
        // RequestUtil for missing permissions
        if (!missingPermission.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.e(TAG, "权限不足，申请权限: ");
            ActivityCompat.requestPermissions(ConnectionActivity.this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        } else {
            isPermissionSucc = true;
            startSDKRegistration();
        }

    }

    /**
     * Result of runtime permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        Log.e(TAG, "onRequestPermissionsResult: ");
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                } else {
                    switch (permissions[i]) {
                        case Manifest.permission.ACCESS_FINE_LOCATION:
                            showToast("位置信息获取授权失败");
                            break;
                        case Manifest.permission.READ_EXTERNAL_STORAGE:
                            showToast("文件读取授权失败");
                            break;
                        case Manifest.permission.READ_PHONE_STATE:
                            showToast("电话管理授权失败");
                            break;
                    }
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            Log.e(TAG, "onRequestPermissionsResult:动态申请权限之后," + missingPermission.size());
            startSDKRegistration();
            isPermissionSucc = true;
        } else {
            for (String s : missingPermission) {
                Log.e(TAG, "onRequestPermissionsResult:动态申请权限之后," + s);
            }
//            showToast("Missing permissions!!!");
        }
    }

    private void startSDKRegistration() {

        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
//                    showToast("正在注册, 请稍等...");
                    DJISDKManager.getInstance().registerApp(getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                        @Override
                        public void onRegister(DJIError djiError) {
                            if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                                DJILog.e("App registration", DJISDKError.REGISTRATION_SUCCESS.getDescription());
                                DJISDKManager.getInstance().startConnectionToProduct();
                                showToast("注册成功");
                                isRegisterSucc = true;
//                                loginAccount();
                            } else {
                                showToast("注册SDK失败，请检查网络状态");
                            }
                            Log.v(TAG, djiError.getDescription());
                        }

                        @Override
                        public void onProductDisconnect() {
                            Log.d(TAG, "onProductDisconnect");
                            showToast("无人机断开连接");
                        }

                        @Override
                        public void onProductConnect(BaseProduct baseProduct) {
                            if (baseProduct.isConnected()) {
                                Log.d(TAG, String.format("onProductConnect newProduct:%s", baseProduct));
//                                showToast("无人机已连接");
                                isConnectionSucc = true;
//                                mHandler.sendEmptyMessage(LOGIN_SUCC);
                                updateTitleBar();
                            }
                        }

                        @Override
                        public void onProductChanged(BaseProduct baseProduct) {

                        }

                        @Override
                        public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent,
                                                      BaseComponent newComponent) {

                            if (newComponent != null) {
                                newComponent.setComponentListener(new BaseComponent.ComponentListener() {

                                    @Override
                                    public void onConnectivityChange(boolean isConnected) {
                                        Log.d(TAG, "onComponentConnectivityChanged: " + isConnected);
                                    }
                                });
                            }
                            Log.d(TAG,
                                    String.format("onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                                            componentKey,
                                            oldComponent,
                                            newComponent));

                        }

                        @Override
                        public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) {

                        }

                        @Override
                        public void onDatabaseDownloadProgress(long l, long l1) {

                        }
                    });
                }
            });
        }
    }

    private void refreshSDKRelativeUIRunOnUIThread() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refreshSDKRelativeUI();
            }
        });
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
//        startSDKRegistration();
//        updateTitleBar();
//        refreshSDKRelativeUI();
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    public void onReturn(View view) {
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
        unregisterReceiver(mReceiver);
        removeFirmwareVersionListener();
        mHandler = null;

    }

    private void initUI() {

        mTextConnectionStatus = (TextView) findViewById(R.id.text_connection_status);
        mTextModelAvailable = (TextView) findViewById(R.id.text_model_available);
        mTextProduct = (TextView) findViewById(R.id.text_product_info);

        mBridgeModeEditText = findViewById(R.id.edittext_bridge_ip);
        mBridgeModeEditText.setText("192.168.8.104");
        mBridgeModeEditText.setFocusable(false);
        mBridgeModeEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBridgeModeEditText.setFocusableInTouchMode(true);
                mBridgeModeEditText.setFocusable(true);
                mBridgeModeEditText.requestFocus();
            }
        });

        mBridgeModeEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || event != null
                        && event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    if (event != null && event.isShiftPressed()) {
                        return false;
                    } else {
                        // the user is done typing.
                        handleBridgeIPTextChange();
                    }
                }
                return false; // pass on to other listeners.
            }
        });

        mBridgeModeEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null && s.toString().contains("\n")) {
                    // the user is done typing.
                    // remove new line characcter
                    final String currentText = mBridgeModeEditText.getText().toString();
                    mBridgeModeEditText.setText(currentText.substring(0, currentText.indexOf('\n')));
                    handleBridgeIPTextChange();
                }
            }
        });

        mBtnOpen = (Button) findViewById(R.id.btn_open);
        mBtnOpen.setOnClickListener(this);


        mBtnExit = findViewById(R.id.btn_exit);
        mBtnExit.setOnClickListener(this);

    }

    private void handleBridgeIPTextChange() {
        // the user is done typing.
        final String bridgeIP = mBridgeModeEditText.getText().toString();
        DJISDKManager.getInstance().enableBridgeModeWithBridgeAppIP(bridgeIP);
        if (!TextUtils.isEmpty(bridgeIP)) {
            showToast("BridgeMode ON!\nIP: " + bridgeIP);
        }
    }


    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
//            refreshSDKRelativeUI();
//            showToast("广播回调");
        }
    };

    private void updateTitleBar() {
        boolean ret = false;
        BaseProduct product = DemoApplication.getProductInstance();
        if (product != null) {
            if (product.isConnected()) {
                //The product is connected
                showToast(DemoApplication.getProductInstance().getModel() + " 连接成功");
                String displayName = DemoApplication.getProductInstance().getCamera().getDisplayName();
                if (displayName.equals("Zenmuse H20T")) {
                    Common.CAMERA_TYPE = CameraTypeEnum.H20T;
                } else {
                    Common.CAMERA_TYPE = CameraTypeEnum.DEFAULT;
                }
                ret = true;
            } else {
                if (product instanceof Aircraft) {
                    Aircraft aircraft = (Aircraft) product;
                    if (aircraft.getRemoteController() != null && aircraft.getRemoteController().isConnected()) {
                        // The product is not connected, but the remote controller is connected
                        showToast("只有遥控器连接");
                        ret = true;
                    }
                }
            }
        }

    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(ConnectionActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateVersion() {
        String version = null;
        if (DemoApplication.getProductInstance() != null) {
            version = DemoApplication.getProductInstance().getFirmwarePackageVersion();
        }

        if (TextUtils.isEmpty(version)) {
            mTextModelAvailable.setText("固件版本:N/A"); //Firmware version:
        } else {
            mTextModelAvailable.setText("固件版本:" + version); //"Firmware version: " +
            removeFirmwareVersionListener();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btn_open: {
                if (!isPermissionSucc) {
                    checkAndRequestPermissions();
                } else if (!isRegisterSucc) {
                    showToast("未注册成功");
                } else if (!isConnectionSucc) {
                    showToast("无人机未连接");
                } else {
                    mHandler.sendEmptyMessage(LOGIN_SUCC);
                }

                break;
            }

            case R.id.btn_exit: {
                DemoApplication.getInstance().exit();
                break;
            }
            default:
                break;
        }
    }

    private void refreshSDKRelativeUI() {
        BaseProduct mProduct = DemoApplication.getProductInstance();

        if (null != mProduct && mProduct.isConnected()) {
            Log.v(TAG, "refreshSDK: True");
            mBtnOpen.setEnabled(true);

            String str = mProduct instanceof Aircraft ? "无人机" : "手持设备";
            mTextConnectionStatus.setText("状态: " + str + " 已连接");
            tryUpdateFirmwareVersionWithListener();

            if (null != mProduct.getModel()) {
                mTextProduct.setText("" + mProduct.getModel().getDisplayName());
            } else {
                mTextProduct.setText(R.string.product_information);
            }

            loginAccount();

        } else {
            Log.v(TAG, "refreshSDK: False");
            mBtnOpen.setEnabled(false);

            mTextProduct.setText(R.string.product_information);
            mTextConnectionStatus.setText(R.string.connection_loose);
        }
    }

    private void loginAccount() {
        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Log.e(TAG, "Login Success");
                        showToast("登录成功!");
                        isLoginSucc = true;
                    }

                    @Override
                    public void onFailure(DJIError error) {
                        showToast("登录错误:"
                                + error.getDescription());
                    }
                });
    }

    private void tryUpdateFirmwareVersionWithListener() {
        if (!hasStartedFirmVersionListener) {
            firmwareVersionUpdater = new KeyListener() {
                @Override
                public void onValueChange(final Object o, final Object o1) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateVersion();
                        }
                    });
                }
            };
            firmwareKey = ProductKey.create(ProductKey.FIRMWARE_PACKAGE_VERSION);
            if (KeyManager.getInstance() != null) {
                KeyManager.getInstance().addListener(firmwareKey, firmwareVersionUpdater);
            }
            hasStartedFirmVersionListener = true;
        }
        updateVersion();
    }

    private void removeFirmwareVersionListener() {
        if (hasStartedFirmVersionListener) {
            if (KeyManager.getInstance() != null) {
                KeyManager.getInstance().removeListener(firmwareVersionUpdater);
            }
        }
        hasStartedFirmVersionListener = false;
    }

}
