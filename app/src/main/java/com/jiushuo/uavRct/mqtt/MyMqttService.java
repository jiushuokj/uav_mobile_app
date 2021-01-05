package com.jiushuo.uavRct.mqtt;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.jskj.mobile.request.ProtoWaypointMission;
import com.jskj.mobile.state.ProtoBattery;
import com.jskj.mobile.state.ProtoFlightController;
import com.jskj.mobile.state.ProtoGimbal;
import com.jskj.mobile.state.ProtoRTK;
import com.jiushuo.uavRct.DemoApplication;
import com.jiushuo.uavRct.entity.mqtt.AircraftInfoResponse;
import com.jiushuo.uavRct.entity.mqtt.CameraInfoResponse;
import com.jiushuo.uavRct.entity.mqtt.CameraModeRequest;
import com.jiushuo.uavRct.entity.mqtt.CameraModeResponse;
import com.jiushuo.uavRct.entity.mqtt.DefaultRequest;
import com.jiushuo.uavRct.entity.mqtt.DefaultResponse;
import com.jiushuo.uavRct.entity.mqtt.FlightControllerInfoResponse;
import com.jiushuo.uavRct.entity.mqtt.FlyToRequest;
import com.jiushuo.uavRct.entity.mqtt.GimbalControlRequest;
import com.jiushuo.uavRct.entity.mqtt.HotpointMissionRequest;
import com.jiushuo.uavRct.entity.mqtt.RequestUtil;
import com.jiushuo.uavRct.entity.mqtt.SetReturnHeightRequest;
import com.jiushuo.uavRct.entity.mqtt.SetReturnHeightResponse;
import com.jiushuo.uavRct.entity.mqtt.WaypointMissionRequest;
import com.jiushuo.uavRct.entity.mqtt.ZoomResponse;
import com.jiushuo.uavRct.utils.ToastUtils;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.battery.AggregationState;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.flightcontroller.Attitude;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.flightcontroller.RTKState;
import dji.common.flightcontroller.adsb.AirSenseSystemInformation;
import dji.common.gimbal.GimbalState;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.mission.hotpoint.HotpointHeading;
import dji.common.mission.hotpoint.HotpointMission;
import dji.common.mission.hotpoint.HotpointStartPoint;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointTurnMode;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.sdk.battery.Battery;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.flightcontroller.RTK;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.hotpoint.HotpointMissionOperator;
import dji.sdk.mission.timeline.TimelineElement;
import dji.sdk.mission.timeline.TimelineEvent;
import dji.sdk.mission.timeline.actions.GoToAction;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.thirdparty.io.reactivex.Observable;
import dji.thirdparty.io.reactivex.disposables.Disposable;
import dji.thirdparty.io.reactivex.functions.Consumer;
import dji.thirdparty.io.reactivex.schedulers.Schedulers;

import static com.jskj.mobile.request.ProtoWaypointMission.WaypointMission.WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;

/**
 * Author       wildma
 * Github       https://github.com/wildma
 * CreateDate   2018/11/08
 * Desc	        ${MQTT服务}
 */

public class MyMqttService extends Service {
    public static final String DEFAULT = "default";
    public static final int DELAY_MILLIS = 5000;
    private Aircraft aircraft;
    private String aircraftName;

    private FlightController flightController;
    private Integer maxFlightHeight;
    private Integer maxFlightRadius;
    private FlightControllerState flightControllerState;
    private RTK rtk;
    private RTKState rtkState;
    private Battery battery;
    private AggregationState aggregationState;
    private GimbalState gimbalState;
    private AirSenseSystemInformation airSenseSystemInformation;
    private String serialNumber;
    private Integer goHomeHeightInMeters;
    private SettingsDefinitions.CameraMode cameraMode;

//    private AtomicBoolean isStarted = new AtomicBoolean(false);
    private Disposable disposable;

    public static String publishTopic;//用来发布状态信息的主题
    public static String responseTopic;//用来发布响应消息的主题
    public static MqttAndroidClient mqttAndroidClient;
    public final String TAG = MyMqttService.class.getSimpleName();
    public static String host;//服务器地址（协议+地址+端口号）
    @SuppressLint("MissingPermission")
    public String CLIENTID = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? Build.getSerial() : Build.SERIAL;//客户端ID，一般以客户端唯一标识符表示，这里用设备序列号表示
    private MqttConnectOptions mMqttConnectOptions;

    private MissionControl missionControl;
    private FlyToRequest request;
    private WaypointMissionOperator waypointMissionOperator;
    private HotpointMissionOperator hotpointMissionOperator;

    private SettingsDefinitions.PhotoFileFormat photoFileFormat = SettingsDefinitions.PhotoFileFormat.JPEG;

    //MQTT是否连接成功的监听
    private IMqttActionListener iMqttActionListener = new IMqttActionListener() {

        @Override
        public void onSuccess(IMqttToken arg0) {
            Log.i(TAG, "Mqtt连接成功 ");
            ToastUtils.setResultToToast("Mqtt连接成功");
            try {
                String topic = "RCT/" + serialNumber + "/Req/#";
                mqttAndroidClient.subscribe(topic, 2);//订阅主题，参数：主题、服务质量
                sendStateData();
            } catch (MqttException e) {
                //isStarted.getAndSet(false);
                e.printStackTrace();
                Log.i(TAG, "发送无人机状态数据发送错误: " + e.getMessage());
            }
        }

        @Override
        public void onFailure(IMqttToken arg0, Throwable arg1) {
            arg1.printStackTrace();
            Log.i(TAG, "Mqtt连接失败");
            //没有可用网络的时候，延迟5秒再尝试重连
            ToastUtils.setResultToToast("Mqtt连接失败，请检查网络连接");
//            doConnectionDelay();
        }
    };
    private long wayPointMissionStartTime;
    private String waypointMissionId = DEFAULT;
    private String zoomResTopic;


    /**
     * 初始化需要获取的状态的无人机部件
     */
    private void initAircraftParams() {
        aircraft = DemoApplication.getAircraftInstance();
        if (aircraft == null) {
            Log.i(TAG, "initAircraftParams: 得不到aircraft实例");
            ToastUtils.setResultToToast("Mqtt获取无人机实例失败");
            return;
        }
        aircraft.getName(new CommonCallbacks.CompletionCallbackWith<String>() {
            @Override
            public void onSuccess(String s) {
                aircraftName = s;
            }

            @Override
            public void onFailure(DJIError djiError) {
                Log.e(TAG, "aircraft.getName: " + djiError);
            }
        });
        flightController = aircraft.getFlightController();
        flightController.getMaxFlightHeight(new CommonCallbacks.CompletionCallbackWith<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                maxFlightHeight = integer;
            }

            @Override
            public void onFailure(DJIError djiError) {
                Log.e(TAG, "flightController.getMaxFlightHeight: " + djiError);
            }
        });
        flightController.getMaxFlightRadius(new CommonCallbacks.CompletionCallbackWith<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                maxFlightRadius = integer;
            }

            @Override
            public void onFailure(DJIError djiError) {
                Log.e(TAG, "flightController.getMaxFlightRadius: " + djiError);
            }
        });
        rtk = flightController.getRTK();
        if (rtk != null) {
            rtk.setStateCallback(new RTKState.Callback() {
                @Override
                public void onUpdate(RTKState state) {
                    rtkState = state;
                }
            });
        }
        flightControllerState = flightController.getState();
        flightController.setASBInformationCallback(new AirSenseSystemInformation.Callback() {
            @Override
            public void onUpdate(AirSenseSystemInformation info) {
                airSenseSystemInformation = info;
            }
        });
        flightController.getSerialNumber(new CommonCallbacks.CompletionCallbackWith<String>() {
            @Override
            public void onSuccess(String s) {
                serialNumber = s;
                Log.i(TAG, "获取序列号成功: " + serialNumber);

                //初始化发布主题和响应主题
                initPublishAndResponseTopic();

                //初始化构建MqttClient的参数
                initMqttClientParams();
            }

            @Override
            public void onFailure(DJIError djiError) {
                Log.e(TAG, "onFailure: " + djiError.getDescription());
            }
        });
        battery = aircraft.getBattery();
        battery.setAggregationStateCallback(new AggregationState.Callback() {
            @Override
            public void onUpdate(AggregationState state) {
                aggregationState = state;
            }
        });

        if (aircraft.getGimbals().size() > 0) {
            aircraft.getGimbals().get(0).setStateCallback(new GimbalState.Callback() {
                @Override
                public void onUpdate(GimbalState state) {
                    gimbalState = state;
                    float currentPitch = state.getAttitudeInDegrees().getPitch();
                    if (gimbalStateCallback != null) {
                        gimbalStateCallback.onGimbalPitchReceived(currentPitch);
                    }
                }
            });
        }

        flightController.getGoHomeHeightInMeters(new CommonCallbacks.CompletionCallbackWith<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                goHomeHeightInMeters = integer;
            }

            @Override
            public void onFailure(DJIError djiError) {
                Log.i(TAG, "onFailure: " + djiError == null ? "success" : djiError.getDescription());
            }
        });

        aircraft.getCameras().get(0).getMode(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.CameraMode>() {
            @Override
            public void onSuccess(SettingsDefinitions.CameraMode mode) {
                cameraMode = mode;
            }

            @Override
            public void onFailure(DJIError djiError) {

            }
        });

        aircraft.getCameras().get(0).getPhotoFileFormat(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.PhotoFileFormat>() {
            @Override
            public void onSuccess(SettingsDefinitions.PhotoFileFormat photoFileFormat1) {
                photoFileFormat = photoFileFormat1;
            }

            @Override
            public void onFailure(DJIError djiError) {

            }
        });
    }

    /**
     * 发送无人机状态数据
     */
    private void sendStateData() {
        if (mqttAndroidClient.isConnected()) {

            ToastUtils.setResultToToast("开始发送Mqtt消息");
            if (disposable != null && !disposable.isDisposed()) {
                return;
            }
            if (aircraft == null) {
                Log.i(TAG, "sendStateData: aircraft实例为空");
                return;
            }
            disposable = Observable.interval(100, TimeUnit.MILLISECONDS).observeOn(Schedulers.newThread())
                    .subscribe(new Consumer<Long>() {
                        @Override
                        public void accept(Long aLong) throws MqttException {
                            if (mqttAndroidClient.isConnected()) {
                                if (serialNumber != null && flightController.isConnected()) {
//                                    Log.d(TAG, "发送第" + (aLong++) + "次Mqtt消息");
                                    LocationCoordinate3D aircraftLocation = flightControllerState.getAircraftLocation();
                                    Attitude flightControllerStateAttitude = flightControllerState.getAttitude();
                                    ProtoFlightController.FlightController.Builder builder = ProtoFlightController.FlightController.newBuilder()
                                            .setAreMotorsOn(flightControllerState.areMotorsOn())
                                            .setIsFlying(flightControllerState.isFlying())
                                            .setLatitude(aircraftLocation.getLatitude())
                                            .setLongitude(aircraftLocation.getLongitude())
                                            .setAltitude(aircraftLocation.getAltitude())
                                            .setTakeoffLocationAltitude(flightControllerState.getTakeoffLocationAltitude())
                                            .setPitch(flightControllerStateAttitude.pitch)
                                            .setRoll(flightControllerStateAttitude.roll)
                                            .setYaw(flightControllerStateAttitude.yaw)
                                            .setVelocityX(flightControllerState.getVelocityX())
                                            .setVelocityY(flightControllerState.getVelocityY())
                                            .setVelocityZ(flightControllerState.getVelocityZ())
                                            .setFlightTimeInSeconds(flightControllerState.getFlightTimeInSeconds())
                                            .setFlightMode(ProtoFlightController.FlightController.FlightMode.values()[flightControllerState.getFlightMode().ordinal()])
                                            .setGPSSatelliteCount(flightControllerState.getSatelliteCount())
                                            .setGPSgSignalLevel(ProtoFlightController.FlightController.GPSSignalLevel.values()[flightControllerState.getGPSSignalLevel().ordinal()])
                                            .setFlightWindWarning(ProtoFlightController.FlightController.FlightWindWarning.values()[flightControllerState.getFlightWindWarning().ordinal()])
                                            .setRemainingFlightTime(flightControllerState.getGoHomeAssessment().getRemainingFlightTime())
                                            .setRemainingFlightTime(flightControllerState.getGoHomeAssessment().getTimeNeededToGoHome())
                                            .setAirSenseWarningLevel(ProtoFlightController.FlightController.AirSenseWarningLevel.values()[airSenseSystemInformation.getWarningLevel().ordinal()]);
                                    if (rtkState != null) {
                                        builder.setMobileStationAltitude(rtkState.getMobileStationAltitude())
                                                .setFusionMobileStationAltitude(rtkState.getFusionMobileStationAltitude())
                                                .setBaseStationAltitude(rtkState.getBaseStationAltitude());
                                    }
                                    ProtoFlightController.FlightController flightInformation = builder.build();
                                    String topic = publishTopic + "/FlightController";
                                    byte[] tosendbytes = flightInformation.toByteArray();
                                    MqttMessage flightInformationMessage = new MqttMessage(tosendbytes);
                                    flightInformationMessage.setQos(1);
                                     if (mqttAndroidClient.isConnected()){
                                        mqttAndroidClient.publish(topic, flightInformationMessage);
//                                         Log.d(TAG,"ltz send out flightInformationMessage "+ tosendbytes.length);
                                     }else{
//                                         Log.d(TAG,"ltz not send out flightInformationMessage");
                                     }

                                 }
                                if (aggregationState != null) {
                                    ProtoBattery.Battery batteryInfo = ProtoBattery.Battery.newBuilder()
                                            .setChargeRemaining(aggregationState.getChargeRemaining())
                                            .setChargeRemainingInPercent(aggregationState.getChargeRemainingInPercent())
                                            .setVoltage(aggregationState.getVoltage())
                                            .setCurrent(aggregationState.getCurrent())
                                            .build();
                                    String topic = publishTopic + "/Battery";
                                    MqttMessage batteryInfoMessage = new MqttMessage(batteryInfo.toByteArray());
                                    batteryInfoMessage.setQos(1);
                                    if (mqttAndroidClient.isConnected())
                                        mqttAndroidClient.publish(topic, batteryInfoMessage);
                                }
                                if (gimbalState != null) {
                                    dji.common.gimbal.Attitude gimbalAttitude = gimbalState.getAttitudeInDegrees();

                                    MqttMessage gimbalInfoMessage = new MqttMessage(ProtoGimbal.Gimbal.newBuilder()
                                            .setGimbalMode(ProtoGimbal.Gimbal.GimbalMode.values()[gimbalState.getMode().ordinal()])
                                            .setPitch(gimbalAttitude.getPitch())
                                            .setRoll(gimbalAttitude.getRoll())
                                            .setYaw(gimbalAttitude.getYaw())
                                            .build()
                                            .toByteArray());
                                    gimbalInfoMessage.setQos(1);
                                    String topic = publishTopic + "/Gimbal";
                                    if (mqttAndroidClient.isConnected())
                                        mqttAndroidClient.publish(topic, gimbalInfoMessage);
                                }
                                if (rtkState != null) {
                                    MqttMessage rtkInfoMessage = new MqttMessage(ProtoRTK.RTK.newBuilder()
                                            .setIsConnected(rtk.isConnected())
                                            .setIsRTKBeingUsed(rtkState.isRTKBeingUsed())
                                            .setHeadingSolution(ProtoRTK.RTK.HeadingSolution.values()[rtkState.getHeadingSolution().ordinal()])
                                            .build()
                                            .toByteArray());
                                    rtkInfoMessage.setQos(1);
                                    String topic = publishTopic + "/RTK";
                                    if (mqttAndroidClient.isConnected())
                                        mqttAndroidClient.publish(topic, rtkInfoMessage);
                                }
                            } else {
                                Log.i(TAG, "发送State数据: mqttAndroidClient is disconnected");
                            }
                        }
                    });
            //isStarted.getAndSet(true);
        }
    }

    /**
     * 订阅主题的回调
     */
    private final MqttCallback mqttCallback = new MqttCallbackExtended() {

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Log.i(TAG, "messageArrived: " + topic);
            Log.i(TAG, "messageArrived: message" + new String(message.getPayload()));
            String requestTopic = "RCT/" + serialNumber + "/Req/";
            String type = topic.replace(requestTopic, "");

            if (type.equals("WaypointMission")) {
                //pb版本，已废弃
                loadWaypointMissionUsingProtoBuf(topic, message);
            } else if (type.equals(RequestUtil.AIRCRAFT)) {
                makeAircraftInfoResponse(message);
            } else if (type.equals(RequestUtil.FLIGHT_CONTROLLER)) {
                makeFlightControllerInfoResponse(message);
            } else if (type.equals(RequestUtil.CAMERA)) {
                //未完成
                makeCameraInfoResponse(message);
            } else if (type.equals(RequestUtil.LOCK)) {
                lockMotors(message);
            } else if (type.equals(RequestUtil.UNLOCK)) {
                unLockMotors(message);
            } else if (type.equals(RequestUtil.SET_RETURN_HEIGHT)) {
                setReturnHeight(message);
            } else if (type.equals(RequestUtil.TAKE_OFF)) {
                takeOff(message);
            } else if (type.equals(RequestUtil.LAND)) {
                land(message);
            } else if (type.equals(RequestUtil.CANCEL_LANDING)) {
                cancelLanding(message);
            } else if (type.equals(RequestUtil.CONFIRM_LANDING)) {
                confirmLanding(message);
            } else if (type.equals(RequestUtil.HOVER)) {
                //未实现
                hover(message);
            } else if (type.equals(RequestUtil.FLY_TO)) {
                //未实现，可以用TimeLineMission中的GoToAction实现
                flyTo(message);
            } else if (type.equals(RequestUtil.RETURN_HOME)) {
                returnHome(message);
            } else if (type.equals(RequestUtil.GIMBAL_CONTROL)) {
                //使用第一个相机的万向节
                gimbalControl(message);
            } else if (type.equals(RequestUtil.CAMERA_SETTING)) {
                //未完成
                cameraSetting(message);
            } else if (type.equals(RequestUtil.CAMERA_MODE)) {
                //返回值会有延迟
                cameraMode(message);
            } else if (type.equals(RequestUtil.TAKE_PHOTO)) {
                //都使用第一个相机
                takePhoto(message);
            } else if (type.equals(RequestUtil.START_RECORD_VIDEO)) {
                startRecordVideo(message);
            } else if (type.equals(RequestUtil.STOP_RECORD_VIDEO)) {
                stopRecordVideo(message);
            } else if (type.equals(RequestUtil.ZOOM_IN)) {
                zoomIn(message);
            } else if (type.equals(RequestUtil.ZOOM_OUT)) {
                zoomOut(message);
            } else if (type.equals(RequestUtil.ZOOM_RESET)) {
                zoomReset(message);
            } else if (type.equals(RequestUtil.WAYPOINT_LOAD)) {
                loadWaypointMission(message);
            } else if (type.equals(RequestUtil.WAYPOINT_UPLOAD)) {
                uploadWaypoint(message);
            } else if (type.equals(RequestUtil.WAYPOINT_START)) {
                startWaypoint(message);
            } else if (type.equals(RequestUtil.WAYPOINT_PAUSE)) {
                pauseWaypoint(message);
            } else if (type.equals(RequestUtil.WAYPOINT_RESUME)) {
                resumeWaypoint(message);
            } else if (type.equals(RequestUtil.WAYPOINT_STOP)) {
                stopWaypoint(message);
            } else if (type.equals(RequestUtil.HOTPOINT_START)) {
                startHotpoint(message);
            } else if (type.equals(RequestUtil.HOTPOINT_PAUSE)) {
                pauseHotpoint(message);
            } else if (type.equals(RequestUtil.HOTPOINT_RESUME)) {
                resumeHotpoint(message);
            } else if (type.equals(RequestUtil.HOTPOINT_STOP)) {
                stopHotpoint(message);
            } else if (type.equals(RequestUtil.HOTPOINT_STOP)) {
                stopHotpoint(message);
            } else {
                Log.i(TAG, "messageArrived: else --->" + topic);
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {

        }

        @Override
        public void connectionLost(Throwable arg0) {
            Log.i(TAG, "connectionLost: mqtt连接断开 ");
            ToastUtils.setResultToToast("mqtt连接断开");
        }
        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            try {
                if (reconnect) {//重新订阅
                    ToastUtils.setResultToToast("mqtt自动重连成功");
                    String topic = "RCT/" + serialNumber + "/Req/#";
                    mqttAndroidClient.subscribe(topic, 2);//订阅主题，参数：主题、服务质量
                }
            } catch (Exception e) {
                Log.d(TAG,"重新订阅失败");
            }
        }
    };

    private void zoomReset(MqttMessage message) {
        getZoomResTopic(message);
        if (zoomCallback != null) {
            zoomCallback.onZoomResetReceived();
        }
    }

    private void zoomIn(MqttMessage message) {
        getZoomResTopic(message);

        if (zoomCallback != null) {
            zoomCallback.onZoomInReceived();
        }
    }

    private void getZoomResTopic(MqttMessage message) {
        DefaultRequest request = JSON.parseObject(new String(message.getPayload()), DefaultRequest.class);
        zoomResTopic = request.getResTopic();
    }

    private void zoomOut(MqttMessage message) {
        getZoomResTopic(message);
        if (zoomCallback != null) {
            zoomCallback.onZoomOutReceived();
        }
    }

    private void loadWaypointMissionUsingProtoBuf(String topic, MqttMessage message) throws com.google.protobuf.InvalidProtocolBufferException {
        ProtoWaypointMission.WaypointMission protoMission = ProtoWaypointMission.WaypointMission.parseFrom(message.getPayload());
        Log.i(TAG, "收到消息： " + protoMission);
        //收到消息，这里弹出Toast表示。如果需要更新UI，可以使用广播或者EventBus进行发送
        Toast.makeText(getApplicationContext(), "messageArrived: " + protoMission, Toast.LENGTH_LONG).show();

        //处理收到的WaypointMission
        WaypointMission waypointMission = handleWaypointMission(protoMission);
        Log.i(TAG, "转换后的消息： " + waypointMission);
        if (waypointMissionOperator == null) {
            waypointMissionOperator = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        //装载任务
        DJIError error = waypointMissionOperator.loadMission(waypointMission);
        if (error == null) {
            Log.i(TAG, "messageArrived: " + "loadWaypointMission succeeded");
            response(topic, "loadWaypointMission succeeded");
        } else {
            Log.i(TAG, "messageArrived: " + "loadWaypointMission failed " + error.getDescription());
            response(topic, "loadWaypointMission failed " + error.getDescription());
        }
    }

    private void stopHotpoint(final MqttMessage message) {
        Log.i(TAG, "stopHotpoint: ");
        if (hotpointMissionOperator == null) {
            hotpointMissionOperator = DJISDKManager.getInstance().getMissionControl().getHotpointMissionOperator();
        }
        hotpointMissionOperator.stop(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                makeDefaultResponse(djiError, message);
            }
        });
    }

    private void resumeHotpoint(final MqttMessage message) {
        Log.i(TAG, "resumeHotpoint: ");
        if (hotpointMissionOperator == null) {
            hotpointMissionOperator = DJISDKManager.getInstance().getMissionControl().getHotpointMissionOperator();
        }
        hotpointMissionOperator.resume(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                makeDefaultResponse(djiError, message);
            }
        });
    }

    private void pauseHotpoint(final MqttMessage message) {
        Log.i(TAG, "pauseHotpoint: ");
        if (hotpointMissionOperator == null) {
            hotpointMissionOperator = DJISDKManager.getInstance().getMissionControl().getHotpointMissionOperator();
        }
        hotpointMissionOperator.pause(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                makeDefaultResponse(djiError, message);
            }
        });
    }

    private void startHotpoint(MqttMessage message) {
        Log.i(TAG, "startHotpoint: ");
        ToastUtils.showToast("messageArrived: \n" + new String(message.getPayload()));
        final HotpointMissionRequest request = JSON.parseObject(new String(message.getPayload()), HotpointMissionRequest.class);
        HotpointMission hotpointMission = handleHotpointMission(request);
        Log.i(TAG, "startHotpoint: " + JSON.toJSONString(hotpointMission));
        if (hotpointMissionOperator == null) {
            hotpointMissionOperator = DJISDKManager.getInstance().getMissionControl().getHotpointMissionOperator();
        }
        hotpointMissionOperator.startMission(hotpointMission, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                makeDefaultResponse(djiError, request.getResTopic());
            }
        });
    }

    private HotpointMission handleHotpointMission(HotpointMissionRequest mission) {
        HotpointMission hotpointMission = new HotpointMission();
        hotpointMission.setHotpoint(new LocationCoordinate2D(mission.getLatitude(), mission.getLongitude()));
        hotpointMission.setStartPoint(HotpointStartPoint.values()[mission.getStartPoint().ordinal()]);
        hotpointMission.setHeading(HotpointHeading.values()[mission.getHeading().ordinal()]);
        hotpointMission.setAltitude(mission.getAltitude());
        hotpointMission.setRadius(mission.getRadius());
        hotpointMission.setAngularVelocity(mission.getAngularVelocity());
        hotpointMission.setClockwise(mission.isClockwise());
        return hotpointMission;
    }

    private void cancelLanding(final MqttMessage message) {
        Log.i(TAG, "cancelLanding: ");
        flightController.cancelLanding(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                makeDefaultResponse(djiError, message);
            }
        });
    }

    private void confirmLanding(final MqttMessage message) {
        Log.i(TAG, "confirmLanding: ");
        flightController.confirmLanding(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                makeDefaultResponse(djiError, message);
            }
        });
    }

    private void stopWaypoint(final MqttMessage message) {
        Log.i(TAG, "stopWaypoint: ");
        ToastUtils.showToast("接收到：停止航点任务指令！");
        if (waypointMissionOperator == null) {
            waypointMissionOperator = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        waypointMissionOperator.stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                makeDefaultResponse(djiError, message);
            }
        });
    }

    private void resumeWaypoint(final MqttMessage message) {
        Log.i(TAG, "resumeWaypoint: ");
        ToastUtils.showToast("接收到：恢复航点任务指令！");
        if (waypointMissionOperator == null) {
            waypointMissionOperator = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        waypointMissionOperator.resumeMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                makeDefaultResponse(djiError, message);
            }
        });
    }

    private void pauseWaypoint(final MqttMessage message) {
        Log.i(TAG, "pauseWaypoint: ");
        ToastUtils.showToast("接收到：暂停航点任务指令！");
        if (waypointMissionOperator == null) {
            waypointMissionOperator = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        waypointMissionOperator.pauseMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                makeDefaultResponse(djiError, message);
            }
        });
    }

    private void startWaypoint(final MqttMessage message) {
        Log.i(TAG, "startWaypoint: ");
        ToastUtils.setResultToToast("接收到：开始航点任务指令！");
        if (waypointMissionOperator == null) {
            waypointMissionOperator = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        waypointMissionOperator.startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                makeDefaultResponse(djiError, message);
                if (djiError == null) {
                    wayPointMissionStartTime = System.currentTimeMillis();
                    storeLatestWaypointMissionIdAndStartTime(waypointMissionId, wayPointMissionStartTime);
                }
                ToastUtils.setResultToToast(djiError == null ? "航点任务开始成功" : "航点任务开始失败" + djiError.getDescription());
            }
        });
    }

    /**
     * 存储最近一次航点任务的Id和开始时间
     *
     * @param id
     * @param time
     */
    private void storeLatestWaypointMissionIdAndStartTime(String id, long time) {
        SharedPreferences.Editor editor = getSharedPreferences("waypoint",
                MODE_PRIVATE).edit();
        editor.putString("id", id);
        editor.putLong("time", time);
        editor.apply();
        Log.i(TAG, "存储最近一次航点任务的Id和开始时间: " + id + "----" + time);
    }

    private void uploadWaypoint(final MqttMessage message) {
        Log.i(TAG, "uploadWaypoint: ");
        ToastUtils.showToast("接收到：上传航点任务指令！");
        if (waypointMissionOperator == null) {
            waypointMissionOperator = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        waypointMissionOperator.uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                makeDefaultResponse(djiError, message);
                ToastUtils.setResultToToast(djiError == null ? "航点任务上传成功" : "航点任务上传失败" + djiError.getDescription());
            }
        });
    }

    private void loadWaypointMission(MqttMessage message) {
        Log.i(TAG, "收到航点任务: ");
        //收到消息，这里弹出Toast表示。如果需要更新UI，可以使用广播或者EventBus进行发送
        WaypointMissionRequest request = JSON.parseObject(new String(message.getPayload()), WaypointMissionRequest.class);
        waypointMissionId = request.getId();
        ToastUtils.showToast("收到航点任务: " + request.getMissionName());
        Log.i(TAG, "收到航点任务: " + request.getMissionName());
        //处理收到的WaypointMission
        WaypointMission waypointMission = handleWaypointMission(request);
        Log.i(TAG, "转换后的消息： " + JSON.toJSONString(waypointMission));
        if (waypointMissionOperator == null) {
            waypointMissionOperator = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        //装载任务
        DJIError error = waypointMissionOperator.loadMission(waypointMission);
        makeDefaultResponse(error, request.getResTopic());
    }

    private void cameraMode(final MqttMessage message) {
        final CameraModeRequest request = JSON.parseObject(new String(message.getPayload()), CameraModeRequest.class);
        aircraft.getCameras().get(0).setMode(SettingsDefinitions.CameraMode.values()[request.getCameraMode()], new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                String responseTopicName = request.getResTopic();
                if (djiError != null) {
                    Log.i(TAG, "onResult: " + responseTopicName + djiError.getDescription());
                    if (responseTopicName != null) {
                        CameraModeResponse response = new CameraModeResponse();
                        response.setCode(djiError.getErrorCode());
                        response.setCameraMode(cameraMode.ordinal());
                        response(responseTopicName, JSON.toJSONString(response, SerializerFeature.WriteMapNullValue));
                    }
                } else {
                    Log.i(TAG, "onResult:" + responseTopicName + " success!");
                    if (responseTopicName != null) {
                        CameraModeResponse response = new CameraModeResponse();
                        response.setCode(RequestUtil.CODE_SUCCESS);
                        response.setCameraMode(cameraMode.ordinal());
                        response(responseTopicName, JSON.toJSONString(response, SerializerFeature.WriteMapNullValue));
                    }
                }
            }
        });
    }

    private void stopRecordVideo(final MqttMessage message) {
        Log.i(TAG, "stopRecordVideo: ");
        aircraft.getCameras().get(0).stopRecordVideo(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                makeDefaultResponse(djiError, message);
            }
        });
    }

    private void startRecordVideo(final MqttMessage message) {
        Log.i(TAG, "startRecordVideo: ");
        aircraft.getCameras().get(0).startRecordVideo(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                makeDefaultResponse(djiError, message);
            }
        });
    }

    private void takePhoto(final MqttMessage message) {
        Log.i(TAG, "takePhoto: ");
        aircraft.getCameras().get(0).startShootPhoto(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                makeDefaultResponse(djiError, message);
            }
        });
    }

    private void cameraSetting(MqttMessage message) {
        Log.i(TAG, "cameraSetting: ");
    }

    private void gimbalControl(final MqttMessage message) {
        Log.i(TAG, "gimbalControl: ");
        GimbalControlRequest request = JSON.parseObject(new String(message.getPayload()), GimbalControlRequest.class);
        Rotation rotation = new Rotation.Builder()
                .mode(RotationMode.values()[request.getMode().ordinal()])
                .pitch(request.getPitch())
                .roll(request.getRoll())
                .yaw(request.getYaw())
                .time(request.getTime())
                .build();
        for (Gimbal gimbal : aircraft.getGimbals()) {
            Log.i(TAG, "gimbalControl: gimbals--->" + gimbal);
        }
        aircraft.getGimbals().get(0).rotate(rotation, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                makeDefaultResponse(djiError, message);
            }
        });
    }

    private void makeCameraInfoResponse(MqttMessage message) {
        Log.i(TAG, "makeCameraInfoResponse: ");
        DefaultRequest request = JSON.parseObject(new String(message.getPayload()), DefaultRequest.class);
        String responseTopicName = request.getResTopic();
        if (responseTopicName != null) {
            CameraInfoResponse response = new CameraInfoResponse();
            response.setCode(RequestUtil.CODE_SUCCESS);
            response(responseTopicName, JSON.toJSONString(response, SerializerFeature.WriteMapNullValue));
        }
    }

    private void makeFlightControllerInfoResponse(MqttMessage message) {
        Log.i(TAG, "makeFlightControllerInfoResponse: ");
        DefaultRequest request = JSON.parseObject(new String(message.getPayload()), DefaultRequest.class);
        String responseTopicName = request.getResTopic();
        if (responseTopicName != null) {
            FlightControllerInfoResponse response = new FlightControllerInfoResponse();
            response.setHomeLocationSet(flightControllerState.isHomeLocationSet());
            response.setHomeLocationLatitude(flightControllerState.getHomeLocation().getLatitude());
            response.setHomeLocationLongitude(flightControllerState.getHomeLocation().getLongitude());
            response.setMaxFlightHeight(maxFlightHeight);
            response.setMaxFlightRadius(maxFlightRadius);
            response.setCode(RequestUtil.CODE_SUCCESS);
            response(responseTopicName, JSON.toJSONString(response, SerializerFeature.WriteMapNullValue));
        }
    }

    private void makeAircraftInfoResponse(MqttMessage message) {
        Log.i(TAG, "makeAircraftInfoResponse: ");
        DefaultRequest request = JSON.parseObject(new String(message.getPayload()), DefaultRequest.class);
        String responseTopicName = request.getResTopic();
        if (responseTopicName != null) {
            AircraftInfoResponse response = new AircraftInfoResponse();
            response.setName(aircraftName);
            response.setFirmwarePackageVersion(aircraft.getFirmwarePackageVersion());
            response.setCode(RequestUtil.CODE_SUCCESS);
            response(responseTopicName, JSON.toJSONString(response, SerializerFeature.WriteMapNullValue));
        }
    }

    private void returnHome(final MqttMessage message) {
        Log.i(TAG, "returnHome: ");
        flightController.startGoHome(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                makeDefaultResponse(djiError, message);
            }
        });
    }

    private void flyTo(final MqttMessage message) {
        Log.i(TAG, "flyTo: ");
        request = JSON.parseObject(new String(message.getPayload()), FlyToRequest.class);
        missionControl = MissionControl.getInstance();
        final MissionControl.Listener listener = new MissionControl.Listener() {
            @Override
            public void onEvent(TimelineElement timelineElement, TimelineEvent timelineEvent, DJIError djiError) {
                Log.i(TAG, "onEvent: timelineElement: " + timelineElement + " TimelineEvent." + timelineEvent.name());
                if (timelineElement == null && timelineEvent == TimelineEvent.STARTED) {
                    makeDefaultResponse(RequestUtil.CODE_FLY_TO_STARTED, request.getResTopic());
                }
                if (timelineElement instanceof GoToAction && timelineEvent == TimelineEvent.PROGRESSED) {
                    makeDefaultResponse(RequestUtil.CODE_FLY_TO_PROCESSED, request.getResTopic());
                }
                if (timelineElement == null && timelineEvent == TimelineEvent.FINISHED) {
                    makeDefaultResponse(RequestUtil.CODE_FLY_TO_FINISHED, request.getResTopic());
                }
                if (timelineElement == null && timelineEvent == TimelineEvent.STOPPED) {
                    makeDefaultResponse(RequestUtil.CODE_FLY_TO_STOPPED, request.getResTopic());
                }
                if (timelineEvent == TimelineEvent.START_ERROR) {
                    makeDefaultResponse(RequestUtil.CODE_FLY_TO_START_ERROR, request.getResTopic());
                }
                if (timelineEvent == TimelineEvent.STOP_ERROR) {
                    makeDefaultResponse(RequestUtil.CODE_FLY_TO_STOP_ERROR, request.getResTopic());
                }
            }
        };

        //判断飞机是否在空中
        if (!flightControllerState.areMotorsOn() && !flightControllerState.isFlying()) {
            Log.i(TAG, "flyTo: " + "不在空中");
            ToastUtils.showToast("请先起飞");
            makeDefaultResponse(RequestUtil.CODE_FAIL, request.getResTopic());
        } else {
            Log.i(TAG, "flyTo: 在空中" + " isTimelineRunning:" + missionControl.isTimelineRunning());
            if (missionControl.isTimelineRunning()) {
                missionControl.stopTimeline();
                Log.i(TAG, "stopTimeline: ");
                makeDefaultResponse(RequestUtil.CODE_FLY_TO_RUNNING, request.getResTopic());
            } else {
                startFlyToMission(listener);
            }
        }
    }

    private void startFlyToMission(MissionControl.Listener listener) {
        if (missionControl.scheduledCount() > 0) {
            missionControl.unscheduleEverything();
            missionControl.removeAllListeners();
        }
        GoToAction goToAction = new GoToAction(new LocationCoordinate2D(request.getLatitude(), request.getLongitude()));
        Log.i(TAG, "flyTo: " + goToAction);
        missionControl.scheduleElement(goToAction);
        missionControl.addListener(listener);
        missionControl.startTimeline();
        Log.i(TAG, "flyTo: missionControl.startTimeline();");
    }

    private void makeDefaultResponse(Integer code, String responseTopic) {
        DefaultResponse response = new DefaultResponse();
        response.setCode(code);
        response(responseTopic, JSON.toJSONString(response, SerializerFeature.WriteMapNullValue));
    }

    private void hover(MqttMessage message) {
    }

    private void land(final MqttMessage message) {
        Log.i(TAG, "land: ");
        flightController.startLanding(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                makeDefaultResponse(djiError, message);
            }
        });
    }

    private void takeOff(final MqttMessage message) {
        Log.i(TAG, "takeOff: ");
        flightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                makeDefaultResponse(djiError, message);
            }
        });
    }

    private void setReturnHeight(final MqttMessage message) {
        Log.i(TAG, "setReturnHeight: " + new String(message.getPayload()));
        final SetReturnHeightRequest request = JSON.parseObject(new String(message.getPayload()), SetReturnHeightRequest.class);
        flightController.setGoHomeHeightInMeters(request.getHeight(), new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                String responseTopicName = request.getResTopic();
                if (djiError != null) {
                    Log.i(TAG, "onResult: " + responseTopicName + djiError.getDescription());
                    if (responseTopicName != null) {
                        SetReturnHeightResponse response = new SetReturnHeightResponse();
                        response.setCode(djiError.getErrorCode());
                        response.setHeight(goHomeHeightInMeters);
                        response(responseTopicName, JSON.toJSONString(response, SerializerFeature.WriteMapNullValue));
                    }
                } else {
                    Log.i(TAG, "onResult:" + responseTopicName + " success!");
                    if (responseTopicName != null) {
                        SetReturnHeightResponse response = new SetReturnHeightResponse();
                        response.setCode(RequestUtil.CODE_SUCCESS);
                        response.setHeight(goHomeHeightInMeters);
                        response(responseTopicName, JSON.toJSONString(response, SerializerFeature.WriteMapNullValue));
                    }
                }
            }
        });
    }

    /**
     * 将收到的航点任务的pb数据转换为大疆的航点任务数据（现在不用了）
     *
     * @param protoMission
     * @return
     */
    private dji.common.mission.waypoint.WaypointMission handleWaypointMission(ProtoWaypointMission.WaypointMission protoMission) {
        dji.common.mission.waypoint.WaypointMission.Builder builder = new dji.common.mission.waypoint.WaypointMission.Builder()
                .autoFlightSpeed(protoMission.getAutoFlightSpeed())
                .maxFlightSpeed(protoMission.getMaxFlightSpeed())
                .headingMode(WaypointMissionHeadingMode.values()[protoMission.getHeadingMode().ordinal()])
                .setGimbalPitchRotationEnabled(protoMission.getGimbalPitchRotationEnabled())
                .finishedAction(WaypointMissionFinishedAction.values()[protoMission.getFinishedAction().ordinal()]);

        List<Waypoint> waypointList = new ArrayList<>();
        //解析航点
        for (ProtoWaypointMission.WaypointMission.Waypoint protoWaypoint : protoMission.getWaypointsList()) {
            Waypoint waypoint = new Waypoint(protoWaypoint.getLatitude(),
                    protoWaypoint.getLongitude(),
                    //判断是否使用任务高度作为航点高度
                    protoWaypoint.getUsingMissionAltitude() ? protoMission.getMissionAltitude() : protoWaypoint.getAltitude());
            //如果“飞行器偏航角”选择“依照每个航点设置”
            if (protoMission.getHeadingMode().equals(USING_WAYPOINT_HEADING)) {
                waypoint.heading = protoWaypoint.getHeading();
                waypoint.turnMode = WaypointTurnMode.values()[protoWaypoint.getTurnMode().ordinal()];
            }

            if (protoMission.getGimbalPitchRotationEnabled()) {
                waypoint.gimbalPitch = protoWaypoint.getGimbalPitch();
            }
            //解析航点动作
            for (ProtoWaypointMission.WaypointMission.WaypointAction protoAction : protoWaypoint.getActionsList()) {
                waypoint.addAction(new WaypointAction(
                        WaypointActionType.values()[protoAction.getActionType().ordinal()]
                        , protoAction.getActionParam()));
            }
            waypointList.add(waypoint);
        }

        builder.waypointList(waypointList)
                .waypointCount(waypointList.size());
        return builder.build();
    }

    /**
     * 将收到的航点任务的pb数据转换为大疆的航点任务数据
     *
     * @param waypointMissionRequest
     * @return
     */
    private WaypointMission handleWaypointMission(WaypointMissionRequest waypointMissionRequest) {
        WaypointMission.Builder builder = new dji.common.mission.waypoint.WaypointMission.Builder()
                .autoFlightSpeed(waypointMissionRequest.getAutoFlightSpeed())
                .maxFlightSpeed(waypointMissionRequest.getMaxFlightSpeed())
                .headingMode(WaypointMissionHeadingMode.values()[waypointMissionRequest.getHeadingMode().ordinal()])
                .setGimbalPitchRotationEnabled(waypointMissionRequest.isGimbalPitchRotationEnabled())
                .finishedAction(WaypointMissionFinishedAction.values()[waypointMissionRequest.getFinishedAction().ordinal()]);

        List<Waypoint> waypointList = new ArrayList<>();
        //解析航点
        for (com.jiushuo.uavRct.entity.mqtt.Waypoint waypointItem : waypointMissionRequest.getWaypoints()) {
            Waypoint waypoint = new Waypoint(waypointItem.getLatitude(),
                    waypointItem.getLongitude(),
                    //判断是否使用任务高度作为航点高度
                    waypointItem.isUsingMissionAltitude() ? waypointMissionRequest.getMissionAltitude() : waypointItem.getAltitude());
            //如果“飞行器偏航角”选择“依照每个航点设置”
            if (waypointMissionRequest.getHeadingMode().equals(USING_WAYPOINT_HEADING)) {
                waypoint.heading = waypointItem.getHeading();
                waypoint.turnMode = WaypointTurnMode.values()[waypointItem.getTurnMode().ordinal()];
            }

            if (waypointMissionRequest.isGimbalPitchRotationEnabled()) {
                waypoint.gimbalPitch = waypointItem.getGimbalPitch();
            }

            //解析拍照间隔时间和距离
            if (photoFileFormat == SettingsDefinitions.PhotoFileFormat.JPEG) {
                if (waypointItem.getShootPhotoTimeInterval() >= 2 && waypointItem.getShootPhotoTimeInterval() <= 6000) {
                    waypoint.shootPhotoTimeInterval = waypointItem.getShootPhotoTimeInterval();
                } else {
                    Log.i(TAG, waypointItem + ".handleWaypointMission: 当前照片格式为JEPG，航点拍照间隔时间不符合规定(2, 6000)：" + waypointItem.getShootPhotoTimeInterval() + "s");
                }
                if (waypointItem.getShootPhotoDistanceInterval() >= 2 * waypointMissionRequest.getAutoFlightSpeed() &&
                        waypointItem.getShootPhotoDistanceInterval() <= 6000 * waypointMissionRequest.getAutoFlightSpeed()) {
                    waypoint.shootPhotoDistanceInterval = waypointItem.getShootPhotoDistanceInterval();
                } else {
                    Log.i(TAG, waypointItem + ".handleWaypointMission: 当前照片格式为JEPG，航点拍照间隔距离不符合规定(2*speed, 6000*speed)：" + waypointItem.getShootPhotoDistanceInterval() + "m");
                }
            } else if (photoFileFormat == SettingsDefinitions.PhotoFileFormat.RAW) {
                if (waypointItem.getShootPhotoTimeInterval() >= 10 && waypointItem.getShootPhotoTimeInterval() < 6000) {
                    waypoint.shootPhotoTimeInterval = waypointItem.getShootPhotoTimeInterval();
                } else {
                    Log.i(TAG, waypointItem + ".handleWaypointMission: 当前照片格式为RAW，航点拍照间隔时间不符合规定(10, 6000)：" + waypointItem.getShootPhotoTimeInterval() + "s");
                }
                if (waypointItem.getShootPhotoDistanceInterval() >= 10 * waypointMissionRequest.getAutoFlightSpeed() &&
                        waypointItem.getShootPhotoDistanceInterval() <= 6000 * waypointMissionRequest.getAutoFlightSpeed()) {
                    waypoint.shootPhotoDistanceInterval = waypointItem.getShootPhotoDistanceInterval();
                } else {
                    Log.i(TAG, waypointItem + ".handleWaypointMission: 当前照片格式为RAW，航点拍照间隔距离不符合规定(10*speed, 6000*speed)：" + waypointItem.getShootPhotoDistanceInterval() + "m");
                }
            }

            //解析航点动作
            for (com.jiushuo.uavRct.entity.mqtt.WaypointAction action : waypointItem.getActions()) {
                WaypointAction waypointAction = new WaypointAction(
                        WaypointActionType.values()[action.getActionType().ordinal()]
                        , action.getActionParam());
                waypoint.addAction(waypointAction);
                Log.i(TAG, "waypointAction: " + waypointAction.actionType + "---" + waypointAction.actionParam);
            }
            waypointList.add(waypoint);
        }

        builder.waypointList(waypointList)
                .waypointCount(waypointList.size());

        //调用接收到航点数据的回调，将数据传到MainActivity中的地图上显示
        if (waypointDataCallback != null) {
            waypointDataCallback.onDataReceived(waypointList,builder.calculateTotalDistance(),(int)builder.calculateTotalTime().floatValue());
        }
        return builder.build();
    }

    private void unLockMotors(final MqttMessage message) {
        Log.i(TAG, "unLockMotors: ");
        flightController.turnOnMotors(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                makeDefaultResponse(djiError, message);
            }
        });
    }

    private void lockMotors(final MqttMessage message) {
        Log.i(TAG, "lockMotors: ");
        flightController.turnOffMotors(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                makeDefaultResponse(djiError, message);
            }
        });
    }

    private void makeDefaultResponse(DJIError djiError, MqttMessage message) {
        DefaultRequest request = JSON.parseObject(new String(message.getPayload()), DefaultRequest.class);
        String responseTopic = request.getResTopic();
        makeDefaultResponse(djiError, responseTopic);
    }

    private void makeDefaultResponse(DJIError djiError, String responseTopic) {
        if (djiError != null) {
            if (responseTopic != null) {
                DefaultResponse response = new DefaultResponse();
                response.setCode(djiError.getErrorCode());
                response(responseTopic, JSON.toJSONString(response, SerializerFeature.WriteMapNullValue));
            }
            Log.i(TAG, "onResult: " + responseTopic + djiError.getDescription());
//            ToastUtils.showToast("onResult: " + responseTopic + djiError.getDescription());
        } else {
            if (responseTopic != null) {
                DefaultResponse response = new DefaultResponse();
                response.setCode(RequestUtil.CODE_SUCCESS);
                response(responseTopic, JSON.toJSONString(response, SerializerFeature.WriteMapNullValue));
            }
            Log.i(TAG, "onResult:" + responseTopic + " success!");
//            ToastUtils.showToast("onResult:" + responseTopic + " success!");
        }
    }


    /**
     * 发布 （模拟其他客户端发布消息）
     *
     * @param message 消息
     */
    public static void publish(String message) {
        String topic = publishTopic;
        Integer qos = 2;
        Boolean retained = false;
        try {
            //参数分别为：主题、消息的字节数组、服务质量、是否在服务器保留断开连接后的最后一条消息
            mqttAndroidClient.publish(topic, message.getBytes(), qos.intValue(), retained.booleanValue());
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发布 （模拟其他客户端发布消息）
     *
     * @param message 消息
     */
    public static void publish(String topic, byte[] message) {
        Integer qos = 2;
        Boolean retained = false;
        try {
            //参数分别为：主题、消息的字节数组、服务质量、是否在服务器保留断开连接后的最后一条消息
            mqttAndroidClient.publish(topic, message, qos.intValue(), retained.booleanValue());
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initMqttUrl();
        //初始化无人机各个部件的变量
        initAircraftParams();
    }

    private void initPublishAndResponseTopic() {
        publishTopic = "RCT/" + serialNumber + "/State";
        Log.i(TAG, "onCreate: publishTopic-->" + publishTopic);
        responseTopic = "RCT/" + serialNumber + "/Res";
        Log.i(TAG, "onCreate: responseTopic" + responseTopic);
    }

    private void initMqttUrl() {
        SharedPreferences mqttPref = getSharedPreferences("mqtt",
                MODE_PRIVATE);
        String mqttUrl = mqttPref.getString("mqttUrl", "");
        if (mqttUrl != null && !"".equals(mqttUrl)) {
            host = "tcp://" + mqttUrl;
        } else {
            mqttUrl = "tcp://192.168.8.2:1883";
        }
    }

    /**
     * 响应 （收到其他客户端的消息后，响应给对方告知消息已到达或者消息有问题等）
     *
     * @param message 消息
     */
    public void response(String message) {
        String topic = responseTopic;
        Integer qos = 2;
        Boolean retained = false;
        try {
            //参数分别为：主题、消息的字节数组、服务质量、是否在服务器保留断开连接后的最后一条消息
            mqttAndroidClient.publish(topic, message.getBytes(), qos.intValue(), retained.booleanValue());
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 响应 （收到其他客户端的消息后，响应给对方告知消息已到达或者消息有问题等）
     *
     * @param message 消息
     */
    public void response(String topic, String message) {
        Integer qos = 2;
        Boolean retained = false;
        try {
            //参数分别为：主题、消息的字节数组、服务质量、是否在服务器保留断开连接后的最后一条消息
            mqttAndroidClient.publish(topic, message.getBytes(), qos.intValue(), retained.booleanValue());
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化MqttClient需要的参数
     */
    private void initMqttClientParams() {
        //mqttAndroidClient = new MqttAndroidClient(this, host, CLIENTID); //ltz denote
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), host, CLIENTID); //ltz change
        mqttAndroidClient.setCallback(mqttCallback); //设置监听订阅消息的回调
        mMqttConnectOptions = new MqttConnectOptions();
        mMqttConnectOptions.setAutomaticReconnect(true); //ltz add
        mMqttConnectOptions.setCleanSession(true); //设置是否清除缓存
        mMqttConnectOptions.setConnectionTimeout(10); //设置超时时间，单位：秒 ltz denote
        mMqttConnectOptions.setKeepAliveInterval(5); //设置心跳包发送间隔，单位：秒 ltz denote
//        mMqttConnectOptions.setUserName(USERNAME); //设置用户名
//        mMqttConnectOptions.setPassword(PASSWORD.toCharArray()); //设置密码

        // last will message
        boolean doConnect = true;
//        String message = "{\"terminal_uid\":\"" + CLIENTID + "\"}";
//        String topic = publishTopic;
//        Integer qos = 2;
//        Boolean retained = false;
//        if ((!message.equals("")) || (!topic.equals(""))) {
//            // 最后的遗嘱
//            try {
//                mMqttConnectOptions.setWill(topic, message.getBytes(), qos.intValue(), retained.booleanValue());
//            } catch (Exception e) {
//                Log.i(TAG, "Exception Occured", e);
//                doConnect = false;
//                iMqttActionListener.onFailure(null, e);
//            }
//        }
        if (doConnect) {
            doClientConnection();
        }
    }

    /**
     * 连接MQTT服务器
     */
    private void doClientConnection() {
        if (!mqttAndroidClient.isConnected() && isConnectIsNomarl()) {
            try {
                mqttAndroidClient.connect(mMqttConnectOptions, null, iMqttActionListener);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 判断网络是否连接
     */
    private boolean isConnectIsNomarl() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            String name = info.getTypeName();
            Log.i(TAG, "当前网络名称：" + name);
            return true;
        } else {
            Log.i(TAG, "没有可用Mqtt网络");
            ToastUtils.setResultToToast("没有可用Mqtt网络");
            /*没有可用网络的时候，延迟5秒再尝试重连*/
//            doConnectionDelay();
            return false;
        }
    }

    /*    */

    /**
     * 没有可用网络的时候，延迟5秒再尝试重连
     *//*
    private void doConnectionDelay() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doClientConnection();
            }
        }, DELAY_MILLIS);
    }*/
    @Override
    public void onDestroy() {
        Log.i(TAG, "服务onDestroy: ");
        try {
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
            }
            if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
                mqttAndroidClient.disconnect(); //断开连接
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    public class MqttBinder extends Binder {
        public void changeMqttUrl(String url) {
            Log.e(TAG, "changeMqttUrl: " + url);
            onDestroy();
            onCreate();
        }

        public boolean isMqttConnected() {
            return mqttAndroidClient.isConnected();
        }

        public boolean isMqttSendingDate() {
            if (disposable == null || disposable.isDisposed()) {
                return false;
            } else {
                return true;
                //return isStarted.get();
            }
        }

        public void connectMqttServer() {
            initAircraftParams();
        }

        public void disconnectMqttServer() throws MqttException {
            if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
                mqttAndroidClient.disconnect();
            }
        }

        public MyMqttService getMyMqttService() {
            return MyMqttService.this;
        }

        public long getWaypointMissionStartTime() {
            return wayPointMissionStartTime;
        }

        public void setWaypointMissionStartTime(long time) {
            wayPointMissionStartTime = time;
        }

        public String getWaypointMissionId() {
            return waypointMissionId;
        }

        public void sentFtpUploadMessage(String s) {
            response("RCT/" + serialNumber + "/State/FtpUpload", s);
        }

        public void makeZoomResponse(int code, String magnification) {
            ZoomResponse response = new ZoomResponse();
            response.setCode(code);
            response.setMagnification(magnification);
            response(zoomResTopic, JSON.toJSONString(response, SerializerFeature.WriteMapNullValue));
        }
    }

    private MqttBinder mqttBinder = new MqttBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mqttBinder;
    }

    private WaypointDataCallback waypointDataCallback = null;

    private ZoomCallback zoomCallback = null;

    private GimbalStateCallback gimbalStateCallback = null;

    public void setWaypointDataCallback(WaypointDataCallback callback) {
        this.waypointDataCallback = callback;
    }

    public void setZoomCallback(ZoomCallback callback) {
        this.zoomCallback = callback;
    }

    public void setGimbalStateCallback(GimbalStateCallback callback) {
        this.gimbalStateCallback = callback;
    }

    public interface WaypointDataCallback {
        void onDataReceived(List<Waypoint> waypointList,float distance, int time_s);
    }

    public interface ZoomCallback {
        void onZoomInReceived();

        void onZoomOutReceived();

        void onZoomResetReceived();
    }

    public interface GimbalStateCallback {
        void onGimbalPitchReceived(float pitch);
    }

}
