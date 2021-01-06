# 效果展示
![avatar](https://github.com/jiushuokj/uav_mobile_app/blob/M210/fig1.png)
![avatar](https://github.com/jiushuokj/uav_mobile_app/blob/M210/fig2.png)


# 设计说明
* 使用大疆4.12版本的SDK
* 本app基于大疆官方的[MediaManagerDemo](https://github.com/DJI-Mobile-SDK-Tutorials/Android-MediaManagerDemo)来构建
* 主要实现了：
    1. 基于Mqtt来实现以一定频率发送无人机的状态信息，接受外部发送的指令（详情见[协议](https://github.com/jiushuokj/uav_protocol/blob/main/RCT.md)，代码MyMqttService.java）
    2. 使用dji-sdk的MediaManager来实现在航点任务结束并且降落后将本次任务拍摄的多媒体文件下载到本地(MainActivity.java中downloadRelatedMedia()方法)
    3. 在多媒体文件下载到本地后，使用FtpClient将这些多媒体文件上传到Ftp服务器（FTPClientFunctions.java和MainActivity.java中uploadFileToFtpServer()方法）
    4. 使用高德地图来实时展示无人机(MainActitity.java中的mFlightController.setStateCallback())和遥控器的当前位置
    5. 在无人机移动时能够显示其移动轨迹(MainActivity.java中startRecordingTrace())
    6. 使用dji-sdk的航点任务Api来实现本地的航点任务的功能(MainActivity.java中loadWaypointMission())
    7. 接受到航点任务的mqtt指令后，将其在地图上显示(MainActivity.java中setWaypointDataCallback())
    8. 根据DJI-Pilot的设置页面来实现自己的设置页面(使用ViewPager实现，具体的每个Fragment详见包settingpanel.fragment中的7个类)
    9. 实现相机的Zoom in/out功能部件（ZoomView.java）

# 使用说明
