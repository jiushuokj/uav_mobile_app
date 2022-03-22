package com.jiushuo.uavRct.settingpanel.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jiushuo.uavRct.DemoApplication;
import com.jiushuo.uavRct.MainActivity;
import com.jiushuo.uavRct.R;

import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;

public class FlightControllerFragment extends Fragment {
    private static final String TAG = "飞控参数设置";
    private View view;

    FlightController flightController;
    private boolean multipleFlightModeEnabled;
    private int goHomeHeightInMeters;
    private int maxFlightHeight;
    private boolean maxFlightRadiusLimitationEnabled;
    private int maxFlightRadius;

    private Spinner spinnerGoHome;
    private Switch switchFightModeEnabled;
    private EditText editTextGoHomeHeight;
    private EditText editTextMaxHeight;
    private Switch switchMaxDistanceEnabled;
    private EditText editTextMaxDistance;
    private ImageButton nextImgBtnSensorState;
    private Spinner spinnerOutOfControlBehavior;
    private Button btnGravitySelfCalibration;
    private ImageButton nextImgBtnExtendIoOptions;//进入扩展IO配置页面的按钮
    private LinearLayout layoutFlightControllerSetting, layoutExtendIoSetting;
    private ImageButton btnExpandIoSettingReturn;//返回飞控参数设置页面的按钮
    private Switch switchPowerSupplyPort, switch_osdk_communication_serial_port, switch_time_sync_and_pwm_function;
    private InputMethodManager imm;

    private MainActivity mainActivity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = inflater.inflate(R.layout.tab1, container, false);
        mainActivity = (MainActivity) getActivity();
        imm = (InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        initUI();
        initParams();
        initEvents();
        return view;
    }

    private void initEvents() {
        initSwitchFightModeEnabledEvent();
        initEditTextGoHomeHeightEvents();
        initEditTextMaxHeightEvents();
        initSwitchMaxDistanceEnabledEvent();
        initEditTextMaxDistanceEvents();
        addNextImgBtnExtendIoOptionsEvent();
        addBtnExpandIoSettingReturnEvent();
//        addSwitchPowerSupplyPortEvent();
    }

    private void addBtnExpandIoSettingReturnEvent() {
        btnExpandIoSettingReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutFlightControllerSetting.setVisibility(View.VISIBLE);
                layoutExtendIoSetting.setVisibility(View.GONE);
            }
        });
    }

    private void addNextImgBtnExtendIoOptionsEvent() {
        nextImgBtnExtendIoOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutFlightControllerSetting.setVisibility(View.GONE);
                layoutExtendIoSetting.setVisibility(View.VISIBLE);
            }
        });
    }

    private void addSwitchPowerSupplyPortEvent() {
        switchPowerSupplyPort.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                flightController.setPowerSupplyPortEnabled(isChecked, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            setResultToToast("使能外部供电口，设置失败：" + djiError.getDescription());
                            Log.i(TAG, "setPowerSupplyPortEnabled 失败：" + djiError.getDescription());
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    switchPowerSupplyPort.setChecked(!isChecked);
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    private void initEditTextMaxDistanceEvents() {
        editTextMaxDistance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editTextMaxDistance.setFocusableInTouchMode(true);
                editTextMaxDistance.setFocusable(true);
                editTextMaxDistance.requestFocus();
            }
        });
        editTextMaxDistance.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
                        Log.i(TAG, "editTextMaxDistance: " + editTextMaxDistance.getText());
                        final String currentText = editTextMaxDistance.getText().toString();
                        if ("".equals(currentText)) {
                            setResultToToast("设置失败！" + "当前值不能为空");
                            getMaxFlightRadius();
                        } else if (!currentText.matches("[0-9]+")) {
                            setResultToToast("设置失败！" + "当前值只能为整数");
                            getMaxFlightRadius();
                        } else {
                            flightController.setMaxFlightRadius(Integer.parseInt(currentText), new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(final DJIError djiError) {
                                    if (djiError == null) {
                                        getMaxFlightRadius();
//                                        setResultToToast("设置成功！");
                                        Log.i(TAG, "editTextMaxDistance: 设置成功！" + editTextMaxDistance.getText());
                                    } else {
                                        setResultToToast("editTextMaxDistance：设置失败！" + djiError.getDescription());
                                        Log.i(TAG, "editTextMaxDistance: 设置失败！" + djiError.getDescription());
                                        getMaxFlightRadius();
                                    }
                                }
                            });
                        }
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                editTextMaxDistance.setFocusable(false);
                                editTextMaxDistance.setFocusableInTouchMode(false);
                            }
                        });
                        //隐藏软键盘
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                }
                return false; // pass on to other listeners.
            }
        });
    }

    private void initSwitchMaxDistanceEnabledEvent() {
        switchMaxDistanceEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                flightController.setMaxFlightRadiusLimitationEnabled(isChecked, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError == null) {
//                            setResultToToast("设置成功！");
                            Log.i(TAG, "switchFightModeEnabled: 设置成功！" + switchMaxDistanceEnabled.isChecked());
                        } else {
                            setResultToToast("设置失败！" + djiError.getDescription());
                            Log.i(TAG, "switchFightModeEnabled: 设置失败！" + djiError.getDescription());
                            getMaxFlightRadiusLimitationEnabled();
                        }
                    }
                });
            }

        });
    }

    private void initSwitchFightModeEnabledEvent() {
        switchFightModeEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                flightController.setMultipleFlightModeEnabled(isChecked, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError == null) {
                            getMultipleFlightModeEnabled();
                            Log.i(TAG, "switchFightModeEnabled: 设置成功！" + switchFightModeEnabled.isChecked());
                        } else {
                            getMultipleFlightModeEnabled();
                            setResultToToast("设置失败！" + djiError.getDescription());
                            Log.i(TAG, "switchFightModeEnabled: 设置失败！" + djiError.getDescription());
                        }
                    }
                });
            }

        });
    }

    private void initEditTextMaxHeightEvents() {
        editTextMaxHeight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editTextMaxHeight.setFocusableInTouchMode(true);
                editTextMaxHeight.setFocusable(true);
                editTextMaxHeight.requestFocus();
            }
        });
        editTextMaxHeight.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
                        Log.i(TAG, "editTextMaxHeight: " + editTextMaxHeight.getText());
                        final String currentText = editTextMaxHeight.getText().toString();
                        if ("".equals(currentText)) {
                            setResultToToast("设置失败！" + "当前值不能为空");
                            getMaxFlightHeight();
                        } else if (!currentText.matches("[0-9]+")) {
                            setResultToToast("设置失败！" + "当前值只能为整数");
                            getMaxFlightHeight();
                        } else {
                            flightController.setMaxFlightHeight(Integer.parseInt(currentText), new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(final DJIError djiError) {
                                    if (djiError == null) {
                                        getMaxFlightHeight();
//                                        setResultToToast("设置成功！");
                                        Log.i(TAG, "editTextMaxHeight: 设置成功！" + editTextMaxHeight.getText());
                                    } else {
                                        setResultToToast("editTextMaxHeight：设置失败！" + djiError.getDescription());
                                        Log.i(TAG, "editTextMaxHeight: 设置失败！" + djiError.getDescription());
                                        getMaxFlightHeight();
                                    }
                                }
                            });
                        }
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                editTextMaxHeight.setFocusable(false);
                                editTextMaxHeight.setFocusableInTouchMode(false);
                            }
                        });
                        //隐藏软键盘
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                }
                return false; // pass on to other listeners.
            }
        });
    }

    private void initEditTextGoHomeHeightEvents() {
        editTextGoHomeHeight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editTextGoHomeHeight.setFocusableInTouchMode(true);
                editTextGoHomeHeight.setFocusable(true);
                editTextGoHomeHeight.requestFocus();
            }
        });
        editTextGoHomeHeight.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
                        Log.i(TAG, "editTextGoHomeHeight: " + editTextGoHomeHeight.getText());
                        final String currentText = editTextGoHomeHeight.getText().toString();
                        if ("".equals(currentText)) {
                            setResultToToast("设置失败！" + "当前值不能为空");
                            getGoHomeHeightInMeters();
                        } else if (!currentText.matches("[0-9]+")) {
                            setResultToToast("设置失败！" + "当前值只能为整数");
                            getGoHomeHeightInMeters();
                        } else {
                            flightController.setGoHomeHeightInMeters(Integer.parseInt(currentText), new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(final DJIError djiError) {
                                    if (djiError == null) {
                                        getGoHomeHeightInMeters();
//                                        setResultToToast("设置成功！");
                                        Log.i(TAG, "editTextGoHomeHeight: 设置成功！" + editTextGoHomeHeight.getText());
                                    } else {
                                        setResultToToast("editTextGoHomeHeight：设置失败！" + djiError.getDescription());
                                        Log.i(TAG, "editTextGoHomeHeight: 设置失败！" + djiError.getDescription());
                                        getGoHomeHeightInMeters();
                                    }
                                }
                            });
                        }
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                editTextGoHomeHeight.setFocusable(false);
                                editTextGoHomeHeight.setFocusableInTouchMode(false);
                            }
                        });
                        //隐藏软键盘
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                }
                return false; // pass on to other listeners.
            }
        });
    }

    private void initUI() {
        spinnerGoHome = view.findViewById(R.id.spinner_go_home);
        switchFightModeEnabled = view.findViewById(R.id.switch_fight_mode_enabled);
        editTextGoHomeHeight = view.findViewById(R.id.edit_text_go_home_height);
        editTextMaxHeight = view.findViewById(R.id.edit_text_max_height);
        switchMaxDistanceEnabled = view.findViewById(R.id.switch_max_distance_enabled);
        editTextMaxDistance = view.findViewById(R.id.edit_text_max_distance);
        nextImgBtnSensorState = view.findViewById(R.id.next_img_btn_sensor_state);
        spinnerOutOfControlBehavior = view.findViewById(R.id.spinner_out_of_control_behavior);
        btnGravitySelfCalibration = view.findViewById(R.id.btn_gravity_self_calibration);
        nextImgBtnExtendIoOptions = view.findViewById(R.id.next_img_btn_extend_io_options);
        layoutFlightControllerSetting = view.findViewById(R.id.layout_flight_controller_setting);
        layoutExtendIoSetting = view.findViewById(R.id.layout_extend_io_setting);
        btnExpandIoSettingReturn = view.findViewById(R.id.btn_expand_io_setting_return);
        switchPowerSupplyPort = view.findViewById(R.id.switch_power_supply_port);
    }

    private void initParams() {
        flightController = DemoApplication.getAircraftInstance().getFlightController();
        getMultipleFlightModeEnabled();
        getGoHomeHeightInMeters();
        getMaxFlightHeight();
        getMaxFlightRadiusLimitationEnabled();
        getMaxFlightRadius();
//        initSwitchPowerSupplyPortValue();
    }

    private void initSwitchPowerSupplyPortValue() {
        flightController.getPowerSupplyPortEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
            @Override
            public void onSuccess(final Boolean aBoolean) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switchPowerSupplyPort.setChecked(aBoolean);
                    }
                });
            }

            @Override
            public void onFailure(DJIError djiError) {
                setResultToToast("使能外部供电口，获取失败：" + djiError.getDescription());
                Log.i(TAG, "init getPowerSupplyPortEnabled onFailure: " + djiError.getDescription());
            }
        });
    }

    private void getMultipleFlightModeEnabled() {
        flightController.getMultipleFlightModeEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                multipleFlightModeEnabled = aBoolean;
                Log.i(TAG, "getMultipleFlightModeEnabled: " + multipleFlightModeEnabled);
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switchFightModeEnabled.setChecked(multipleFlightModeEnabled);
                    }
                });
            }

            @Override
            public void onFailure(DJIError djiError) {
                Log.i(TAG, "getMultipleFlightModeEnabled: " + djiError.getDescription());
            }
        });
    }

    private void getGoHomeHeightInMeters() {
        flightController.getGoHomeHeightInMeters(new CommonCallbacks.CompletionCallbackWith<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                goHomeHeightInMeters = integer;
                Log.i(TAG, "getGoHomeHeightInMeters: " + goHomeHeightInMeters);
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        editTextGoHomeHeight.setText(Integer.toString(goHomeHeightInMeters));
                    }
                });
            }

            @Override
            public void onFailure(DJIError djiError) {
                Log.i(TAG, "getGoHomeHeightInMeters: " + djiError);
            }
        });
    }

    private void getMaxFlightHeight() {
        flightController.getMaxFlightHeight(new CommonCallbacks.CompletionCallbackWith<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                maxFlightHeight = integer;
                Log.i(TAG, "getMaxFlightHeight: " + maxFlightHeight);
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        editTextMaxHeight.setText(Integer.toString(maxFlightHeight));
                    }
                });
            }

            @Override
            public void onFailure(DJIError djiError) {
                Log.i(TAG, "getMaxFlightHeight: " + djiError.getDescription());
            }
        });
    }

    private void getMaxFlightRadiusLimitationEnabled() {
        flightController.getMaxFlightRadiusLimitationEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                maxFlightRadiusLimitationEnabled = aBoolean;
                Log.i(TAG, "getMaxFlightRadiusLimitationEnabled: " + maxFlightRadiusLimitationEnabled);
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switchMaxDistanceEnabled.setChecked(maxFlightRadiusLimitationEnabled);
                    }
                });
            }

            @Override
            public void onFailure(DJIError djiError) {
                Log.i(TAG, "getMaxFlightRadiusLimitationEnabled: " + djiError.getDescription());
            }
        });
    }

    private void getMaxFlightRadius() {
        flightController.getMaxFlightRadius(new CommonCallbacks.CompletionCallbackWith<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                maxFlightRadius = integer;
                Log.i(TAG, "getMaxFlightRadius: " + maxFlightRadius);
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        editTextMaxDistance.setText(Integer.toString(maxFlightRadius));
                    }
                });
            }

            @Override
            public void onFailure(DJIError djiError) {
                Log.i(TAG, "getMaxFlightRadius: " + djiError.getDescription());
            }
        });
    }

    private void setResultToToast(final String str) {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mainActivity, str, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
