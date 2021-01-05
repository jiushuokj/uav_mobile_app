package com.jiushuo.uavRct.utils;

import com.amap.api.location.CoordinateConverter;
import com.amap.api.location.DPoint;
import com.amap.api.maps.model.LatLng;
import com.amap.api.services.core.LatLonPoint;
import com.jiushuo.uavRct.DemoApplication;

/**
 * 地球坐标系——WGS84：常见于 GPS 设备，Google 地图等国际标准的坐标体系。
 * 火星坐标系——GCJ-02：中国国内使用的被强制加密后的坐标体系，高德坐标就属于该种坐标体系。
 * 百度坐标系——BD-09：百度地图所使用的坐标体系，是在火星坐标系的基础上又进行了一次加密处理。
 */

public class CoordinateTransUtils {

    private final static double a = 6378245.0; // 长半轴
    private final static double pi = 3.14159265358979324; // π
    private final static double ee = 0.00669342162296594323; // e²

    /**
     * GPS坐标转成高德坐标
     *
     * @param lat
     * @param lng
     * @return
     */
    public static LatLng getGDLatLng(double lat, double lng) {
        CoordinateConverter converter = new CoordinateConverter(DemoApplication.getInstance());
        // CoordType.GPS 待转换坐标类型
        converter.from(CoordinateConverter.CoordType.GPS);
        // sourceLatLng待转换坐标点 DPoint类型
        DPoint desLatLng = new DPoint();
        try {
            converter.coord(new DPoint(lat, lng));
            // 执行转换操作
            desLatLng = converter.convert();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new LatLng(desLatLng.getLatitude(), desLatLng.getLongitude());
    }

    // GCJ-02 to WGS-84
    public static LatLng getDJILatLng(double latitude, double longitude) {
        LatLonPoint dev = calDev(latitude, longitude);
        double retLat = latitude - dev.getLatitude();
        double retLon = longitude - dev.getLongitude();
        for (int i = 0; i < 1; i++) {
            dev = calDev(retLat, retLon);
            retLat = latitude - dev.getLatitude();
            retLon = longitude - dev.getLongitude();
        }
        return new LatLng(retLat, retLon);
    }

    // 计算偏差
    private static LatLonPoint calDev(double wgLat, double wgLon) {
        if (isOutOfChina(wgLat, wgLon)) {
            return new LatLonPoint(0, 0);
        }
        double dLat = calLat(wgLon - 105.0, wgLat - 35.0);
        double dLon = calLon(wgLon - 105.0, wgLat - 35.0);
        double radLat = wgLat / 180.0 * pi;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
        return new LatLonPoint(dLat, dLon);
    }

    // 判断坐标是否在国外
    private static boolean isOutOfChina(double lat, double lon) {
        if (lon < 72.004 || lon > 137.8347)
            return true;
        if (lat < 0.8293 || lat > 55.8271)
            return true;
        return false;
    }

    // 计算纬度
    private static double calLat(double x, double y) {
        double resultLat = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y
                + 0.2 * Math.sqrt(Math.abs(x));
        resultLat += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        resultLat += (20.0 * Math.sin(y * pi) + 40.0 * Math.sin(y / 3.0 * pi)) * 2.0 / 3.0;
        resultLat += (160.0 * Math.sin(y / 12.0 * pi) + 320 * Math.sin(y * pi / 30.0)) * 2.0 / 3.0;
        return resultLat;
    }

    // 计算经度
    private static double calLon(double x, double y) {
        double resultLon = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1
                * Math.sqrt(Math.abs(x));
        resultLon += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        resultLon += (20.0 * Math.sin(x * pi) + 40.0 * Math.sin(x / 3.0 * pi)) * 2.0 / 3.0;
        resultLon += (150.0 * Math.sin(x / 12.0 * pi) + 300.0 * Math.sin(x / 30.0
                * pi)) * 2.0 / 3.0;
        return resultLon;
    }


}
