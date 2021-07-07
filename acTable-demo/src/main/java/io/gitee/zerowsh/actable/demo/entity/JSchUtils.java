package io.gitee.zerowsh.actable.demo.entity;

import com.jcraft.jsch.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author Mr.Xu
 * @description: 连接linux工具类, 可实现执行命令和文件上传
 * @create 2021-02-01 18:24
 */
public class JSchUtils {
    private static Integer TIMEOUT = 5 * 60 * 1000;

    /**
     * 获取session
     *
     * @param host     ip
     * @param port     端口
     * @param username 用户名
     * @param password 密码
     * @return Session
     */
    public static Session getSession(String host, Integer port, String username, String password) {
        Properties properties = new Properties();
        properties.put("StrictHostKeyChecking", "no");
        JSch jSch = new JSch();
        Session session = null;
        try {
            session = jSch.getSession(username, host, port);
            session.setPassword(password);
            session.setTimeout(TIMEOUT);
            session.setConfig(properties);
            session.connect();
        } catch (JSchException e) {
            e.printStackTrace();
        }
        return session;
    }

    /**
     * 开启exec通道
     *
     * @param session Session
     * @return ChanelExec
     */
    public static ChannelExec openChannelExec(Session session) {
        ChannelExec channelExec = null;
        try {
            channelExec = (ChannelExec) session.openChannel("exec");
        } catch (JSchException e) {
            e.printStackTrace();
        }
        return channelExec;
    }

    /**
     * 关闭channelExec
     *
     * @param channelExec ChannelExec
     */
    public static void closeChannelExec(ChannelExec channelExec) {
        if (channelExec != null) {
            channelExec.disconnect();
        }
    }

    /**
     * 异步执行,不需要结果
     *
     * @param session Session
     * @param cmd     命令
     */
    public static void execCmdWithOutResult(Session session, String cmd) {
        ChannelExec channelExec = openChannelExec(session);
        channelExec.setCommand(cmd);
        try {
            channelExec.connect();
        } catch (JSchException e) {
            e.printStackTrace();
        }
        closeChannelExec(channelExec);
    }

    /**
     * 同步执行,需要获取执行完的结果
     *
     * @param session Session
     * @param cmd     命令
     * @param charset 字符格式
     * @return 结果
     */
    public static String execCmdWithResult(Session session, String cmd, String charset) {
        ChannelExec channelExec = openChannelExec(session);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        channelExec.setCommand(cmd);
        channelExec.setOutputStream(out);
        String result = null;
        try {
            channelExec.connect();
            Thread.sleep(2000);
            result = new String(out.toByteArray(), charset);
            out.close();
            //关闭通道
            closeChannelExec(channelExec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 开启SFTP通道
     *
     * @param session Session
     * @return ChannelSftp
     * @throws Exception
     */
    public static ChannelSftp openChannelSftp(Session session) {
        ChannelSftp channelSftp = null;
        try {
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
        } catch (JSchException e) {
            e.printStackTrace();
        }
        return channelSftp;
    }

    /**
     * 关闭ChannelSftp
     *
     * @param channelSftp ChannelSftp
     */
    public static void closeChannelSftp(ChannelSftp channelSftp) {
        if (channelSftp != null) {
            channelSftp.disconnect();
        }
    }

    /**
     * 上传文件,相同路径ui覆盖
     *
     * @param session    Session
     * @param remotePath 远程目录地址
     * @param uploadFile 文件 File
     */
    public static void uploadFile(Session session, String remotePath, File uploadFile) {
        ChannelSftp channelSftp = null;
        FileInputStream input = null;
        try {
            channelSftp = openChannelSftp(session);
            input = new FileInputStream(uploadFile);
            if (hasPath(remotePath, channelSftp)) {
                System.out.println("可以找到 pathHome === " + remotePath);
                channelSftp.cd(remotePath);
            }
            channelSftp.put(input, uploadFile.getName());
            session.disconnect();
            System.out.println(">>>>>文件上传成功****" + uploadFile.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断有无路径
     *
     * @param path 路径
     * @return true or false
     */
    public static Boolean hasPath(String path, ChannelSftp sftp) {
        try {
            sftp.lstat(path);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 根据进程名获取进程的pid
     *
     * @param session     Session
     * @param processName 进程名
     * @return List<String> 进程集合
     */
    public static List<String> getPidLinuxCmd(Session session, String processName) {
        String cmd = "ps -ef|grep " + processName + " | grep -v grep";
        String result = execCmdWithResult(session, cmd, "utf-8");
        String[] arr = result.split("\n");
        List<String> pids = new ArrayList<>();
        for (int i = 0; i <= arr.length - 1; ++i) {
            String thatPid = arr[i].split("\\s+")[1];
            if ("-f".equals(thatPid)) {
                break;
            }
            pids.add(thatPid);
        }
        return pids;
    }
}
