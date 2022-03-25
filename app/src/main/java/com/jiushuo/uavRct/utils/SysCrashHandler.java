package com.jiushuo.uavRct.utils;

import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class SysCrashHandler implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        saveCrashInfoToFile(throwable);
    }


    /**
     * 保存错误信息到文件中
     * @param ex
     */
    private void saveCrashInfoToFile(Throwable ex) {
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable exCause = ex.getCause();
        while (exCause != null) {
            exCause.printStackTrace(printWriter);
            exCause =exCause.getCause();
        }
        printWriter.close();

        long timeMillis = System.currentTimeMillis();
        //错误日志文件名称
        String fileName = "crash-" + timeMillis + ".txt";
        //判断sd卡可正常使用
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            //文件存储位置
            String path = Environment.getExternalStorageDirectory().getPath() + "/Uav/Logs/";
            File fl = new File(path);
            //创建文件夹
            if(!fl.exists()) {
                fl.mkdirs();
            }
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(path + fileName);
                fileOutputStream.write(writer.toString().getBytes());
                fileOutputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
