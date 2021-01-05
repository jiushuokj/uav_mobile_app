package com.jiushuo.uavRct;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.jiushuo.uavRct.R;
import com.jiushuo.uavRct.entity.sqlite.Action;
import com.jiushuo.uavRct.entity.sqlite.Mission;
import com.jiushuo.uavRct.entity.sqlite.WayPoint;
import com.jiushuo.uavRct.mqtt.MyMqttService;
import com.jiushuo.uavRct.settingpanel.fragment.BatteryFragment;
import com.jiushuo.uavRct.settingpanel.fragment.FlightControllerFragment;
import com.jiushuo.uavRct.settingpanel.fragment.GimbalFragment;
import com.jiushuo.uavRct.settingpanel.fragment.ImageTransFragment;
import com.jiushuo.uavRct.settingpanel.fragment.OthersFragment;
import com.jiushuo.uavRct.settingpanel.fragment.RemoteControllerFragment;
import com.jiushuo.uavRct.settingpanel.fragment.SensorFragment;
import com.jiushuo.uavRct.utils.CoordinateTransUtils;
import com.jiushuo.uavRct.utils.DoubleComparer;
import com.jiushuo.uavRct.utils.Helper;
import com.jiushuo.uavRct.utils.ToastUtils;
import com.jiushuo.uavRct.view.ZoomView;

import org.eclipse.paho.android.service.MqttService;
import org.litepal.crud.callback.SaveCallback;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJICameraError;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.model.LocationCoordinate2D;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.log.DJILog;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.media.DownloadListener;
import dji.sdk.media.MediaFile;
import dji.sdk.media.MediaManager;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;
import dji.thirdparty.io.reactivex.Observable;
import dji.thirdparty.io.reactivex.ObservableEmitter;
import dji.thirdparty.io.reactivex.ObservableOnSubscribe;
import dji.thirdparty.io.reactivex.Observer;
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers;
import dji.thirdparty.io.reactivex.disposables.Disposable;
import dji.thirdparty.io.reactivex.functions.Consumer;
import dji.thirdparty.io.reactivex.schedulers.Schedulers;
import dji.ux.widget.FPVOverlayWidget;

public class MainActivity extends BaseActivity implements View.OnClickListener, AMap.OnMyLocationChangeListener, LocationSource, AMap.OnMapClickListener {
    private static final String TAG = "主活动";
    public static final double DELTA = 0.00001;
    private static final int EXIT_APP_FIRST_TIME = 10001;
    private static final String TAG1 = "多媒体下载";
    private static final String TAG2 = "多媒体上传";
    private static final String TAG3 = "多媒体删除";


    public View radarWidget;
    private Button mMediaManagerBtn, mFpvScaleBtn;
    private View fpvVideoFeedView, secondaryFpvWidget;
    private FPVOverlayWidget fpvOverlayWidget;
    private View cameraSettingAdvancedPanel, cameraSettingExposurePanel;
    private Button mLiveStreamSettingBtn, mTasksPanelButton, mExitAppButton;
    private LinearLayout mTasksPanel;
    private ZoomView mZoomView;
    private boolean isFpvScaleUp, isMapScaleUp;
    private RelativeLayout mainCameraLayout, mainMapLayout;
    private MapView aMapView;
    private AMap aMap;
    private Spinner mapSpinner;
    private Button locate, add, delete, clear, clearTrace, offMapSettingButton;
    private Button upload, start, pause, resume, stop;
    private boolean isAdd = false;
    private FlightController mFlightController;
    private double droneLocationLat = DemoApplication.getAircraftInstance().getFlightController().getState().getAircraftLocation().getLatitude();
    private double droneLocationLng = DemoApplication.getAircraftInstance().getFlightController().getState().getAircraftLocation().getLongitude();
    private double droneLocationLatForTrace = DemoApplication.getAircraftInstance().getFlightController().getState().getAircraftLocation().getLatitude();
    private double droneLocationLngForTrace = DemoApplication.getAircraftInstance().getFlightController().getState().getAircraftLocation().getLongitude();
    private int droneHeadDirection;
    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();//地图上的标记点
    private final Map<Integer, Polyline> mPolylines = new ConcurrentHashMap<Integer, Polyline>();//标记点之间的线
    private final Map<Integer, Polyline> mTracePolylines = new ConcurrentHashMap<Integer, Polyline>();//飞行轨迹
    private Marker selectedMarker = null;
    private Marker droneMarker = null;
    private Marker homeMarker = null;

    private List<Waypoint> waypointList = new ArrayList<Waypoint>();
    public static WaypointMission.Builder waypointMissionBuilder;
    private WaypointMissionOperator waypointMissionOperator;
    private LinearLayout weaypointSettingPanel;
    private float altitude = 50.0f;
    private float mSpeed = 5.0f;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;
    private TextView waypointNumText, waypointDistanceText, waypointTimeText, aircraftLatitudeText, aircraftLongitudeText, gimbalPitchText;//航点任务相关信息
    private Intent mMqttServiceIntent;
    private Disposable makeTraceDisposable;
    private boolean isFlying;
    private MediaManager mMediaManager;
    public MediaManager.FileListState currentFileListState = MediaManager.FileListState.UNKNOWN;
    private String destBaseDir = Environment.getExternalStorageDirectory().getPath() + "/WaypointMission/";
    private int lastIndex;
    private List<MediaFile> mediaFileList = new ArrayList<>();
    public FTPClientFunctions ftpClientFunctions = new FTPClientFunctions();
    private String ftpHost = "192.168.8.103";
    private String ftpUsr = "admin";
    private String ftpPsd = "123";
    private String ftpPort = "21";
    //用来判断当前任务是否已经完成
    private volatile ConcurrentHashMap<Long, Boolean> waypointMissionFinishedMap = new ConcurrentHashMap();
    //用于判断当前飞行是否是航点任务
    private boolean isWaypointMission;
    //用来判断是否下载上传照相机中的所有文件
    public boolean isDownloadAndUploadAll;
    //下载上传所有文件的时间
    private long downloadAllMediaTime;
    //要删除的文件集合
    private ArrayList<MediaFile> fileToDelete = new ArrayList<MediaFile>();
//    private int fetchFileListRetryCurrentTimes = 0;

    //设置面板的元素
    private Button cetcButton;//用于控制设置面板显隐
    private LinearLayout settingPanel;
    private ViewPager mViewPager;
    private FragmentPagerAdapter mAdapter;
    //装载Fragment的集合
    private List<Fragment> mFragments;
    //七个Tab对应的布局
    private LinearLayout mTabFlightController, mTabSensor, mTabRemoteController, mTabImageTrans, mTabBattery, mTabGimbal, mTabGeneral;
    //七个Tab对应的ImageButton
    private ImageButton mImgFlightController, mImgSensor, mImgRemoteController, mImgImageTrans, mImgBattery, mImgGimbal, mImgGeneral;

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };

    private static boolean isExit = false;  // 标识是否退出

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EXIT_APP_FIRST_TIME: {
                    isExit = false;
                    break;
                }
                default:
                    break;
            }
        }
    };

    public MyMqttService.MqttBinder mqttBinder;
    private ServiceConnection connection = new ServiceConnection() {
        //活动与服务的连接断开时调用
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        //活动与服务成功绑定时调用
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //获取DownloadBinder实例,由onBind()方法返回downloadBinder实例
            mqttBinder = (MyMqttService.MqttBinder) service;
            mqttBinder.getMyMqttService().setWaypointDataCallback(new MyMqttService.WaypointDataCallback() {
                @Override
                public void onDataReceived(List<Waypoint> waypointList) {
                    Log.i(TAG, "onDataReceived: 接收到远程发送的航点任务数据：" + waypointList);
                    //首先清除地图上的航点
                    clearWaypoint();
                    //再将从远程接收到的航点显示在地图上
                    for (Waypoint waypoint : waypointList) {
                        markWaypoint(CoordinateTransUtils.getGDLatLng(waypoint.coordinate.getLatitude(), waypoint.coordinate.getLongitude()));
                    }
                    //更新航点任务信息数据
                    updateWaypointMissionInfo();
                }
            });
            mZoomView.setMqttBinder(mqttBinder);
            mqttBinder.getMyMqttService().setZoomCallback(new MyMqttService.ZoomCallback() {
                @Override
                public void onZoomInReceived() {
                    Log.i(TAG, "onZoomInReceived: 接收到远程发送ZoomIn指令");
                    mZoomView.zoomIn();
                }

                @Override
                public void onZoomOutReceived() {
                    Log.i(TAG, "onZoomOutReceived: 接收到远程发送ZoomOut指令");
                    mZoomView.zoomOut();
                }

                @Override
                public void onZoomResetReceived() {
                    Log.i(TAG, "onZoomResetReceived: 接收到远程发送ZoomReset指令");
                    mZoomView.zoomReset();
                }
            });
            mqttBinder.getMyMqttService().setGimbalStateCallback(new MyMqttService.GimbalStateCallback() {
                @Override
                public void onGimbalPitchReceived(float pitch) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!DoubleComparer.considerFloatEqual(gimbalPitch, pitch, 0.1f)) {
                                gimbalPitch = pitch;
                                updateGimbalPitchText(pitch);
                            }
                        }
                    });
                }
            });
        }
    };
    private float gimbalPitch;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RegisterConnectionChangeBroadcastReceiver();
        initAirCraftParams();
        initUI();
        initMap(savedInstanceState);
        startMyMqttService();
        initFtpServerConnection(true);
//        LitePal.getDatabase();
        Log.w(TAG, "onCreate: ");
    }

    public void initFtpServerConnection(boolean isConnectionTest) {
        if (ftpClientFunctions.isFtpConnected()) {
            ftpClientFunctions.ftpDisconnect();
        }
        connectToFtpServer(isConnectionTest);
    }

    private void connectToFtpServer(boolean isConnectionTest) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                doConnectToFtpServer(isConnectionTest);
            }
        }).start();
    }

    private void RegisterConnectionChangeBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(DemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);
    }

    private void initAirCraftParams() {
        waypointMissionBuilder = new WaypointMission.Builder();
        waypointMissionOperator = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        initMediaManager();
        initFlightController();
        addWaypointMissionOperatorListener();
    }

    private void updateGimbalPitchText(float gimbalPitch) {
        gimbalPitchText.setText(String.format("%.1f度", gimbalPitch));
    }

    private void initMediaManager() {
        mMediaManager = DemoApplication.getCameraInstance().getMediaManager();
        if (null != mMediaManager) {
            mMediaManager.addUpdateFileListStateListener(fileListStateListener);
        }
    }

    private MediaManager.FileListStateListener fileListStateListener = new MediaManager.FileListStateListener() {
        @Override
        public void onFileListStateChange(MediaManager.FileListState state) {
            currentFileListState = state;
        }
    };

    private void startRecordingTrace() {
        if (makeTraceDisposable != null && !makeTraceDisposable.isDisposed()) {
            return;
        }
        Log.i(TAG, "startRecordingTrace: " + "开始记录航线");
        makeTraceDisposable = Observable.interval(500, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        if (!DoubleComparer.considerEqual(droneLocationLatForTrace, mFlightController.getState().getAircraftLocation().getLatitude(), DELTA) ||
                                !DoubleComparer.considerEqual(droneLocationLngForTrace, mFlightController.getState().getAircraftLocation().getLongitude(), DELTA)) {
                            //坐标转换
//                            final LatLng oldLatlng = new LatLng(droneLocationLatForTrace, droneLocationLngForTrace);
                            final LatLng oldLatlng = CoordinateTransUtils.getGDLatLng(droneLocationLatForTrace, droneLocationLngForTrace);
                            droneLocationLatForTrace = mFlightController.getState().getAircraftLocation().getLatitude();
                            droneLocationLngForTrace = mFlightController.getState().getAircraftLocation().getLongitude();
                            //坐标转换
//                            final LatLng newLatlng = new LatLng(droneLocationLatForTrace, droneLocationLngForTrace);
                            final LatLng newLatlng = CoordinateTransUtils.getGDLatLng(droneLocationLatForTrace, droneLocationLngForTrace);
                            Polyline polyline = aMap.addPolyline(new PolylineOptions().
                                    addAll(Arrays.asList(oldLatlng, newLatlng)).width(4).color(Color.argb(255, 255, 1, 1)));
                            mTracePolylines.put(mTracePolylines.size(), polyline);
                            Log.d(TAG, "startRecordingTrace: " + mTracePolylines.size() + "次轨迹");
                        }
                    }
                });
    }

    private void stopRecordingTrace() {
        if (makeTraceDisposable != null && !makeTraceDisposable.isDisposed()) {
            Log.i(TAG, "stopRecordingTrace: " + "结束记录航线");
            makeTraceDisposable.dispose();
        }
    }

    private void startMyMqttService() {
        //将MainActivity和Service进行绑定
        Intent intent = new Intent(this, MyMqttService.class);
        //传入BIND_AUTO_CREATE 表示在活动和服务进行绑定后自动创建服务。这会使得MyService中的onCreate() 方法得到执行，但onStartCommand() 方法不会执行。
        //会回调服务中的onBind()方法来返回Binder实例
        startService(intent);
        bindService(intent, connection, BIND_AUTO_CREATE);// 绑定服务
    }

    private void initUI() {
        radarWidget = findViewById(R.id.radar_widget);

        waypointNumText = findViewById(R.id.waypoint_number_txt);
        waypointDistanceText = findViewById(R.id.waypoint_distance_txt);
        waypointTimeText = findViewById(R.id.waypoint_time_txt);
        aircraftLatitudeText = findViewById(R.id.aircraft_latitude_txt);
        aircraftLongitudeText = findViewById(R.id.aircraft_longitude_txt);
        updateAircraftLocationText();
        gimbalPitchText = findViewById(R.id.gimbal_pitch_txt);

        mMediaManagerBtn = findViewById(R.id.btn_mediaManager);
        mLiveStreamSettingBtn = findViewById(R.id.btn_live_stream_setting);

        fpvOverlayWidget = findViewById(R.id.fpv_overlay_widget);
        /*fpvOverlayWidget.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.i(TAG, "onTouch: " + event.getX() + ":" + event.getY());
                return false;
            }
        });*/

        cameraSettingAdvancedPanel = findViewById(R.id.CameraAdvancedSetting);
        cameraSettingExposurePanel = findViewById(R.id.CameraExposureMode);

        fpvVideoFeedView = findViewById(R.id.video_view_fpv_video);
        secondaryFpvWidget = findViewById(R.id.secondary_fpv_widget);
        if (Helper.isMultiStreamPlatform()) {
            fpvVideoFeedView.setVisibility(View.VISIBLE);
            secondaryFpvWidget.setVisibility(View.VISIBLE);
        }

        mainCameraLayout = findViewById(R.id.main_camera_layout);
        mainMapLayout = findViewById(R.id.main_left_bottom_map_layout);

        mFpvScaleBtn = findViewById(R.id.btn_fpv_scale);
        mFpvScaleBtn.setOnClickListener(this);

        mMediaManagerBtn.setOnClickListener(this);
        mLiveStreamSettingBtn.setOnClickListener(this);

        Button mMapCameraExchangeBtn = findViewById(R.id.btn_map_camera_exchange);
        mMapCameraExchangeBtn.setOnClickListener(this);

        mapSpinner = findViewById(R.id.map_spinner);
        mapSpinner.setSelection(1, false); // so the listener won't be called before the map is initialized
        mapSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch ((int) id) {
                    case 0:
                        aMap.setMapType(AMap.MAP_TYPE_NORMAL);
                        break;
                    case 1:
                        aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
                        break;
                    case 2:
                        aMap.setMapType(AMap.MAP_TYPE_NIGHT);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        offMapSettingButton = findViewById(R.id.off_map_setting_btn);
        offMapSettingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,
                        com.amap.api.maps.offlinemap.OfflineMapActivity.class));
            }
        });

        locate = (Button) findViewById(R.id.waypoint_locate);
        add = (Button) findViewById(R.id.waypoint_add);
        delete = findViewById(R.id.waypoint_delete);
        clear = (Button) findViewById(R.id.waypoint_clear);

        clearTrace = findViewById(R.id.waypoint_clear_trace);
        upload = (Button) findViewById(R.id.waypoint_upload);
        start = (Button) findViewById(R.id.waypoint_start);
        pause = findViewById(R.id.waypoint_pause);
        resume = findViewById(R.id.waypoint_resume);
        stop = (Button) findViewById(R.id.waypoint_stop);

        locate.setOnClickListener(this);
        add.setOnClickListener(this);
        delete.setOnClickListener(this);
        clear.setOnClickListener(this);
        clearTrace.setOnClickListener(this);
        upload.setOnClickListener(this);
        start.setOnClickListener(this);
        pause.setOnClickListener(this);
        resume.setOnClickListener(this);
        stop.setOnClickListener(this);

        mTasksPanelButton = findViewById(R.id.btn_tasks_panel);
        mTasksPanelButton.setOnClickListener(this);
        mExitAppButton = findViewById(R.id.btn_exit_app);
        mExitAppButton.setOnClickListener(this);

        mTasksPanel = findViewById(R.id.tasks_panel);
        mZoomView = findViewById(R.id.zoom_view);

        cetcButton = findViewById(R.id.cetc_btn);
        cetcButton.setOnClickListener(this);
        settingPanel = findViewById(R.id.setting_panel);
        initSettingPanelView();
        initWaypointSettingUi();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_mediaManager: {
                if (!isStreamOn()) {
                    Intent intent = new Intent(this, PlaybackActivity.class);
                    startActivity(intent);
                } else {
                    setResultToToast("正在推流，请关闭推流后再试");
                }
                break;
            }

            case R.id.btn_fpv_scale: {
                scaleFpv();
                break;
            }

            case R.id.btn_map_camera_exchange: {
                ToastUtils.showToast("交换地图和主摄像头");
                cameraUpdate();
                exchangeMapCameraLayout();
                break;
            }

            case R.id.waypoint_locate: {
                setHomeMarker();
                updateDroneLocation();
                cameraUpdate(); // Locate the drone's place
                updateAircraftLocationText();
                break;
            }
            case R.id.waypoint_add: {
                enableDisableAdd();
                updateWaypointMissionInfo();
                break;
            }
            case R.id.waypoint_delete: {
                deleteSelectedMarker();
                updateWaypointMissionInfo();
                break;
            }
            case R.id.waypoint_clear: {
                clearWaypoint();
                updateWaypointMissionInfo();
                break;
            }

            case R.id.waypoint_clear_trace: {
                clearTrace();
                break;
            }
            case R.id.waypoint_upload: {
                uploadWayPointMission();
                break;
            }
            case R.id.waypoint_start: {
                startWaypointMission();
                break;
            }
            case R.id.waypoint_pause: {
                pauseWaypointMission();
                break;
            }
            case R.id.waypoint_resume: {
                resumeWaypointMission();
                break;
            }
            case R.id.waypoint_stop: {
                stopWaypointMission();
                break;
            }
            case R.id.btn_tasks_panel: {
                showTaskPanel();
                break;
            }
            case R.id.cetc_btn: {
                showSettingPanel();
                break;
            }
            case R.id.btn_exit_app: {
                exitApp();
                break;
            }

            default:
                break;
        }
    }

    private void exitApp() {
        if (!isExit) {
            isExit = true;
            Toast.makeText(this, "再按一次退出键退出程序", Toast.LENGTH_SHORT).show();
            mHandler.sendEmptyMessageDelayed(EXIT_APP_FIRST_TIME, 3000);  // 利用handler延迟发送更改状态信息
        } else {
            DemoApplication.getInstance().exit();
        }
    }

    private void showSettingPanel() {
        if (settingPanel.getVisibility() == View.VISIBLE) {
            settingPanel.setVisibility(View.INVISIBLE);
            mFpvScaleBtn.setEnabled(true);
        } else {
            settingPanel.setVisibility(View.VISIBLE);
            settingPanel.bringToFront();
            mFpvScaleBtn.setEnabled(false);
        }
    }

    private void initSettingPanelView() {
        initSettingViewUIs();//初始化设置页面控件
        initSettingViewEvents();//初始化设置页面事件
        initViewPager();//初始化设置页面ViewPager
    }

    private void initViewPager() {
        mFragments = new ArrayList<>();
        //将七个Fragment加入集合中
        mFragments.add(new FlightControllerFragment());
        mFragments.add(new SensorFragment());
        mFragments.add(new RemoteControllerFragment());
        mFragments.add(new ImageTransFragment());
        mFragments.add(new BatteryFragment());
        mFragments.add(new GimbalFragment());
        mFragments.add(new OthersFragment());

        //初始化适配器
        mAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {//从集合中获取对应位置的Fragment
                return mFragments.get(position);
            }

            @Override
            public int getCount() {//获取集合中Fragment的总数
                return mFragments.size();
            }

        };
        //不要忘记设置ViewPager的适配器
        mViewPager.setAdapter(mAdapter);
        //设置ViewPager的切换监听
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            //页面滚动事件
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            //页面选中事件
            @Override
            public void onPageSelected(int position) {
                //设置position对应的集合中的Fragment
                mViewPager.setCurrentItem(position);
                resetImgs();
                selectTab(position);
            }

            @Override
            //页面滚动状态改变事件
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private View.OnClickListener settingViewOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //先将设置页面的七个ImageButton置为灰色
            resetImgs();

            //根据点击的Tab切换不同的页面及设置对应的ImageButton为绿色
            switch (v.getId()) {
                case R.id.id_tab_flight_controller:
                    selectTab(0);
                    break;
                case R.id.id_tab_sensor:
                    selectTab(1);
                    break;
                case R.id.id_tab_remote_controller:
                    selectTab(2);
                    break;
                case R.id.id_tab_image_trans:
                    selectTab(3);
                    break;
                case R.id.id_tab_battery:
                    selectTab(4);
                    break;
                case R.id.id_tab_gimbal:
                    selectTab(5);
                    break;
                case R.id.id_tab_general:
                    selectTab(6);
                    break;
            }
        }
    };

    private void initSettingViewEvents() {
        //设置七个Tab的点击事件
        mTabFlightController.setOnClickListener(settingViewOnClickListener);
        mTabSensor.setOnClickListener(settingViewOnClickListener);
        mTabRemoteController.setOnClickListener(settingViewOnClickListener);
        mTabImageTrans.setOnClickListener(settingViewOnClickListener);
        mTabBattery.setOnClickListener(settingViewOnClickListener);
        mTabGimbal.setOnClickListener(settingViewOnClickListener);
        mTabGeneral.setOnClickListener(settingViewOnClickListener);
    }

    //初始化控件
    private void initSettingViewUIs() {
        mViewPager = (ViewPager) findViewById(R.id.id_viewpager);

        mTabFlightController = (LinearLayout) findViewById(R.id.id_tab_flight_controller);
        mTabSensor = (LinearLayout) findViewById(R.id.id_tab_sensor);
        mTabRemoteController = (LinearLayout) findViewById(R.id.id_tab_remote_controller);
        mTabImageTrans = (LinearLayout) findViewById(R.id.id_tab_image_trans);
        mTabBattery = (LinearLayout) findViewById(R.id.id_tab_battery);
        mTabGimbal = (LinearLayout) findViewById(R.id.id_tab_gimbal);
        mTabGeneral = (LinearLayout) findViewById(R.id.id_tab_general);

        mImgFlightController = (ImageButton) findViewById(R.id.id_tab_flight_controller_img);
        mImgSensor = (ImageButton) findViewById(R.id.id_tab_sensor_img);
        mImgRemoteController = (ImageButton) findViewById(R.id.id_tab_remote_controller_img);
        mImgImageTrans = (ImageButton) findViewById(R.id.id_image_trans_img);
        mImgBattery = (ImageButton) findViewById(R.id.id_battery_img);
        mImgGimbal = (ImageButton) findViewById(R.id.id_gimbal_img);
        mImgGeneral = (ImageButton) findViewById(R.id.id_general_img);
    }

    private void selectTab(int i) {
        //根据点击的Tab设置对应的ImageButton为蓝色
        switch (i) {
            case 0:
                mImgFlightController.setImageResource(R.drawable.flight_controller_2);
                break;
            case 1:
                mImgSensor.setImageResource(R.drawable.sensor_2);
                break;
            case 2:
                mImgRemoteController.setImageResource(R.drawable.remote_controller_2);
                break;
            case 3:
                mImgImageTrans.setImageResource(R.drawable.image_tans_2);
                break;
            case 4:
                mImgBattery.setImageResource(R.drawable.battery_2);
                break;
            case 5:
                mImgGimbal.setImageResource(R.drawable.gimbal2);
                break;
            case 6:
                mImgGeneral.setImageResource(R.drawable.general_2);
                break;
        }
        //设置当前点击的Tab所对应的页面
        mViewPager.setCurrentItem(i);
    }

    //将四个ImageButton设置为灰色
    private void resetImgs() {
        mImgFlightController.setImageResource(R.drawable.flight_controller_1);
        mImgSensor.setImageResource(R.drawable.sensor_1);
        mImgRemoteController.setImageResource(R.drawable.remote_controller_1);
        mImgImageTrans.setImageResource(R.drawable.image_tans_1);
        mImgBattery.setImageResource(R.drawable.battery_1);
        mImgGimbal.setImageResource(R.drawable.gimbal_1);
        mImgGeneral.setImageResource(R.drawable.general_1);

    }

    private void scaleFpv() {
        if (!isFpvScaleUp) {
            scaleFpvLayout(dp2pxInt(500), dp2pxInt(375));
            fpvVideoFeedView.bringToFront();
            scaleFpvScaleBtn(dp2pxInt(500), dp2pxInt(375));
            mFpvScaleBtn.bringToFront();
            isFpvScaleUp = true;
        } else {
            scaleFpvLayout(dp2pxInt(200), dp2pxInt(150));
            scaleFpvScaleBtn(dp2pxInt(200), dp2pxInt(150));
            isFpvScaleUp = false;
        }
    }

    private void clearTrace() {
        if (mFlightController.getState().isFlying()) {
            setResultToToast("无人机正在飞行，请在无人机降落后再试！");
            return;
        }
        if (mTracePolylines.size() == 0) {
            setResultToToast("地图上没有航迹！");
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (Integer integer : mTracePolylines.keySet()) {
                    mTracePolylines.get(integer).remove();
                }
                Log.i(TAG, "clearTrace: " + mTracePolylines.size());
                mTracePolylines.clear();
                Log.i(TAG, "clearTrace: " + mTracePolylines.size());
                setResultToToast("清除航迹成功！");
            }
        });
    }

    private void clearWaypoint() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (Integer integer : mMarkers.keySet()) {
                    mMarkers.get(integer).remove();
                    mPolylines.get(integer).remove();
                }
                Log.i(TAG, "clearWaypoint: " + mMarkers.size() + "---" + mPolylines.size());
                mMarkers.clear();
                mPolylines.clear();
                Log.i(TAG, "clearWaypoint: " + mMarkers.size() + "---" + mPolylines.size());
            }
        });
        waypointList.clear();
        waypointMissionBuilder.waypointList(waypointList);
        updateDroneLocation();
    }

    private void resumeWaypointMission() {
        Log.i(TAG, "resumeWaypointMission: ");
        if (waypointMissionOperator == null) {
            waypointMissionOperator = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        waypointMissionOperator.resumeMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    setResultToToast("航点任务恢复成功");
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            upload.setEnabled(false);
                            start.setEnabled(false);
                            pause.setEnabled(true);
                            resume.setEnabled(false);
                            stop.setEnabled(true);
                        }
                    });
                } else {
                    setResultToToast("航点任务恢复失败：\n" + djiError.getDescription());
                }
            }
        });
    }

    private void pauseWaypointMission() {
        Log.i(TAG, "pauseWaypointMission: ");
        if (waypointMissionOperator == null) {
            waypointMissionOperator = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        waypointMissionOperator.pauseMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    setResultToToast("航点暂停开始成功");
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            upload.setEnabled(false);
                            start.setEnabled(false);
                            pause.setEnabled(false);
                            resume.setEnabled(true);
                            stop.setEnabled(true);
                        }
                    });
                } else {
                    setResultToToast("航点任务暂停失败：\n" + djiError.getDescription());
                }
            }
        });
    }

    /**
     * 删除地图上当前选定的标记
     * 1.mMarkers中删除该点，地图上删除点，并且在mMarkers中将其后面的点往前移动
     * 2.mPolyLines中删除该点有关的两条线，地图也删除，并且将其前后两点相连
     * 3.选中改点的下一点为当前标记
     */
    private void deleteSelectedMarker() {
        if (selectedMarker == null) {
            setResultToToast("当前没有选中标记");
            return;
        }
        if (mMarkers.size() == 0) {
            setResultToToast("当前地图上没有标记");
            return;
        }
        final Integer selectedIndex = getKey(mMarkers, selectedMarker);
        if (selectedIndex < 0) {
            setResultToToast("选中的标记不在mMarker中");
            return;
        }
        selectedMarker.remove();
        for (int i = selectedIndex; i < mMarkers.size() - 1; i++) {
            mMarkers.put(i, mMarkers.get(i + 1));
        }
        mMarkers.remove(mMarkers.size() - 1);
        if (mPolylines.size() == 0) {
            setResultToToast("当前地图上没有连线");
        } else if (mPolylines.size() == 1) {
            //只有一个点
            for (Integer integer : mPolylines.keySet()) {
                mPolylines.get(integer).remove();
                mPolylines.remove(integer);
            }
        } else if (selectedIndex == mPolylines.size() - 1) {
            //最后一个点
            mPolylines.get(selectedIndex).remove();
            mPolylines.remove(selectedIndex);
            setSelectedMarker(mMarkers.get(selectedIndex - 1));
        } else {
            LatLng homeLatLng = getHomeLatLng();

            Polyline newPolyline = aMap.addPolyline((new PolylineOptions())
                    .add(selectedIndex == 0 ? homeLatLng : mMarkers.get(selectedIndex - 1).getPosition(),
                            mMarkers.get(selectedIndex).getPosition())
                    .width(5)
                    .setDottedLine(true) //虚线
                    .color(Color.BLUE));
            Polyline selectedPolyline = mPolylines.get(selectedIndex);
            selectedPolyline.remove();
            Polyline nextSelectedPolyline = mPolylines.get(selectedIndex + 1);
            nextSelectedPolyline.remove();
            mPolylines.put(selectedIndex, newPolyline);
            for (int i = selectedIndex + 1; i < mPolylines.size() - 1; i++) {
                mPolylines.put(i, mPolylines.get(i + 1));
            }
            mPolylines.remove(mPolylines.size() - 1);
            setSelectedMarker(mMarkers.get(selectedIndex));
        }
        for (Integer integer : mMarkers.keySet()) {
            Log.i(TAG, "deleteSelectedMarker: 第" + integer + "个标记" + mMarkers.get(integer).getPosition());
        }
        for (Integer integer : mPolylines.keySet()) {
            Log.i(TAG, "deleteSelectedMarker: 第" + integer + "条线" + mPolylines.get(integer).getPoints());
        }
    }

    private LatLng getHomeLatLng() {
        LocationCoordinate2D home = mFlightController.getState().getHomeLocation();
        //坐标转换
//        return new LatLng(home.getLatitude(), home.getLongitude());
        return CoordinateTransUtils.getGDLatLng(home.getLatitude(), home.getLongitude());
    }

    private void newTask() {
        Log.i(TAG, "newTask: ");
        final Mission mission = new Mission();
        mission.setMissionId(getUUID());
        mission.setName("task1");
        mission.setType(0);
//        mission.save();
        //例：异步保存：开个线程进行保存，处理完监听回调是否保存成功
        mission.saveAsync().listen(new SaveCallback() {
            @Override
            public void onFinish(boolean success) {
                final WayPoint wayPoint = new WayPoint();
                wayPoint.setWaypointId(getUUID());
                wayPoint.setMissionId(mission.getMissionId());
                wayPoint.setAltitude(50);
                wayPoint.setGimbalPitch(50);
                wayPoint.saveAsync().listen(new SaveCallback() {
                    @Override
                    public void onFinish(boolean success) {
                        Action action = new Action();
                        action.setActionId(getUUID());
                        action.setWaypointId(wayPoint.getWaypointId());
                        action.setActionType(WaypointActionType.GIMBAL_PITCH.ordinal());
                        action.setActionParam(1);
                        action.save();

                        Action action1 = new Action();
                        action1.setActionId(getUUID());
                        action1.setWaypointId(wayPoint.getWaypointId());
                        action1.setActionType(WaypointActionType.GIMBAL_PITCH.ordinal());
                        action1.setActionParam(1);
                        action1.save();
                    }
                });
            }
        });
    }

    private String getUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    private void showTaskPanel() {
        mTasksPanel.bringToFront();
        if (mTasksPanel.getVisibility() == View.VISIBLE) {
            mTasksPanel.setVisibility(View.INVISIBLE);
            weaypointSettingPanel.setVisibility(View.INVISIBLE);
        } else {
            mTasksPanel.setVisibility(View.VISIBLE);
        }
    }

    private void initMap(Bundle savedInstanceState) {
        //获取地图控件引用
        aMapView = (MapView) findViewById(R.id.main_left_bottom_map);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        aMapView.onCreate(savedInstanceState);
        aMapView.setOnClickListener(this);
        if (aMap == null) {
            aMap = aMapView.getMap();
            aMap.setOnMapClickListener(this);// add the listener for click for amap object
        }
        MyLocationStyle myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        myLocationStyle.strokeWidth(0);
        myLocationStyle.showMyLocation(true);
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW_NO_CENTER);//连续定位、蓝点不会移动到地图中心点，并且蓝点会跟随设备移动。
        myLocationStyle.interval(2000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
//        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.pad));
        aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
        aMap.getUiSettings().setMyLocationButtonEnabled(true);//设置默认定位按钮是否显示，非必需设置。
        aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
        aMap.moveCamera(CameraUpdateFactory.zoomTo(18));
        //默认使用卫星图
        aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
        // 绑定 Marker 被点击事件
        aMap.setOnMarkerClickListener(new AMap.OnMarkerClickListener() {
            // marker 对象被点击时回调的接口
            // 返回 true 则表示接口已响应事件，否则返回false
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (getKey(mMarkers, marker) >= 0) {
                    setResultToToast("第" + getKey(mMarkers, marker) + "坐标" + marker.getPosition());
                    setSelectedMarker(marker);
                    return true;
                }
                return false;
            }
        });
        // 绑定marker拖拽事件
        aMap.setOnMarkerDragListener(new AMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker arg0) {
                setResultToToast("开始拖动：" + "第" + getKey(mMarkers, arg0) + "坐标");
            }

            @Override
            public void onMarkerDragEnd(Marker arg0) {
                setResultToToast("结束拖动：" + "第" + getKey(mMarkers, arg0) + "坐标");
                Integer index = getKey(mMarkers, arg0);
                if (index < 0) {
                    return;
                }
                Log.i(TAG, "onMarkerDrag: ---" + index + "---" + arg0.getPosition());
                // 以该标记作为终点的线
                Polyline oldPolyline1 = mPolylines.get(index);
                Polyline newPolyline1 = aMap.addPolyline((new PolylineOptions())
                        .add(oldPolyline1.getPoints().get(0), arg0.getPosition())
                        .width(5)
                        .setDottedLine(true) //虚线
                        .color(Color.BLUE));
                mPolylines.put(index, newPolyline1);
                oldPolyline1.remove();
                // 以该标记作为起点的线
                Polyline oldPolyline2 = mPolylines.get(index + 1);
                Polyline newPolyline2 = aMap.addPolyline((new PolylineOptions())
                        .add(arg0.getPosition(), oldPolyline2.getPoints().get(1))
                        .width(5)
                        .setDottedLine(true) //虚线
                        .color(Color.BLUE));
                mPolylines.put(index + 1, newPolyline2);
                oldPolyline2.remove();
            }

            @Override
            public void onMarkerDrag(Marker arg0) {

            }
        });
        //显示指南针
        aMap.getUiSettings().setCompassEnabled(true);
        //显示比例尺
        aMap.getUiSettings().setScaleControlsEnabled(true);
        setHomeMarker();
        updateDroneLocation();
        cameraUpdate();
    }

    //Add Listener for WaypointMissionOperator
    private void addWaypointMissionOperatorListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().addListener(waypointMissionOperatorListener);
        }
    }

    private void removeListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().removeListener(waypointMissionOperatorListener);
        }
    }

    private WaypointMissionOperatorListener waypointMissionOperatorListener = new WaypointMissionOperatorListener() {
        @Override
        public void onDownloadUpdate(WaypointMissionDownloadEvent downloadEvent) {

        }

        @Override
        public void onUploadUpdate(WaypointMissionUploadEvent uploadEvent) {

        }

        @Override
        public void onExecutionUpdate(WaypointMissionExecutionEvent executionEvent) {

        }

        @Override
        public void onExecutionStart() {
        }

        @Override
        public synchronized void onExecutionFinish(@Nullable final DJIError error) {
            //这个方法可能会被调用多次，要保证一次任务执行完，相关的动作只执行一次
            long waypointMissionStartTime = mqttBinder.getWaypointMissionStartTime();
            Log.i(TAG, "任务id: " + mqttBinder.getWaypointMissionId() + " 执行结束，开始时间为" + getFormattedDate(waypointMissionStartTime));
            if (waypointMissionFinishedMap.get(waypointMissionStartTime) == null) {
                waypointMissionFinishedMap.put(waypointMissionStartTime, false);
            }
            if (!waypointMissionFinishedMap.get(waypointMissionStartTime)) {
                Log.i(TAG, "真正执行完任务的动作: " + mqttBinder.getWaypointMissionId() + "--" + getFormattedDate(waypointMissionStartTime));
                setHomeMarker();
                updateDroneLocation();
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        upload.setEnabled(true);
                        start.setEnabled(true);
                        pause.setEnabled(false);
                        resume.setEnabled(false);
                        stop.setEnabled(false);
                    }
                });
                setResultToToast("航点任务执行: " + (error == null ? "成功!" : "失败，" + error.getDescription()));
                if (error == null) {
                    //如果航点任务执行成功
//                    fetchFileListRetryCurrentTimes = 0;
                    waypointMissionFinishedMap.put(waypointMissionStartTime, true);
                }
            }

        }
    };

    private void doConnectToFtpServer(boolean isConnectionTest) {
        SharedPreferences mqttPref = getSharedPreferences("ftp", MODE_PRIVATE);
        String ftpUrl = mqttPref.getString("ftpUrl", "");
        String usr = mqttPref.getString("ftpUsr", null);
        String psd = mqttPref.getString("ftpPsd", null);
        Log.i(TAG, "从SharedPreferences中获取Ftp的连接信息：url-->" + ftpUrl + "--user-->" + usr + "--psd-->" + psd);
        String ip = null;
        String port = null;
        if (ftpUrl != null && ftpUrl != "") {
            String[] strs = ftpUrl.split(":");
            ip = strs[0];
            port = strs[1];
        }
        boolean status = ftpClientFunctions.ftpConnect(ip == null ? ftpHost : ip,
                usr == null ? ftpUsr : usr,
                psd == null ? ftpPsd : psd,
                Integer.parseInt(port == null ? ftpPort : port));
        Log.i(TAG2, "ftp连接状态" + status);
        setResultToToast(status ? "连接Ftp服务器成功！" : "连接Ftp服务器失败！");
        //连接成功后，如果只是为了测试连接，则关闭连接
        if (isConnectionTest) {
            ftpClientFunctions.ftpDisconnect();
        }
    }

    public void downloadRelatedMedia() {
        DemoApplication.getCameraInstance().getMode(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.CameraMode>() {
            @Override
            public void onSuccess(SettingsDefinitions.CameraMode cameraMode) {
                if (cameraMode != SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD) {
                    DemoApplication.getCameraInstance().setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError error) {
                            if (error == null) {
                                Log.i(TAG1, "设置照相机模式为MEDIA_DOWNLOAD成功");
                                getFileList();
                            } else {
                                Log.e(TAG1, "设置照相机模式为MEDIA_DOWNLOAD失败" + error.getDescription());
                            }
                        }
                    });
                } else {
                    Log.i(TAG, "当前照相机模式为MEDIA_DOWNLOAD");
                    getFileList();
                }
            }

            @Override
            public void onFailure(DJIError djiError) {

            }
        });

    }

    private void getFileList() {
        mMediaManager = DemoApplication.getCameraInstance().getMediaManager();
        Log.i(TAG, "getFileList方法: currentFileListState-->" + currentFileListState);
        setResultToToast("currentFileListState: " + currentFileListState);
        if (mMediaManager != null) {
            if ((currentFileListState == MediaManager.FileListState.SYNCING) || (currentFileListState == MediaManager.FileListState.DELETING)) {
                DJILog.e(TAG1, "多媒体管理的文件列表状态为SYNCING或DELETING，不能下载");
                setResultToToast("多媒体管理的文件列表状态为SYNCING或DELETING，不能下载");
//                afterFetchingFileListFailedAction();
            } else {
                /*if (currentFileListState == MediaManager.FileListState.INCOMPLETE) {
                    DJILog.e(TAG1, "多媒体管理的文件列表状态为INCOMPLETE，不能下载");
                    setResultToToast("多媒体管理的文件列表状态为INCOMPLETE，不能下载");
//                    afterFetchingFileListFailedAction();
                    return;
                }*/
                Log.e(TAG1, "refreshFileListOfStorageLocation: 开始");
                mMediaManager.refreshFileListOfStorageLocation(SettingsDefinitions.StorageLocation.SDCARD, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (null == djiError) {
                            Log.e(TAG1, "refreshFileListOfStorageLocation: ");
                            //Reset data
                            if (currentFileListState != MediaManager.FileListState.INCOMPLETE) {
                                mediaFileList.clear();
                            }
                            mediaFileList = mMediaManager.getSDCardFileListSnapshot();
                            //排序，最新的照片在最前面
                            Collections.sort(mediaFileList, new Comparator<MediaFile>() {
                                @Override
                                public int compare(MediaFile lhs, MediaFile rhs) {
                                    if (lhs.getTimeCreated() < rhs.getTimeCreated()) {
                                        return 1;
                                    } else if (lhs.getTimeCreated() > rhs.getTimeCreated()) {
                                        return -1;
                                    }
                                    return 0;
                                }
                            });
                            Log.i(TAG1, "mediaFileList的大小为: " + mediaFileList.size());
                            downLoadMediaFiles();
                        } else {
                            Log.e(TAG1, "refreshFileListOfStorageLocation: " + djiError.getDescription());
                            setResultToToast("刷新照相机存储位置文件列表失败：" + djiError.getDescription());
//                            afterFetchingFileListFailedAction();
                        }
                    }
                });

            }
        }
    }

/*    private void afterFetchingFileListFailedAction() {
        //最多下载10次，次数可以扩展为自己设置
        if (fetchFileListRetryCurrentTimes < 10) {
            setResultToToast("从相机下载照片失败，1分钟后再试，当前第(" + (fetchFileListRetryCurrentTimes + 1) + "/" + 10 + ")次");
            fetchFileListRetryCurrentTimes++;
            setCameraModeToShootPhoto();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG1, "1分钟后再下载一次");
                    downloadRelatedMedia();
                }
            }, 60000);
        } else {
            setResultToToast("从相机下载照片失败，请手动从相机SD卡下载");
        }
    }*/

    private void downLoadMediaFiles() {
        if (mediaFileList.size() <= 0) {
            Log.i(TAG1, "downLoadMediaFiles: list为空");
        }
        long waypointMissionStartTime = getWaypointMissionStartTime();
        if (waypointMissionStartTime == 0L) {
            downloadAllMediaTime = System.currentTimeMillis();
        }
        Log.i(TAG1, "downLoadMediaFiles: 任务id和开始时间为：" + getWaypointMissionId() + "---->" + getFormattedDate(waypointMissionStartTime));
        List<Integer> indexToBeDownloaded = new ArrayList<>();

        //找到任务开始时间以后拍的照片的index
        for (int i = 0; i < mediaFileList.size(); i++) {
            if (mediaFileList.get(i).getTimeCreated() > waypointMissionStartTime) {
                Log.i(TAG1, "准备下载的文件: " + mediaFileList.get(i).getFileName() + "---->" + getFormattedDate(mediaFileList.get(i).getTimeCreated()));
                indexToBeDownloaded.add(i);
                lastIndex = i;
            } else {
                break;
            }
        }
        Log.i(TAG1, "indexToBeDownloaded的大小: " + indexToBeDownloaded.size());
        //说明没有等待下载的文件，这时说明文件已经下载到移动设备本地或者就没有拍摄照片
        if (indexToBeDownloaded.size() == 0) {
            //下载到移动设备本地的路径
            File localPath = getDownloadLocalPath();
            //判断照片是否已经下载到移动设备本地存储
            if (localPath.exists()) {
                Log.i(TAG, "照片已经下载到移动设备本地存储" + localPath);
                setResultToToast("照片已经下载到" + localPath);
                uploadFileToFtpServer();
            } else {
                setResultToToast("本次任务没有拍摄照片");
                Log.i(TAG2, "本次任务没有拍摄照片");
            }
            afterDownloadActions();
        } else {
            doDownloadMediaFiles();
        }
    }

    private File getDownloadLocalPath() {
        long time = getWaypointMissionStartTime();
        return new File(destBaseDir + getWaypointMissionId() + "/" + getFormattedDate(time == 0L ? downloadAllMediaTime : time));
    }


    private void doDownloadMediaFiles() {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            // 1. 创建被观察者 & 生产事件
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                //因为mediaFileList在前面已经排成倒序
                for (int i = lastIndex; i >= 0; i--) {
                    emitter.onNext(i);
                }
                emitter.onComplete();
            }
        }).observeOn(Schedulers.newThread())
                .subscribe(new Observer<Integer>() {
                    // 2. 通过通过订阅（subscribe）连接观察者和被观察者
                    // 3. 创建观察者 & 定义响应事件的行为
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.i(TAG1, "开始采用subscribe连接");
                    }

                    // 默认最先调用复写的 onSubscribe（）
                    @Override
                    public void onNext(Integer value) {
                        Log.i(TAG1, "对Next事件" + value + "作出响应");
                        doDownloadMediaFileByIndex(value);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i(TAG1, "对Error事件作出响应" + e);
                    }

                    @Override
                    public void onComplete() {
                        Log.i(TAG1, "对Complete事件作出响应");
                    }

                });
    }

    private void doUploadFileToFtpServer() {
        //初始化Ftp连接
        if (ftpClientFunctions.isFtpConnected()) {
            ftpClientFunctions.ftpDisconnect();
        }
        doConnectToFtpServer(false);

        //下载完成，那么将文件上传到FTP服务器
        Log.i(TAG2, "下载到本地完成，开始上传到Ftp服务器: " + ftpClientFunctions.ftpClient);

        //获取航点任务的id和开始时间
        String waypointMissionId = getWaypointMissionId();
        long waypointMissionStartTime = getWaypointMissionStartTime();
        if (waypointMissionStartTime == 0L) {
            waypointMissionStartTime = downloadAllMediaTime;
        }
        //本地地址和远端地址
        String localPath = destBaseDir + waypointMissionId + "/" + getFormattedDate(waypointMissionStartTime);
        String remotePath = "/WaypointMission/" + waypointMissionId + "/" + getFormattedDate(waypointMissionStartTime);

        //在ftp服务器上创建目录
        if (ftpClientFunctions.isFtpConnected()) {
            Log.i(TAG2, "isFtpConnected(): " + ftpClientFunctions.isFtpConnected());
            if (ftpClientFunctions.makeDir("/WaypointMission")) {
                Log.i(TAG2, "makeDir(\"/WaypointMission\")");
                if (ftpClientFunctions.makeDir(waypointMissionId)) {
                    Log.i(TAG2, "makeDir(" + waypointMissionId + ")");
                    if (ftpClientFunctions.makeDir(getFormattedDate(waypointMissionStartTime))) {
                        Log.i(TAG2, "makeDir(" + getFormattedDate(waypointMissionStartTime) + ")");
                        try {
                            Log.i(TAG2, "remotePath: " + remotePath + "<-----local Path: " + localPath);
                            //开始真正上传文件
                            ftpClientFunctions.uploadDir(remotePath, localPath);
                            ftpClientFunctions.ftpDisconnect();
                        } catch (IOException e) {
                            Log.i(TAG2, e.getMessage());
                            setResultToToast(e.getMessage());
                        }
                    }
                }
            }
        } else {
            //没有连接上Ftp服务器
            Log.i(TAG2, "isFtpConnected(): " + ftpClientFunctions.isFtpConnected());
        }
    }

    private String getWaypointMissionId() {
        String waypointMissionId = mqttBinder.getWaypointMissionId();
        if (MyMqttService.DEFAULT.equals(waypointMissionId)) {
            waypointMissionId = getLatestWaypointMissionId();
        }
        return waypointMissionId;
    }

    private long getWaypointMissionStartTime() {
        long waypointMissionStartTime = mqttBinder.getWaypointMissionStartTime();
        if (waypointMissionStartTime == 0L) {
            //如果不是下载所有，则获取上次任务开始时间，来下载上次任务拍摄的照片
            if (!isDownloadAndUploadAll) {
                waypointMissionStartTime = getLatestWaypointMissionStartTime();
            }
        } else {
            //如果是下载所有，则waypointMissionStartTime = 0L，这样就能把所有文件都下载下来了
            if (isDownloadAndUploadAll) {
                waypointMissionStartTime = 0L;
            }
        }
        return waypointMissionStartTime;
    }

    private long getLatestWaypointMissionStartTime() {
        long waypointMissionStartTime;
        SharedPreferences preferences = getSharedPreferences("waypoint",
                MODE_PRIVATE);
        waypointMissionStartTime = preferences.getLong("time", 0L);
        Log.i(TAG2, "getLatestWaypointMissionStartTime: " + waypointMissionStartTime);
        return waypointMissionStartTime;
    }

    private String getLatestWaypointMissionId() {
        String waypointMissionId;
        SharedPreferences preferences = getSharedPreferences("waypoint",
                MODE_PRIVATE);
        waypointMissionId = preferences.getString("id", "default");
        Log.i(TAG2, "getLatestWaypointMissionId: " + waypointMissionId);
        return waypointMissionId;
    }

    private void doDownloadMediaFileByIndex(final int index) {
        File localPath = getDownloadLocalPath();
        Log.i(TAG1, "downloadMediaFileByIndex: " + mediaFileList.get(index).getFileName() + " to filepath: " + localPath.getAbsolutePath());
        if ((mediaFileList.get(index).getMediaType() == MediaFile.MediaType.PANORAMA) || (mediaFileList.get(index).getMediaType() == MediaFile.MediaType.SHALLOW_FOCUS)) {
            Log.i(TAG1, "downloadMediaFileByIndex: mediaFileList.get(index).getMediaType() == MediaFile.MediaType.PANORAMA) || (mediaFileList.get(index).getMediaType() == MediaFile.MediaType.SHALLOW_FOCUS");
            return;
        }

        mediaFileList.get(index).fetchFileData(localPath, null, new DownloadListener<String>() {
            @Override
            public void onFailure(DJIError error) {
                setResultToToast("下载第" + (lastIndex - index + 1) + "个多媒体文件：" + mediaFileList.get(index).getFileName() + "失败，总共有" + (lastIndex + 1) + "个多媒体文件");
                Log.i(TAG1, "下载第" + index + "个多媒体文件失败" + error.getDescription());
            }

            @Override
            public void onProgress(long total, long current) {
            }

            @Override
            public void onRateUpdate(long total, long current, long persize) {
            }

            @Override
            public void onStart() {
                Log.i(TAG1, "开始下载第" + index + "个多媒体文件，总共有" + lastIndex + "个多媒体文件");
            }

            @Override
            public void onSuccess(String filePath) {
                setResultToToast("(" + (lastIndex - index + 1) + "/" + (lastIndex + 1) + ")" + "下载成功");
                Log.i(TAG1, "下载第" + index + "个多媒体文件成功！");
                //每下载完成一张照片后，删除相机SD卡上的文件
                batchDeleteFiles(index);
                if (index == 0) {
                    Log.i(TAG1, "第" + (lastIndex + 1) + "个多媒体文件（最后一个）完成！");
                    //所有照片都下载成功后，上传到Ftp服务器
                    uploadFileToFtpServer();
                }
            }
        });
    }

    private void uploadFileToFtpServer() {
        //上传文件到Ftp服务器
        Observable.create(new ObservableOnSubscribe<Integer>() {
            // 1. 创建被观察者 & 生产事件
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                //因为mediaFileList在前面已经排成倒序
                emitter.onNext(1);
                emitter.onComplete();
            }
        }).observeOn(Schedulers.newThread())
                .subscribe(new Observer<Integer>() {
                    // 2. 通过通过订阅（subscribe）连接观察者和被观察者
                    // 3. 创建观察者 & 定义响应事件的行为
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.i(TAG2, "开始采用subscribe连接");
                    }

                    // 默认最先调用复写的 onSubscribe（）
                    @Override
                    public void onNext(Integer value) {
                        Log.i(TAG2, "对Next事件" + value + "作出响应");
                        doUploadFileToFtpServer();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i(TAG2, "对Error事件作出响应" + e);
                    }

                    @Override
                    public void onComplete() {
                        Log.i(TAG2, "对Complete事件作出响应");
                        //完成了上传，则发送消息到mqtt服务器通知，已经完成了上传
                        String remotePath = "/WaypointMission/" + mqttBinder.getWaypointMissionId() + "/" + getFormattedDate(getWaypointMissionStartTime());
                        mqttBinder.sentFtpUploadMessage(remotePath);
                        /*if (getWaypointMissionStartTime() == 0L) {
                            File localPath = getDownloadLocalPath();
                            deleteFile(localPath);
                        }*/
                    }

                });
    }

    public boolean deleteFile(File file) {
        //判断文件不为null或文件目录存在
        if (file == null || !file.exists()) {
            Log.i(TAG2, "deleteFile: 文件删除失败,请检查文件路径是否正确");
            return false;
        }
        //取得这个目录下的所有子文件对象
        File[] files = file.listFiles();
        //遍历该目录下的文件对象
        for (File f : files) {
            //打印文件名
            String name = file.getName();
            System.out.println(name);
            //判断子目录是否存在子目录,如果是文件则删除
            if (f.isDirectory()) {
                deleteFile(f);
            } else {
                f.delete();
            }
        }
        //删除空文件夹  for循环已经把上一层节点的目录清空。
        file.delete();
        Log.i(TAG2, "deleteFile:删除" + file.getAbsolutePath() + "成功！");
        return true;
    }

    private void batchDeleteFiles(final int index) {
        if (mediaFileList.size() > index) {
            fileToDelete.add(mediaFileList.get(index));
        }
        if (index == 0) {
            mMediaManager.deleteFiles(fileToDelete, new CommonCallbacks.CompletionCallbackWithTwoParam<List<MediaFile>, DJICameraError>() {
                @Override
                public void onSuccess(List<MediaFile> x, DJICameraError y) {
                    for (MediaFile mediaFile : fileToDelete) {
                        Log.i(TAG3, "删除" + mediaFile.getFileName() + "成功！");
                    }
                    setResultToToast("删除" + mediaFileList.size() + "个文件成功！");
                    if (index == 0) {
                        //退出下载模式，将照相机设置为拍照模式，清空mediaFileList
                        afterDownloadActions();
                        fileToDelete.clear();
                    }
                }

                @Override
                public void onFailure(DJIError error) {
                    Log.i(TAG3, "删除" + mediaFileList.size() + "个文件失败！");
                    setResultToToast("删除" + mediaFileList.size() + "个文件失败！");
                    fileToDelete.clear();
                }
            });
        }

    }

    private void afterDownloadActions() {
        if (mMediaManager != null) {
            mMediaManager.exitMediaDownloading();
            Log.i(TAG2, "MediaManager退出下载");
        }
        setCameraModeToShootPhoto();
        Log.i(TAG2, "afterDownloadActions: MediaManager的currentFileListState为" + currentFileListState);
        mediaFileList.clear();
    }

    private void setCameraModeToShootPhoto() {
        DemoApplication.getCameraInstance().setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError mError) {
                if (mError != null) {
                    setResultToToast("照相机设置为拍照模式失败：" + mError.getDescription());
                    Log.e(TAG2, "照相机设置为拍照模式失败：" + mError.getDescription());
                } else {
                    Log.i(TAG2, "照相机设置为拍照模式成功！ ");
                }
            }
        });
    }

    public WaypointMissionOperator getWaypointMissionOperator() {
        if (waypointMissionOperator == null) {
            waypointMissionOperator = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        return waypointMissionOperator;
    }

    private void onProductConnectionChange() {
        initAirCraftParams();
        loginAccount();
    }

    private void loginAccount() {
        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Log.e(TAG, "Login Success");
                    }

                    @Override
                    public void onFailure(DJIError error) {
                        setResultToToast("Login Error:" + error.getDescription());
                    }
                });
    }

    private void initFlightController() {
        BaseProduct product = DemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();
            }
        }
        if (mFlightController != null) {
            mFlightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(FlightControllerState state) {
                    //如果无人机的坐标或者方向发生变化
                    if (!DoubleComparer.considerEqual(droneLocationLat, state.getAircraftLocation().getLatitude(), DELTA) ||
                            !DoubleComparer.considerEqual(droneLocationLng, state.getAircraftLocation().getLongitude(), DELTA) ||
                            droneHeadDirection != state.getAircraftHeadDirection()) {
                        droneLocationLat = state.getAircraftLocation().getLatitude();
                        droneLocationLng = state.getAircraftLocation().getLongitude();
                        droneHeadDirection = state.getAircraftHeadDirection();
                        updateDroneLocation();
                    }
                    if (isFlying != state.isFlying()) {
                        if (isFlying == false) {
                            startRecordingTrace();
                            Log.i(TAG, "waypointMissionOperator的当前状态: " + waypointMissionOperator.getCurrentState());
                            isWaypointMission = false;
                            if (waypointMissionOperator.getCurrentState() == WaypointMissionState.EXECUTING) {
                                isWaypointMission = true;
                            }
                        } else {
                            stopRecordingTrace();
                            if (isWaypointMission) {
                                Log.i(TAG, "下载相关的多媒体文件: " + getFormattedDate(getWaypointMissionStartTime()));
                                //下载相关的Media
                                downloadRelatedMedia();
                            }
                        }
                        isFlying = state.isFlying();
                    }
                }
            });
        }
    }

    private void updateDroneLocation() {
        //坐标转换
//        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        LatLng pos = CoordinateTransUtils.getGDLatLng(droneLocationLat, droneLocationLng);
        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions().position(pos).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_compass_aircraft));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }
                if (checkGpsCoordination(droneLocationLat, droneLocationLng)) {
                    droneMarker = aMap.addMarker(markerOptions);
                    //图标根据机头方向旋转
                    droneMarker.setRotateAngle(360 - (float) droneHeadDirection);
                    //显示飞机纬度经度
                    updateAircraftLocationText();
                }
            }
        });
    }

    @SuppressLint("DefaultLocale")
    private void updateAircraftLocationText() {
        aircraftLatitudeText.setText(String.format("%.5f", droneLocationLat));
        aircraftLongitudeText.setText(String.format("%.5f", droneLocationLng));
    }

    /**
     * 判断是否在推流
     *
     * @return
     */
    private boolean isStreamOn() {
        if (DJISDKManager.getInstance().getLiveStreamManager() == null) {
            ToastUtils.setResultToToast("No live stream manager!");
            return false;
        }
        return DJISDKManager.getInstance().getLiveStreamManager().isStreaming();
    }

    private void cameraUpdate() {
        //坐标转换
//        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        LatLng pos = CoordinateTransUtils.getGDLatLng(droneLocationLat, droneLocationLng);
        float zoomlevel = (float) 18.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
        aMap.moveCamera(cu);
    }

    private void enableDisableAdd() {
        setHomeMarker();
        updateDroneLocation();
        if (!isAdd) {
            if (mFlightController.getState().isFlying()) {
                setResultToToast("无人机正在飞行，请在无人机降落后再试！");
                return;
            }
            isAdd = true;
            add.setText("保存/退出");
            setMarkersDraggable(true);
            delete.setEnabled(true);
            clear.setEnabled(true);
            upload.setEnabled(false);
            start.setEnabled(false);
            pause.setEnabled(false);
            resume.setEnabled(false);
            stop.setEnabled(false);
            weaypointSettingPanel.setVisibility(View.VISIBLE);
        } else {
            isAdd = false;
            add.setText("新建/编辑");
            setMarkersDraggable(false);
            loadWaypointMission();
            weaypointSettingPanel.setVisibility(View.INVISIBLE);
        }
    }

    private void setMarkersDraggable(boolean draggable) {
        for (Integer integer : mMarkers.keySet()) {
            mMarkers.get(integer).setDraggable(draggable);
        }
    }

    private void loadWaypointMission() {
        if (mMarkers.size() == 0) {
            setResultToToast("地图上没有标记！");
            return;
        }
        // 将mMarkers中的点转换成航点的列表
        if (waypointMissionBuilder == null) {
            waypointMissionBuilder = new WaypointMission.Builder();
        }
        List<Waypoint> waypointList = new ArrayList<>();
        for (int i = 0; i < mMarkers.size(); i++) {
            //坐标转换
            LatLng djiLatLng = CoordinateTransUtils.getDJILatLng(mMarkers.get(i).getPosition().latitude,
                    mMarkers.get(i).getPosition().longitude);
            Waypoint waypoint = new Waypoint(djiLatLng.latitude,
                    djiLatLng.longitude, altitude);
            waypointList.add(waypoint);
        }
        waypointMissionBuilder
                .finishedAction(mFinishedAction)
                .headingMode(mHeadingMode)
                .autoFlightSpeed(mSpeed)
                .maxFlightSpeed(mSpeed)
                .flightPathMode(WaypointMissionFlightPathMode.NORMAL)
                .waypointList(waypointList)
                .waypointCount(waypointList.size());
        if (waypointMissionOperator == null) {
            waypointMissionOperator = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        Log.i(TAG, "loadWaypointMission: altitude-->" + altitude + "\nmFinishedAction-->" + mFinishedAction + "\nmHeadingMode-->" + mHeadingMode + "\nmSpeed-->" + mSpeed);
        DJIError error = waypointMissionOperator.loadMission(waypointMissionBuilder.build());
        if (error == null) {
            setResultToToast("保存任务成功！");
            delete.setEnabled(false);
            clear.setEnabled(false);
            upload.setEnabled(true);
            start.setEnabled(false);
            pause.setEnabled(false);
            resume.setEnabled(false);
            stop.setEnabled(false);
        } else {
            setResultToToast("保存任务失败：\n " + error.getDescription());
        }
    }

    private void initWaypointSettingUi() {
        weaypointSettingPanel = findViewById(R.id.waypoint_config_panel);

        final TextView wpAltitude_TV = (TextView) findViewById(R.id.altitude);
        wpAltitude_TV.setText("50");
        RadioGroup speed_RG = (RadioGroup) findViewById(R.id.speed);

        RadioGroup actionAfterFinished_RG = (RadioGroup) findViewById(R.id.actionAfterFinished);
        RadioGroup heading_RG = (RadioGroup) findViewById(R.id.heading);

        wpAltitude_TV.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
                        altitude = Integer.parseInt(nullToIntegerDefault(wpAltitude_TV.getText().toString()));
                    }
                }
                return false; // pass on to other listeners.
            }
        });

        speed_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.lowSpeed) {
                    mSpeed = 3.0f;
                } else if (checkedId == R.id.MidSpeed) {
                    mSpeed = 5.0f;
                } else if (checkedId == R.id.HighSpeed) {
                    mSpeed = 10.0f;
                }
            }

        });

        actionAfterFinished_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select finish action");
                if (checkedId == R.id.finishNone) {
                    mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                } else if (checkedId == R.id.finishGoHome) {
                    mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
                } else if (checkedId == R.id.finishAutoLanding) {
                    mFinishedAction = WaypointMissionFinishedAction.AUTO_LAND;
                } else if (checkedId == R.id.finishToFirst) {
                    mFinishedAction = WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
                }
            }
        });

        heading_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select heading");

                if (checkedId == R.id.headingNext) {
                    mHeadingMode = WaypointMissionHeadingMode.AUTO;
                } else if (checkedId == R.id.headingInitDirec) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
                } else if (checkedId == R.id.headingRC) {
                    mHeadingMode = WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
                } else if (checkedId == R.id.headingWP) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
                }
            }
        });

    }

    String nullToIntegerDefault(String value) {
        if (!isIntValue(value)) value = "0";
        return value;
    }

    boolean isIntValue(String val) {
        try {
            val = val.replace(" ", "");
            Integer.parseInt(val);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private void uploadWayPointMission() {
//        Log.d(TAG, "uploadWayPointMission: " + waypointList.toString());
        for (Waypoint waypoint : waypointList) {
            Log.d(TAG, "uploadWayPointMission: " + waypoint);
        }

        getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    setResultToToast("航点任务上传成功");
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            start.setEnabled(true);
                            pause.setEnabled(false);
                            resume.setEnabled(false);
                            stop.setEnabled(false);
                        }
                    });
                } else {
                    setResultToToast("Mission upload failed, error: " + error.getDescription() + " retrying...");
                    getWaypointMissionOperator().retryUploadMission(null);
                }
            }
        });

    }

    private void startWaypointMission() {
        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    setResultToToast("航点任务开始成功");
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            upload.setEnabled(false);
                            start.setEnabled(false);
                            pause.setEnabled(true);
                            resume.setEnabled(false);
                            stop.setEnabled(true);
                        }
                    });
                } else {
                    setResultToToast("航点任务开始失败：\n" + error.getDescription());
                }
            }
        });

    }

    public static String getFormattedDate(long wayPointMissionStartTime) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        Date date = new Date(wayPointMissionStartTime);
        return simpleDateFormat.format(date);
    }

    private void stopWaypointMission() {
        getWaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    setResultToToast("航点任务结束成功");
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pause.setEnabled(false);
                            resume.setEnabled(false);
                        }
                    });
                } else {
                    setResultToToast("航点任务结束失败：\n" + error.getDescription());
                }
            }
        });
    }

    /**
     * 设置相关的组件的可见性：
     * 1.主摄像头参数状态栏
     * 2.Beacon等左上角组件
     * 3.Radar
     * 4.拍照组件
     * 5.任务相关按钮
     */
    private void setRelatedViewVisibility() {
        LinearLayout cameraStatusBar = findViewById(R.id.camera_status_bar);
        LinearLayout upLeftWidgets = findViewById(R.id.up_left_widgets);

        View cameraControlsWidget = findViewById(R.id.CameraCapturePanel);

        Button mLiveStreamSettingBtn = findViewById(R.id.btn_live_stream_setting);


        if (isMapScaleUp) {
            cameraStatusBar.setVisibility(View.INVISIBLE);
            upLeftWidgets.setVisibility(View.INVISIBLE);
//            radarWidget.setVisibility(View.INVISIBLE);
            cameraControlsWidget.setVisibility(View.INVISIBLE);

            locate.setVisibility(View.VISIBLE);
            mapSpinner.setVisibility(View.VISIBLE);
            offMapSettingButton.setVisibility(View.VISIBLE);

            mMediaManagerBtn.setTextColor(getResources().getColor(R.color.black_half));
            mMediaManagerBtn.setBackgroundColor(getResources().getColor(R.color.white));
            mLiveStreamSettingBtn.setTextColor(getResources().getColor(R.color.black_half));
            mLiveStreamSettingBtn.setBackgroundColor(getResources().getColor(R.color.white));

            mTasksPanelButton.setVisibility(View.VISIBLE);
            mTasksPanelButton.setTextColor(getResources().getColor(R.color.black_half));
            mTasksPanelButton.setBackgroundColor(getResources().getColor(R.color.white));

            mZoomView.setVisibility(View.INVISIBLE);
            clearTrace.setVisibility(View.VISIBLE);
        } else {
            cameraStatusBar.setVisibility(View.VISIBLE);
            upLeftWidgets.setVisibility(View.VISIBLE);
            radarWidget.setVisibility(View.VISIBLE);
            cameraControlsWidget.setVisibility(View.VISIBLE);

            locate.setVisibility(View.INVISIBLE);
            mapSpinner.setVisibility(View.INVISIBLE);
            offMapSettingButton.setVisibility(View.INVISIBLE);

            mMediaManagerBtn.setTextColor(getResources().getColor(R.color.white));
            mMediaManagerBtn.setBackgroundColor(getResources().getColor(R.color.transparent));
            mLiveStreamSettingBtn.setTextColor(getResources().getColor(R.color.white));
            mLiveStreamSettingBtn.setBackgroundColor(getResources().getColor(R.color.transparent));

            mTasksPanelButton.setVisibility(View.INVISIBLE);
            mTasksPanelButton.setTextColor(getResources().getColor(R.color.white));
            mTasksPanelButton.setBackgroundColor(getResources().getColor(R.color.transparent));

            mZoomView.setVisibility(View.VISIBLE);
            mTasksPanel.setVisibility(View.INVISIBLE);
            clearTrace.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * 交换主摄像头和地图的布局参数，来交换两者的大小和位置
     */
    private void exchangeMapCameraLayout() {
        RelativeLayout.LayoutParams layoutParams1 = (RelativeLayout.LayoutParams) mainMapLayout.getLayoutParams();
        RelativeLayout.LayoutParams layoutParams2 = (RelativeLayout.LayoutParams) mainCameraLayout.getLayoutParams();
        mainCameraLayout.setLayoutParams(layoutParams1);
        mainMapLayout.setLayoutParams(layoutParams2);
        if (isMapScaleUp) {
            isMapScaleUp = false;
        } else {
            isMapScaleUp = true;
        }
        setRelatedViewVisibility();
    }


    private void scaleFpvLayout(int width, int height) {
        fpvVideoFeedView.getLayoutParams().width = width;
        fpvVideoFeedView.getLayoutParams().height = height;
        secondaryFpvWidget.getLayoutParams().width = width;
        secondaryFpvWidget.getLayoutParams().height = height;
    }

    private void scaleFpvScaleBtn(int width, int height) {
        mFpvScaleBtn.getLayoutParams().width = width;
        mFpvScaleBtn.getLayoutParams().height = height;
    }

    public float dp2px(float dpValue) {
        final float scale = MainActivity.this.getResources().getDisplayMetrics().density;
        return (dpValue * scale + 0.5f);
    }

    public int dp2pxInt(float dpValue) {
        final float scale = MainActivity.this.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.w(TAG, "onStart: ");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.w(TAG, "onStop: ");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: ");
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        if (ftpClientFunctions.isFtpConnected()) {
            ftpClientFunctions.ftpDisconnect();
        }
        mMediaManager.removeFileListStateCallback(fileListStateListener);
        aMapView.onDestroy();
        unregisterReceiver(mReceiver);
        removeListener();
        if (makeTraceDisposable != null && !makeTraceDisposable.isDisposed()) {
            makeTraceDisposable.dispose();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (Integer integer : mMarkers.keySet()) {
                    mMarkers.get(integer).remove();
                    mPolylines.get(integer).remove();
                }
                for (Integer integer : mTracePolylines.keySet()) {
                    mTracePolylines.get(integer).remove();
                }
                mMarkers.clear();
                mPolylines.clear();
                mTracePolylines.clear();
            }
        });

        unbindService(connection);
        Log.i(TAG, "服务unbindService: ");
        stopService(new Intent(this, MqttService.class));
        Log.i(TAG, "服务stopMqttService: ");
        stopService(new Intent(this, MyMqttService.class));
        Log.i(TAG, "服务stopMyMqttService: ");

        Log.w(TAG, "onDestroy: ");
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        aMapView.onResume();
        initAirCraftParams();
        Log.w(TAG, "onResume: ");
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        aMapView.onPause();
        Log.w(TAG, "onPause: ");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        aMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (isStreamOn()) {
            stopLiveShow();
        }
        super.onBackPressed();
    }

    private void stopLiveShow() {
        if (!isStreamOn()) {
            return;
        }
        DJISDKManager.getInstance().getLiveStreamManager().stopStream();
        ToastUtils.setResultToToast("结束Rtmp推流");
    }

    @Override
    public void onMyLocationChange(Location location) {
        Log.w(TAG, "onMyLocationChange: " + location);
    }

    @Override
    public void activate(LocationSource.OnLocationChangedListener onLocationChangedListener) {
    }

    @Override
    public void deactivate() {

    }

    @Override
    public void onMapClick(LatLng point) {
        if (isAdd) {
            markWaypoint(point);
            updateWaypointMissionInfo();
            Log.d(TAG, "onMapClick: " + " 经度：" + point.longitude + "  纬度：" + point.latitude);
            clear.setEnabled(true);
        } else {
            setResultToToast("地图不能进行点击操作！");
        }
    }

    private void updateWaypointMissionInfo() {
        waypointNumText.setText(String.valueOf(mMarkers.size()));
        float distance = calculateWaypointMissionDistance();
        waypointDistanceText.setText((int) distance + "m");
        waypointTimeText.setText(secToTime((int) (distance / mSpeed)));
    }

    private float calculateWaypointMissionDistance() {
        float distance = 0;
        if (mMarkers.size() == 0) {
            return distance;
        }
        if (mMarkers.size() == 1) {
            distance = AMapUtils.calculateLineDistance(homeMarker.getPosition(), mMarkers.get(0).getPosition());
            return distance;
        }

        for (Integer integer : mMarkers.keySet()) {
            if (integer.intValue() == 0) {
                distance += AMapUtils.calculateLineDistance(homeMarker.getPosition(), mMarkers.get(0).getPosition());
            } else {
                distance += AMapUtils.calculateLineDistance(mMarkers.get(integer - 1).getPosition(), mMarkers.get(integer).getPosition());
            }
        }
        return distance;
    }

    public static String secToTime(int seconds) {
        int hour = seconds / 3600;
        int minute = (seconds - hour * 3600) / 60;
        int second = (seconds - hour * 3600 - minute * 60);

        StringBuffer sb = new StringBuffer();
        if (hour > 0) {
            sb.append(hour + "h");
        }
        if (minute > 0) {
            sb.append(minute + "m");
        }
        if (second > 0) {
            sb.append(second + "s");
        }
        if (second == 0) {
            sb.append("0s");
        }
        return sb.toString();
    }

    private void setHomeMarker() {
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(getHomeLatLng());
        markerOptions.draggable(false);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (homeMarker != null) {
                    homeMarker.remove();
                }
                homeMarker = aMap.addMarker(markerOptions);
                Log.i(TAG, "setHomeMarker: " + markerOptions.getPosition());
            }
        });
    }

    private void markWaypoint(final LatLng point) {
        //Create MarkerOptions object
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.draggable(true);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
        Marker marker = aMap.addMarker(markerOptions);
        //画线
        List<LatLng> latLngs = new ArrayList<LatLng>();
        if (mMarkers.size() == 0) {
            LatLng homeLatLng = getHomeLatLng();
            Log.i(TAG, "home坐标：" + homeLatLng.latitude + "---" + homeLatLng.longitude);
            latLngs.add(homeLatLng);
        } else {
            latLngs.add(mMarkers.get(mMarkers.size() - 1).getPosition());
        }
        latLngs.add(marker.getPosition());
        for (LatLng latLng : latLngs) {
            Log.i(TAG, "画线坐标: " + latLng);
        }
        Polyline polyline = aMap.addPolyline((new PolylineOptions())
                .add(latLngs.get(0), latLngs.get(1))
                .width(5)
                .setDottedLine(true) //虚线
                .color(Color.BLUE));
        mMarkers.put(mMarkers.size(), marker);
        mPolylines.put(mPolylines.size(), polyline);
        setSelectedMarker(marker);
    }

    private void setSelectedMarker(Marker marker) {
        for (Integer integer : mMarkers.keySet()) {
            if (mMarkers.get(integer).equals(marker)) {
                mMarkers.get(integer).setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                selectedMarker = marker;
            } else {
                mMarkers.get(integer).setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
            }
        }
    }

    private void setResultToToast(final String string) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //根据map的value获取map的key
    private Integer getKey(Map<Integer, Marker> map, Marker value) {
        Integer key = -1;
        for (Map.Entry<Integer, Marker> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                key = entry.getKey();
            }
        }
        return key;
    }

    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }


}
