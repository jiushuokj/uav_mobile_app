package com.jiushuo.uavRct.settingpanel.fragment;

import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jiushuo.uavRct.DemoApplication;
import com.jiushuo.uavRct.MainActivity;
//import com.dji.mediaManagerDemo.R;
import com.jiushuo.uavRct.R;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.sdkmanager.DJISDKManager;

import static android.content.Context.MODE_PRIVATE;

public class OthersFragment extends Fragment {
    private static final String TAG = "其他参数设置";
    private View view;
    private MainActivity mainActivity;
    private EditText editTextMqttUrl;
    private EditText editTextRtmpUrl;
    private Switch switchRtmpVideoEncode;
    private Spinner spinnerRtmpVideoSource;
    private Switch switchRtmpSoundOn;
    private Switch switchRtmpOnOff;
    private Button btnQueryMqttStatus;
    private Pattern ipPattern = Pattern.compile("(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])");
    private Pattern portPattern = Pattern.compile("(\\d|[1-9]\\d{1,3}|[1-5]\\d{4}|6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])");
    private String mqttUrl;
    private String rtmpUrl;
    private String ftpUrl, ftpUsr, ftpPsd;
    private boolean rtmpVideoEncode;
    private boolean rtmpSoundOn;
    private String serialNumber;
    private InputMethodManager imm;
    private EditText editTextFtpUrl, editTextFtpUsr, editTextFtpPsd;
    private Button btn_query_file_list_state, btn_download_upload_file_list, btn_connect_to_ftp_server, btn_download_upload_all_file;
/*    private Button btn_speech;
    private TextToSpeech textToSpeech;*/

    /*
    usde to push raw h264 data to server
     */
    private static boolean init_socket = false;
    private static DatagramSocket udp_sock;
    private static InetAddress serverAddr;
    private static boolean raw_h264_pusing_ = false;
    protected static VideoFeeder.VideoDataListener mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {
        @Override
        public void onReceive(byte[] videoBuffer, int size) {
            if(!raw_h264_pusing_) return;
            int leftsize = size;
            while(leftsize > 0){
                int send_size = 1024;
                if(leftsize < send_size) {
                    send_size = leftsize;
                }
                byte[] tosends = Arrays.copyOfRange(videoBuffer, size - leftsize, size - leftsize + send_size );
                DatagramPacket packet = new DatagramPacket(tosends , send_size, serverAddr, 34330);
                try {
                    udp_sock.send(packet);
//                        Log.d(TAG,"send frame....");
                } catch (IOException e) {
                    Log.d(TAG, "mReceivedVideoDataListener2 recv data, save dato to file, len = "+send_size);
                    Log.e(TAG, "udp_sock.send(packet) error: " + e);
                    e.printStackTrace();
                }
                leftsize -= send_size;
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.tab7, container, false);
        mainActivity = (MainActivity) getActivity();
        imm = (InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);

        //init udp to send raw h264
//        raw_h264_pusing_ = false;
        if(!init_socket){
            try {
                udp_sock = new DatagramSocket();
                init_socket = true;
            } catch (SocketException e) {
                Log.e(TAG, "udp_sock = new DatagramSocket(6666) error: " + e);
                e.printStackTrace();
            }
        }

        // The callback for receiving the raw H264 video data for camera live view
//        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {
//            @Override
//            public void onReceive(byte[] videoBuffer, int size) {
//                if(!raw_h264_pusing_) return;
//                int leftsize = size;
//                while(leftsize > 0){
//                    int send_size = 1024;
//                    if(leftsize < send_size) {
//                        send_size = leftsize;
//                    }
//                    byte[] tosends = Arrays.copyOfRange(videoBuffer, size - leftsize, size - leftsize + send_size );
//                    DatagramPacket packet = new DatagramPacket(tosends , send_size, serverAddr, 33330);
//                    try {
//                        udp_sock.send(packet);
////                        Log.d(TAG,"send frame....");
//                    } catch (IOException e) {
//                        Log.d(TAG, "mReceivedVideoDataListener2 recv data, save dato to file, len = "+send_size);
//                        Log.e(TAG, "udp_sock.send(packet) error: " + e);
//                        e.printStackTrace();
//                    }
//                    leftsize -= send_size;
//                }
//            }
//        };

        initSerialNumber();
        initUI();

        return view;
    }

    private void initSerialNumber() {
        DemoApplication.getAircraftInstance().getFlightController().getSerialNumber(new CommonCallbacks.CompletionCallbackWith<String>() {
            @Override
            public void onSuccess(String s) {
                serialNumber = s;
            }

            @Override
            public void onFailure(DJIError djiError) {
                Log.e(TAG, "onFailure: " + djiError.getDescription());
            }
        });
    }

    private void initUI() {
        editTextMqttUrl = view.findViewById(R.id.edit_text_mqtt_url);
        initEditTextMqttUrl();
        initEditTextMqttUrlEvents();

        btnQueryMqttStatus = view.findViewById(R.id.btn_query_mqtt_status);
        btnQueryMqttStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean mqttConnected = mainActivity.mqttBinder.isMqttConnected();
                setResultToToast(mqttConnected ? "已经连接到Mqtt服务器" : "没有连接到Mqtt服务器");
                if (mqttConnected) {
                    boolean mqttSendingDate = mainActivity.mqttBinder.isMqttSendingDate();
                    setResultToToast(mqttSendingDate ? "正在发送数据" : "没有发送数据");
                    if (!mqttSendingDate) {
                        setResultToToast("尝试重新连接Mqtt服务器，并发送数据");
                        try {
                            mainActivity.mqttBinder.disconnectMqttServer();
                        } catch (MqttException e) {
                            Log.i(TAG, "disconnectMqttServer, 出现问题" + e.getCause());
                        } finally {
                            mainActivity.mqttBinder.connectMqttServer();
                        }
                    }
                } else {
                    setResultToToast("尝试连接Mqtt服务器");
                    mainActivity.mqttBinder.connectMqttServer();
                }
            }
        });

        editTextRtmpUrl = view.findViewById(R.id.edit_text_rtmp_url);
        initEditTextRtmpUrl();
        initEditTextRtmpUrlEvents();

        switchRtmpVideoEncode = view.findViewById(R.id.switch_rtmp_video_encode);
        initSwitchRtmpVideoEncode();
        initSwitchRtmpVideoEncodeEvent();

        switchRtmpSoundOn = view.findViewById(R.id.switch_rtmp_sound_on);
        initSwitchRtmpSoundOn();
        initSwitchRtmpSoundOnEvent();

        //还未实现
        spinnerRtmpVideoSource = view.findViewById(R.id.spinner_rtmp_video_source);
        initSpinnerRtmpVideoSource();
        initSpinnerRtmpVideoSourceEvent();

        switchRtmpOnOff = view.findViewById(R.id.switch_rtmp_on_off);
        initSwitchRtmpOnOff();
        initSwitchRtmpOnOffEvent();

        editTextFtpUrl = view.findViewById(R.id.edit_text_ftp_url);
        initEditTextFtpUrl();
        initEditTextFtpUrlEvent();

        editTextFtpUsr = view.findViewById(R.id.edit_text_ftp_usr);
        initEditTextFtpUsr();
        initEditTextFtpUsrEvent();

        editTextFtpPsd = view.findViewById(R.id.edit_text_ftp_psd);
        initEditTextFtpPsd();
        initEditTextFtpPsdEvent();

        btn_query_file_list_state = view.findViewById(R.id.btn_query_file_list_state);
        btn_query_file_list_state.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResultToToast("文件列表状态：" + mainActivity.currentFileListState);
            }
        });

        btn_connect_to_ftp_server = view.findViewById(R.id.btn_connect_to_ftp_server);
        btn_connect_to_ftp_server.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.initFtpServerConnection(true);
            }
        });

        btn_download_upload_file_list = view.findViewById(R.id.btn_download_upload_file_list);
        btn_download_upload_file_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.isDownloadAndUploadAll = false;
                mainActivity.downloadRelatedMedia();
            }
        });

        btn_download_upload_all_file = view.findViewById(R.id.btn_download_upload_all_file);
        btn_download_upload_all_file.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //将下载所有文件设置为true，MainActivity中的getWaypointMissionStartTime()就能返回0，这样就能下载所有文件
                mainActivity.isDownloadAndUploadAll = true;
                mainActivity.downloadRelatedMedia();
            }
        });

/*        btn_speech = view.findViewById(R.id.btn_speech);
        btn_speech.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textToSpeech = new TextToSpeech(mainActivity,
                        new TextToSpeech.OnInitListener() {
                            @Override
                            public void onInit(int i) {
                                if (i == TextToSpeech.SUCCESS) {
                                    int language = textToSpeech.setLanguage(Locale.CHINESE);
                                    textToSpeech.setSpeechRate(1.2f);
                                    textToSpeech.setPitch(0.7f);
                                    if ((language != textToSpeech.LANG_COUNTRY_AVAILABLE)
                                            && (language != TextToSpeech.LANG_AVAILABLE)) {
                                        Toast.makeText(mainActivity, "暂时不支持这种语言的朗读", Toast.LENGTH_SHORT)
                                                .show();
                                    }
                                    textToSpeech.speak("微信支付到账10元",
                                            TextToSpeech.QUEUE_ADD, null);
                                }else{
                                    Log.i(TAG, "onInit: TTS引擎初始化失败");
                                }
                            }
                        });
            }
        });*/
    }

    private void initEditTextFtpPsdEvent() {
        editTextFtpPsd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editTextFtpPsd.setFocusableInTouchMode(true);
                editTextFtpPsd.setFocusable(true);
                editTextFtpPsd.requestFocus();
            }
        });
        editTextFtpPsd.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
                        final String currentText = editTextFtpPsd.getText().toString();
                        Log.i(TAG, "initEditTextFtpPsdEvent：输入的Ftp密码---> " + currentText);
                        SharedPreferences.Editor editor = mainActivity.getSharedPreferences("ftp",
                                MODE_PRIVATE).edit();
                        editor.putString("ftpPsd", currentText);
                        editor.apply();
                        setResultToToast("设置Ftp密码成功！");
                    }
                    initEditTextFtpPsd();
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            editTextFtpPsd.setFocusable(false);
                            editTextFtpPsd.setFocusableInTouchMode(false);
                        }
                    });
                    //隐藏软键盘
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                return false; // pass on to other listeners.
            }
        });
    }

    private void initEditTextFtpPsd() {
        SharedPreferences ftpPref = mainActivity.getSharedPreferences("ftp",
                MODE_PRIVATE);
        ftpPsd = ftpPref.getString("ftpPsd", "123");
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (ftpPsd != null) {
                    editTextFtpPsd.setText(ftpPsd);
                } else {
                    editTextFtpPsd.setHint("请输入Ftp服务器密码");
                }
            }
        });
    }

    private void initEditTextFtpUsrEvent() {
        editTextFtpUsr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editTextFtpUsr.setFocusableInTouchMode(true);
                editTextFtpUsr.setFocusable(true);
                editTextFtpUsr.requestFocus();
            }
        });
        editTextFtpUsr.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
                        final String currentText = editTextFtpUsr.getText().toString();
                        Log.i(TAG, "initEditTextFtpUsrEvents：输入的Ftp用户名---> " + currentText);
                        SharedPreferences.Editor editor = mainActivity.getSharedPreferences("ftp",
                                MODE_PRIVATE).edit();
                        editor.putString("ftpUsr", currentText);
                        editor.apply();
                        setResultToToast("设置Ftp用户名成功！");
                    }
                    initEditTextFtpUsr();
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            editTextFtpUsr.setFocusable(false);
                            editTextFtpUsr.setFocusableInTouchMode(false);
                        }
                    });
                    //隐藏软键盘
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                return false; // pass on to other listeners.
            }
        });
    }

    private void initEditTextFtpUsr() {
        SharedPreferences ftpPref = mainActivity.getSharedPreferences("ftp",
                MODE_PRIVATE);
        ftpUsr = ftpPref.getString("ftpUsr", "admin");
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (ftpUsr != null) {
                    editTextFtpUsr.setText(ftpUsr);
                } else {
                    editTextFtpUsr.setHint("请输入Ftp服务器用户名");
                }
            }
        });
    }

    private void initEditTextFtpUrlEvent() {
        editTextFtpUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editTextFtpUrl.setFocusableInTouchMode(true);
                editTextFtpUrl.setFocusable(true);
                editTextFtpUrl.requestFocus();
            }
        });
        editTextFtpUrl.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
                        Log.i(TAG, "editTextFtpUrl: " + editTextFtpUrl.getText());
                        final String currentText = editTextFtpUrl.getText().toString();
                        if ("".equals(currentText)) {
                            setResultToToast("设置失败！" + "当前值不能为空");
                        } else {
                            Log.i(TAG, "initEditTextFtpUrlEvents：输入的IP地址---> " + currentText);
                            String[] strs = currentText.split(":");
                            String ip = strs[0];
                            String port = strs[1];
                            Matcher matcherIp = ipPattern.matcher(ip);
                            Matcher matcherPort = portPattern.matcher(port);
                            if (matcherIp.matches() && matcherPort.matches()) {
                                SharedPreferences.Editor editor = mainActivity.getSharedPreferences("ftp",
                                        MODE_PRIVATE).edit();
                                editor.putString("ftpUrl", currentText);
                                editor.apply();
                                setResultToToast("设置地址成功！");
                            } else {
                                if (!matcherIp.matches()) {
                                    setResultToToast("请输入正确的ip地址!");
                                } else {
                                    setResultToToast("请输入正确的端口！");
                                }
                            }

                        }
                        initEditTextFtpUrl();
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                editTextFtpUrl.setFocusable(false);
                                editTextFtpUrl.setFocusableInTouchMode(false);
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

    private void initEditTextFtpUrl() {
        SharedPreferences ftpPref = mainActivity.getSharedPreferences("ftp",
                MODE_PRIVATE);
        ftpUrl = ftpPref.getString("ftpUrl", "192.168.8.127:21");
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (ftpUrl != null) {
                    editTextFtpUrl.setText(ftpUrl);
                } else {
                    editTextFtpUrl.setHint("请输入IP地址:端口");
                }
            }
        });
    }

    private void initSpinnerRtmpVideoSourceEvent() {

    }

    private void initSpinnerRtmpVideoSource() {

    }

    private void initSwitchRtmpSoundOnEvent() {
        switchRtmpSoundOn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = mainActivity.getSharedPreferences("rtmp",
                        MODE_PRIVATE).edit();
                editor.putBoolean("rtmpSoundOn", isChecked);
                editor.apply();
                Log.i(TAG, "initSwitchRtmpSoundOnEvent: " + "写入SharedPreferencesc成功" + isChecked);
                initSwitchRtmpSoundOn();
            }
        });
    }

    private void initSwitchRtmpSoundOn() {
        SharedPreferences rtmpPref = mainActivity.getSharedPreferences("rtmp", MODE_PRIVATE);
        rtmpSoundOn = rtmpPref.getBoolean("rtmpSoundOn", false);
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switchRtmpSoundOn.setChecked(rtmpSoundOn);
            }
        });
        DJISDKManager.getInstance().getLiveStreamManager().setAudioMuted(!rtmpSoundOn);
//        setResultToToast("设置声音为：" + !DJISDKManager.getInstance().getLiveStreamManager().isAudioMuted());
        Log.i(TAG, "initSwitchRtmpSoundOn: isAudioMuted--->" + DJISDKManager.getInstance().getLiveStreamManager().isAudioMuted());
    }

    private void initSwitchRtmpOnOffEvent() {
        switchRtmpOnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    initEditTextRtmpUrl();
                    startRtmpStreaming("rtmp://" + rtmpUrl + "/stream/" + serialNumber);
                } else {
                    stopRtmpStreaming();
                }
            }
        });
    }

    public void startRtmpStreaming(final String url) {
        if (url != null) {
            if (DJISDKManager.getInstance().getLiveStreamManager() == null) {
                setSwitchRtmpOnOff(false);
                return;
            }
            if (DJISDKManager.getInstance().getLiveStreamManager().isStreaming()) {
                setResultToToast("已经开始Rtmp推流了!");
                setSwitchRtmpOnOff(true);
                return;
            }
            new Thread() {
                @Override
                public void run() {
                    DJISDKManager.getInstance().getLiveStreamManager().setLiveUrl(url);
                    int result = DJISDKManager.getInstance().getLiveStreamManager().startStream();
                    if (result == 0) {
                        //说明推流成功
                        DJISDKManager.getInstance().getLiveStreamManager().setStartTime();
                        setResultToToast("开始Rtmp推流成功！");
                        Log.i(TAG, "开始Rtmp推流:" + result +
                                "\n isVideoStreamSpeedConfigurable:" + DJISDKManager.getInstance().getLiveStreamManager().isVideoStreamSpeedConfigurable() +
                                "\n isLiveAudioEnabled:" + DJISDKManager.getInstance().getLiveStreamManager().isLiveAudioEnabled() +
                                "\n 地址" + url);
                        setSwitchRtmpOnOff(true);
                    } else {
                        setResultToToast("推流失败！请检查地址是否正确！");
                        setSwitchRtmpOnOff(false);
                    }
                }
            }.start();
        }

    }

    private void setSwitchRtmpOnOff(final boolean state) {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switchRtmpOnOff.setChecked(state);
            }
        });
    }

    private void stopRtmpStreaming() {
        if (!isRtmpStreamingOn()) {
            setResultToToast("当前没有进行推流！");
            return;
        }
        DJISDKManager.getInstance().getLiveStreamManager().stopStream();
        Log.i(TAG, "stopRtmpStreaming: " + rtmpUrl);
        setResultToToast("停止Rtmp推流！");
    }

    private void initSwitchRtmpOnOff() {
        setSwitchRtmpOnOff(isRtmpStreamingOn());
    }

    /**
     * 判断是否正在Rtmp推流
     *
     * @return
     */
    private boolean isRtmpStreamingOn() {
        if (DJISDKManager.getInstance().getLiveStreamManager() == null) {
            return false;
        }
        return DJISDKManager.getInstance().getLiveStreamManager().isStreaming();
    }

    /////////////////////////////////////////ltz added
    private void start_pushing_raw_h264_data() {
        //check the ip is valid
        String[] strs = rtmpUrl.split(":");
        String ip = strs[0];
        Matcher matcherIp = ipPattern.matcher(ip);
        if (!matcherIp.matches()) {
            Log.d(TAG, "start_pushing_raw_h264_data ip invalid");
            return;
        }

        try {
            serverAddr = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            Log.e(TAG, ip + " serverAddr = InetAddress.getByName() error: " + e);
            e.printStackTrace();
        }
        VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
        Log.d(TAG, "mReceivedVideoDataListener addVideoDataListener");
        raw_h264_pusing_ = true;
    }

    private void stop_pushing_raw_h264_data(){
        VideoFeeder.getInstance().getPrimaryVideoFeed().removeVideoDataListener(mReceivedVideoDataListener);
        Log.d(TAG, "mReceivedVideoDataListener removeVideoDataListener");
        raw_h264_pusing_ = false;
    }
    /////////////////////////////////////////ltz added

    private void initSwitchRtmpVideoEncode() {
        SharedPreferences rtmpPref = mainActivity.getSharedPreferences("rtmp", MODE_PRIVATE);
        rtmpVideoEncode = rtmpPref.getBoolean("rtmpVideoEncode", false);
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switchRtmpVideoEncode.setChecked(rtmpVideoEncode);
            }
        });
//        DJISDKManager.getInstance().getLiveStreamManager().setVideoEncodingEnabled(rtmpVideoEncode);
//        setResultToToast("设置Rtmp编码成功！" + DJISDKManager.getInstance().getLiveStreamManager().isVideoEncodingEnabled());
//        Log.i(TAG, "initSwitchRtmpVideoEncode: " + DJISDKManager.getInstance().getLiveStreamManager().isVideoEncodingEnabled());
          Log.i(TAG,"initSwitchRtmpVideoEncode set pushing h264 raw data to "+rtmpVideoEncode);
          if(raw_h264_pusing_ && !rtmpVideoEncode){
              stop_pushing_raw_h264_data();
          }
          if(!raw_h264_pusing_ && rtmpVideoEncode){
              start_pushing_raw_h264_data();
          }
    }


    private void initSwitchRtmpVideoEncodeEvent() {
        switchRtmpVideoEncode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = mainActivity.getSharedPreferences("rtmp",
                        MODE_PRIVATE).edit();
                editor.putBoolean("rtmpVideoEncode", isChecked);
                editor.apply();
                Log.i(TAG, "initSwitchRtmpVideoEncodeEvent: " + "写入SharedPreferencesc成功" + isChecked);
                initSwitchRtmpVideoEncode();
            }
        });
    }

    private void initEditTextRtmpUrl() {
        SharedPreferences rtmpPref = mainActivity.getSharedPreferences("rtmp",
                MODE_PRIVATE);
        rtmpUrl = rtmpPref.getString("rtmpUrl", "192.168.8.127:1935");
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (rtmpUrl != null && !"".equals(rtmpUrl)) {
                    editTextRtmpUrl.setText(rtmpUrl);
                } else {
                    editTextRtmpUrl.setHint("请输入IP地址:端口");
                }
            }
        });
    }

    private void initEditTextRtmpUrlEvents() {
        editTextRtmpUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editTextRtmpUrl.setFocusableInTouchMode(true);
                editTextRtmpUrl.setFocusable(true);
                editTextRtmpUrl.requestFocus();
            }
        });
        editTextRtmpUrl.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
                        Log.i(TAG, "editTextRtmpUrl: " + editTextRtmpUrl.getText());
                        final String currentText = editTextRtmpUrl.getText().toString();
                        if ("".equals(currentText)) {
                            setResultToToast("设置失败！" + "当前值不能为空");
                        } else {
                            Log.i(TAG, "initEditTextRtmpUrlEvents：输入的IP地址---> " + currentText);
                            String[] strs = currentText.split(":");
                            String ip = strs[0];
                            String port = strs[1];
                            Matcher matcherIp = ipPattern.matcher(ip);
                            Matcher matcherPort = portPattern.matcher(port);
                            if (matcherIp.matches() && matcherPort.matches()) {
                                SharedPreferences.Editor editor = mainActivity.getSharedPreferences("rtmp",
                                        MODE_PRIVATE).edit();
                                editor.putString("rtmpUrl", currentText);
                                editor.apply();
                                setResultToToast("设置地址成功！");
                            } else {
                                if (!matcherIp.matches()) {
                                    setResultToToast("请输入正确的ip地址!");
                                } else {
                                    setResultToToast("请输入正确的端口！");
                                }
                            }

                        }
                        initEditTextRtmpUrl();
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                editTextRtmpUrl.setFocusable(false);
                                editTextRtmpUrl.setFocusableInTouchMode(false);
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

    private void initEditTextMqttUrl() {
        SharedPreferences mqttPref = mainActivity.getSharedPreferences("mqtt",
                MODE_PRIVATE);
        mqttUrl = mqttPref.getString("mqttUrl", "192.168.8.127:1883");
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mqttUrl != null && !"".equals(mqttUrl)) {
                    editTextMqttUrl.setText(mqttUrl);
                } else {
                    editTextMqttUrl.setHint("请输入IP地址:端口");
                }
            }
        });
    }

    private void initEditTextMqttUrlEvents() {
        editTextMqttUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editTextMqttUrl.setFocusableInTouchMode(true);
                editTextMqttUrl.setFocusable(true);
                editTextMqttUrl.requestFocus();
            }
        });
        editTextMqttUrl.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
                        Log.i(TAG, "edit_text_mqtt_url: " + editTextMqttUrl.getText());
                        final String currentText = editTextMqttUrl.getText().toString();
                        if ("".equals(currentText)) {
                            setResultToToast("设置失败！" + "当前值不能为空");
                        } else {
                            Log.i(TAG, "initEditTextMqttUrlEvents: 输入的IP地址--->" + currentText);
                            String[] strs = currentText.split(":");
                            String ip = strs[0];
                            String port = strs[1];
                            Matcher matcherIp = ipPattern.matcher(ip);
                            Matcher matcherPort = portPattern.matcher(port);
                            if (matcherIp.matches() && matcherPort.matches()) {
                                SharedPreferences.Editor editor = mainActivity.getSharedPreferences("mqtt",
                                        MODE_PRIVATE).edit();
                                editor.putString("mqttUrl", currentText);
                                editor.apply();
                                setResultToToast("设置地址成功！");
                                mainActivity.mqttBinder.changeMqttUrl(currentText);
                            } else {
                                if (!matcherIp.matches()) {
                                    setResultToToast("请输入正确的ip地址!");
                                } else {
                                    setResultToToast("请输入正确的端口！");
                                }
                            }
                        }
                        initEditTextMqttUrl();
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                editTextMqttUrl.setFocusable(false);
                                editTextMqttUrl.setFocusableInTouchMode(false);
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


    private void setResultToToast(final String str) {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mainActivity, str, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
