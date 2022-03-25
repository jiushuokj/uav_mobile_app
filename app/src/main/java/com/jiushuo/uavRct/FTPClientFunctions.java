package com.jiushuo.uavRct;

import android.util.Log;

import com.jiushuo.uavRct.utils.ToastUtils;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FTPClientFunctions {

    private static final String TAG = "多媒体上传";

    public FTPClient ftpClient = null; // FTP客户端

    /**
     * 连接到FTP服务器
     *
     * @param host     ftp服务器域名
     * @param username 访问用户名
     * @param password 访问密码
     * @param port     端口
     * @return 是否连接成功
     */
    public boolean ftpConnect(String host, String username, String password, int port) {
        try {
            ftpClient = new FTPClient();
            Log.i(TAG, "connecting to the ftp server " + host + " ：" + port);
            ftpClient.setConnectTimeout(5000);
            ftpClient.connect(host, port);
            // 根据返回的状态码，判断链接是否建立成功
            if (FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                Log.i(TAG, "login to the ftp server");
                boolean status = ftpClient.login(username, password);
                /*
                 * 设置文件传输模式
                 * 避免一些可能会出现的问题，在这里必须要设定文件的传输格式。
                 * 在这里我们使用BINARY_FILE_TYPE来传输文本、图像和压缩文件。
                 */
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                ftpClient.enterLocalPassiveMode();
                return status;
            } else {
                ftpClient.disconnect();
                Log.i(TAG, "ftpConnect: 建立连接不成功");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error: could not connect to host " + host);
        }
        return false;
    }

    /**
     * 断开ftp服务器连接
     *
     * @return 断开结果
     */
    public boolean ftpDisconnect() {
        // 判断空指针
        if (ftpClient == null) {
            return true;
        }
        // 断开ftp服务器连接
        try {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
            } else {
                Log.i(TAG, "ftpDisconnect：与FTP服务器本来就没有连接");
            }
            return true;
        } catch (Exception e) {
            Log.i(TAG, "ftpClient.logout();时发生错误");
        } finally {
            try {
                //注意，要将disconnect()放在finally模块里，因为logout也会抛出异常
                ftpClient.disconnect();
                Log.i(TAG, "ftpDisconnect：断开与FTP服务器的连接成功");
            } catch (IOException e) {
                Log.i(TAG, "ftpClient.disconnect();时发生错误");
            }
        }
        return false;
    }

    /**
     * ftp 文件上传
     *
     * @param srcFilePath  源文件目录
     * @param desFileName  文件名称
     * @param desDirectory 目标文件
     * @return 文件上传结果
     */
    public boolean ftpUpload(String srcFilePath, String desFileName, String desDirectory) {
        boolean status = false;
        try {
            FileInputStream srcFileStream = new FileInputStream(srcFilePath);
            status = ftpClient.storeFile(desDirectory + "/" + desFileName, srcFileStream);
            srcFileStream.close();
            return status;
        } catch (Exception e) {
            Log.i(TAG, "上传文件失败 " + e.getLocalizedMessage());
        }
        return status;
    }

    /**
     * ftp 更改目录
     *
     * @param path 更改的路径
     * @return 更改是否成功
     */
    public boolean ftpChangeDir(String path) {
        boolean status = false;
        try {
            status = ftpClient.changeWorkingDirectory(path);
        } catch (Exception e) {
            Log.i(TAG, "change directory failed: " + e.getLocalizedMessage());
        }
        return status;
    }

    public boolean isFtpConnected() {
        if (ftpClient == null) {
            return false;
        } else {
            return ftpClient.isConnected();
        }
    }

    public boolean makeDir(String dir) {
        try {
            if (ftpClient.changeWorkingDirectory(dir)) {
                return true;
            }
            ftpClient.makeDirectory(dir);
            ftpClient.changeWorkingDirectory(dir);
            return true;
        } catch (IOException e) {
            Log.i(TAG, "makeDir: 连接FTP出错：" + e.getMessage());
            return false;
        }
    }

    /**
     * 上传文件夹到ftp上
     *
     * @param remotePath ftp上文件夹路径名称
     * @param localPath  本地上传的文件夹路径名称
     */
    public void uploadDir(String remotePath, String localPath, boolean isRoot) throws IOException {
        File file = new File(localPath);
        if (file.exists()) {
            if (!ftpClient.changeWorkingDirectory(remotePath)) {
                ftpClient.makeDirectory(remotePath);
                ftpClient.changeWorkingDirectory(remotePath);
            }
            if (isRoot) {
                ftpClient.makeDirectory("ZOOM");
                ftpClient.makeDirectory("WIDE");
                ftpClient.makeDirectory("THRM");
                ftpClient.makeDirectory("VIDEO");
            }

            File[] files = file.listFiles();
            if (null != files) {
                for (File f : files) {
                    if (f.isDirectory() && !f.getName().equals(".") && !f.getName().equals("..")) {
                        uploadDir(remotePath + "/" + f.getName(), f.getPath(), false);
                    } else if (f.isFile()) {
                        if (isRoot) {
                            if (!f.getName().endsWith("JPG")) {
                                upload(remotePath + "/" + "VIDEO/" + f.getName(), f);
                            } else {
                                if (f.getName().contains("_Z")) {
                                    upload(remotePath + "/" + "ZOOM/" + f.getName(), f);
                                } else if (f.getName().contains("_W")) {
                                    upload(remotePath + "/" + f.getName(), f);
                                } else if (f.getName().contains("_T")) {
                                    upload(remotePath + "/" + "THRM/" + f.getName(), f);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 上传文件到ftp
     *
     * @param ftpFileName 上传到ftp文件路径名称
     * @param localFile   本地文件路径名称
     */
    public void upload(String ftpFileName, File localFile) throws IOException {
        if (!localFile.exists()) {
            throw new IOException("Can't upload '" + localFile.getAbsolutePath() + "'. This file doesn't exist.");
        }

        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(localFile));
            if (!ftpClient.storeFile(ftpFileName, in)) {
                Log.e(TAG, "文件: " + localFile + "上传到FTP服务器失败");
                throw new IOException("上传文件： '" + localFile + "' 到FTP服务器失败，请检查权限和地址");
            }
            Log.i(TAG, "文件: " + localFile + "上传到FTP服务器成功");
            ToastUtils.setResultToToast(localFile.getName() + "上传成功！");
        } finally {
            closeStream(in);
        }
    }

    /**
     * 关闭流
     *
     * @param stream 流
     */
    private static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ex) {
                // do nothing
            }
        }
    }


}