package com.example.multithreaddown;

import android.util.Log;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Description:
 * <br/>网站: <a href="http://www.crazyit.org">疯狂Java联盟</a>
 * <br/>Copyright (C), 2001-2014, Yeeku.H.Lee
 * <br/>This program is protected by copyright laws.
 * <br/>Program Name:
 * <br/>Date:
 *
 * @author Yeeku.H.Lee kongyeeku@163.com
 * @version 1.0
 */
public class DownUtil {

    private String TAG=getClass().getSimpleName();
    // 定义下载资源的路径
    private String path;
    // 指定所下载的文件的保存位置
    private String targetFile;
    // 定义需要使用多少线程下载资源
    private int threadNum;
    // 定义下载的线程对象
    private DownThread[] threads;
    // 定义下载的文件的总大小
    private int fileSize;

    public DownUtil(String path, String targetFile, int threadNum) {
        this.path = path;
        this.threadNum = threadNum;
        // 初始化threads数组
        threads = new DownThread[threadNum];
        this.targetFile = targetFile;
    }

    public void download() throws Exception {
        Log.e(TAG,"文件地址path:"+path);
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5 * 1000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty(
                "Accept",
                "image/gif, image/jpeg, image/pjpeg, image/pjpeg, "
                        + "application/x-shockwave-flash, application/xaml+xml, "
                        + "application/vnd.ms-xpsdocument, application/x-ms-xbap, "
                        + "application/x-ms-application, application/vnd.ms-excel, "
                        + "application/vnd.ms-powerpoint, application/msword, */*");
        conn.setRequestProperty("Accept-Language", "zh-CN");
        conn.setRequestProperty("Charset", "UTF-8");
        conn.setRequestProperty("Connection", "Keep-Alive");
        // 得到文件大小
        fileSize = conn.getContentLength();
        Log.e(TAG,"文件大小fileSize:"+fileSize);
        conn.disconnect();
        //除最后一个线程外其它线程平均下载的文件大小
        int currentPartSize = fileSize % threadNum==0?fileSize/threadNum:fileSize/threadNum+1;
        Log.e(TAG,"第1-(threadNum-1)线程平均下载的文件大小currentPartSize:"+currentPartSize);
        int currentSumSize=(threadNum-1)*currentPartSize;
        //Log.e(TAG,"第1-(threadNum-1)线程累计文件大小currentSumSize:"+currentSumSize);
        //最后一个线程下载的文件大小
        int lastPartSize=fileSize-currentSumSize;
        Log.e(TAG,"第threadNum个线程下载的文件大小lastPartSize:"+lastPartSize);
        RandomAccessFile file = new RandomAccessFile(targetFile, "rw");
        // 设置本地文件的大小
        file.setLength(fileSize);
        file.close();
        for (int i = 0; i < threadNum; i++) {
            // 计算每条线程的下载的开始位置
            int startPos = i * currentPartSize;
            int endPos=(i+1)*currentPartSize-1;
            //Log.e(TAG,"下载起点startPos:"+startPos);
            // 每个线程使用一个RandomAccessFile进行下载
            RandomAccessFile currentPart = new RandomAccessFile(targetFile,
                    "rw");
            //当前文件指针位置
            //long pointPosition1=currentPart.getFilePointer();
            //Log.e(TAG,"当前文件指针位置pointPosition-1:"+pointPosition1);
            // 定位该线程的下载位置
            currentPart.seek(startPos);
            //long pointPosition2=currentPart.getFilePointer();
            //Log.e(TAG,"当前文件指针位置pointPosition-2:"+pointPosition2);
            // 创建下载线程
            //最后一个线程下载文件剩余长度
                threads[i] = new DownThread(startPos,endPos,currentPartSize,
                        currentPart);
            //设置现场名称
            threads[i].setName("t"+i);
            // 启动下载线程
            threads[i].start();
        }
    }

    // 获取下载的完成百分比
    public double getCompleteRate() {
        // 统计多条线程已经下载的总大小
        int sumSize = 0;
        for (int i = 0; i < threadNum; i++) {
            if (threads[i] != null) {
                sumSize += threads[i].length;
            }
        }
        // 返回已经完成的百分比
        return sumSize * 1.0 / fileSize;
    }

    private class DownThread extends Thread {
        // 当前线程的下载位置
        private int startPos;
        //当前线程的下载结束位置
        private  int endPos;
        // 定义当前线程负责下载的文件大小
        private int currentPartSize;
        // 当前线程需要下载的文件块
        private RandomAccessFile currentPart;
        // 定义已经该线程已下载的字节数
        public int length;

        public DownThread(int startPos,int endPos, int currentPartSize,
                          RandomAccessFile currentPart) {
            this.startPos = startPos;
            this.endPos=endPos;
            this.currentPartSize = currentPartSize;
            this.currentPart = currentPart;
        }

        @Override
        public void run() {
            try {
                URL url = new URL(path);
                HttpURLConnection conn = (HttpURLConnection) url
                        .openConnection();
                conn.setConnectTimeout(5 * 1000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty(
                        "Accept",
                        "image/gif, image/jpeg, image/pjpeg, image/pjpeg, "
                                + "application/x-shockwave-flash, application/xaml+xml, "
                                + "application/vnd.ms-xpsdocument, application/x-ms-xbap, "
                                + "application/x-ms-application, application/vnd.ms-excel, "
                                + "application/vnd.ms-powerpoint, application/msword, */*");
                conn.setRequestProperty("Range", "bytes=" + startPos + "-" + endPos);
                conn.setRequestProperty("Accept-Language", "zh-CN");
                conn.setRequestProperty("Charset", "UTF-8");
                InputStream inStream = conn.getInputStream();
                // 跳过startPos个字节，表明该线程只下载自己负责哪部分文件。
                inStream.skip(this.startPos);
                byte[] buffer = new byte[1024];
                int hasRead = 0;
                // 读取网络数据，并写入本地文件
                while ((hasRead = inStream.read(buffer))!=-1) {
                    currentPart.write(buffer, 0, hasRead);
                    // 累计该线程下载的总大小
                    length += hasRead;
                }
                Log.e(TAG,"线程"+Thread.currentThread().getName()+"下载的文件大小length:"+length);
                currentPart.close();
                inStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}