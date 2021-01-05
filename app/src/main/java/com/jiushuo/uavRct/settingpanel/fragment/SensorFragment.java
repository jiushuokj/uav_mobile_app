package com.jiushuo.uavRct.settingpanel.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jiushuo.uavRct.DemoApplication;
import com.jiushuo.uavRct.MainActivity;
//import com.dji.mediaManagerDemo.R;
import com.jiushuo.uavRct.R;

import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;

public class SensorFragment extends Fragment {
    private static final String TAG = "感知参数设置";
    private View view;
    private Switch switchVisualObstacleAvoidanceSystem, switchShowRadar, switchTopInfraredSensingSystem, switchDownViewPositioningSystem, switchObstacleDetectionForReturnVoyage;
    private View radarWidget;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.tab2, container, false);
        initUI();
        return view;
    }

    private void initUI() {
        switchVisualObstacleAvoidanceSystem = view.findViewById(R.id.switch_visual_obstacle_avoidance_system);
        initSwitch1Value();
        addSwitch1Event();

        radarWidget = ((MainActivity)getActivity()).radarWidget;
        switchShowRadar = view.findViewById(R.id.switch_show_radar);
        switchShowRadar.setChecked(radarWidget.getVisibility() == View.VISIBLE);
        switchShowRadar.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        radarWidget.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                        Log.i(TAG, "switchShowRadar: " + radarWidget.getVisibility());
                    }
                });
            }
        });

        switchTopInfraredSensingSystem = view.findViewById(R.id.switch_top_infrared_sensing_system);
        initSwitch3Value();
        addSwitch3Event();

        switchDownViewPositioningSystem = view.findViewById(R.id.switch_down_view_positioning_system);
        initSwitch4Value();
        addSwitch4Event();

        switchObstacleDetectionForReturnVoyage = view.findViewById(R.id.switch_obstacle_detection_for_return_voyage);
        initSwitch5Value();
        addSwitch5Event();
    }

    private void addSwitch4Event() {
        switchDownViewPositioningSystem.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                DemoApplication.getAircraftInstance().getFlightController().getFlightAssistant().setVisionAssistedPositioningEnabled(isChecked, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            setResultToToast("4设置失败：" + djiError.getDescription());
                            Log.i(TAG, "setVisionAssistedPositioningEnabled 失败：" + djiError.getDescription());
                            /*getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    switchDownViewPositioningSystem.setChecked(!isChecked);
                                }
                            });*/
                        }
                    }
                });
            }
        });
    }

    private void initSwitch4Value() {
        DemoApplication.getAircraftInstance().getFlightController().getFlightAssistant().getVisionAssistedPositioningEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
            @Override
            public void onSuccess(final Boolean aBoolean) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switchDownViewPositioningSystem.setChecked(aBoolean);
                    }
                });
            }

            @Override
            public void onFailure(DJIError djiError) {
                setResultToToast("4获取失败：" + djiError.getDescription());
                Log.i(TAG, "init switchDownViewPositioningSystem onFailure: " + djiError.getDescription());
            }
        });
    }

    private void addSwitch1Event() {
        switchVisualObstacleAvoidanceSystem.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                DemoApplication.getAircraftInstance().getFlightController().getFlightAssistant().setCollisionAvoidanceEnabled(isChecked, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            setResultToToast("1设置失败：" + djiError.getDescription());
                            Log.i(TAG, "setCollisionAvoidanceEnabled 失败：" + djiError.getDescription());
                            /*getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    switchVisualObstacleAvoidanceSystem.setChecked(!isChecked);
                                }
                            });*/
                        }
//                        initSwitch1Value();
                    }
                });
            }
        });
    }

    private void initSwitch1Value() {
        DemoApplication.getAircraftInstance().getFlightController().getFlightAssistant().getCollisionAvoidanceEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
            @Override
            public void onSuccess(final Boolean aBoolean) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switchVisualObstacleAvoidanceSystem.setChecked(aBoolean);
                    }
                });
            }

            @Override
            public void onFailure(DJIError djiError) {
                setResultToToast("1获取失败：" + djiError.getDescription());
                Log.i(TAG, "init switchVisualObstacleAvoidanceSystem onFailure: " + djiError.getDescription());
            }
        });
    }

    private void addSwitch5Event() {
        switchObstacleDetectionForReturnVoyage.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                DemoApplication.getAircraftInstance().getFlightController().getFlightAssistant().setRTHObstacleAvoidanceEnabled(isChecked, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            setResultToToast("5设置失败：" + djiError.getDescription());
                            Log.i(TAG, "setRTHObstacleAvoidanceEnabled 失败：" + djiError.getDescription());
                            /*getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    switchObstacleDetectionForReturnVoyage.setChecked(!isChecked);
                                }
                            });*/
                        }
                    }
                });
            }
        });
    }

    private void initSwitch5Value() {
        DemoApplication.getAircraftInstance().getFlightController().getFlightAssistant().getRTHObstacleAvoidanceEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
            @Override
            public void onSuccess(final Boolean aBoolean) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switchObstacleDetectionForReturnVoyage.setChecked(aBoolean);
                    }
                });
            }

            @Override
            public void onFailure(DJIError djiError) {
                setResultToToast("5获取失败：" + djiError.getDescription());
                Log.i(TAG, "init getRTHObstacleAvoidanceEnabled onFailure: " + djiError.getDescription());
            }
        });
    }

    private void addSwitch3Event() {
        switchTopInfraredSensingSystem.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                DemoApplication.getAircraftInstance().getFlightController().getFlightAssistant().setUpwardVisionObstacleAvoidanceEnabled(isChecked, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            setResultToToast("3设置失败：" + djiError.getDescription());
                            Log.i(TAG, "setUpwardsAvoidanceEnabled 失败：" + djiError.getDescription());
                            /*getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    switchTopInfraredSensingSystem.setChecked(!isChecked);
                                }
                            });*/
                        }
                    }
                });
            }
        });
    }

    private void initSwitch3Value() {
        DemoApplication.getAircraftInstance().getFlightController().getFlightAssistant().getUpwardVisionObstacleAvoidanceEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
            @Override
            public void onSuccess(final Boolean aBoolean) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switchTopInfraredSensingSystem.setChecked(aBoolean);
                    }
                });
            }

            @Override
            public void onFailure(DJIError djiError) {
                setResultToToast("3获取失败：" + djiError.getDescription());
                Log.i(TAG, "init getUpwardsAvoidanceEnabled onFailure: " + djiError.getDescription());
            }
        });
    }

    private void setResultToToast(final String str) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), str, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
