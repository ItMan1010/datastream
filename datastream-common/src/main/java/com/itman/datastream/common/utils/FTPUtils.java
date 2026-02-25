/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.itman.datastream.common.utils;

import cn.hutool.extra.ftp.Ftp;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

import static cn.hutool.extra.ftp.FtpMode.Passive;


@Slf4j
public class FTPUtils {

    /**
     * FTP
     */
    private static final int FTP_TYPE = 1;

    /**
     * SFTP
     */
    private static final int SFTP_TYPE = 2;

    /**
     * 统一的连接接口
     */
    public interface FtpConnection {
        void disconnect();
        List<String> listFiles(String path);
        void download(String remotePath, String fileName, File localFile);
    }

    /**
     * FTP 连接实现
     */
    private static class FtpConnectionImpl implements FtpConnection {
        private final Ftp ftp;

        public FtpConnectionImpl(Ftp ftp) {
            this.ftp = ftp;
        }

        @Override
        public void disconnect() {
            close(ftp);
        }

        @Override
        public List<String> listFiles(String path) {
            try {
                return ftp.ls(path);
            } catch (Exception e) {
                log.error("FTP获取文件列表失败：{}", e.getMessage());
                return null;
            }
        }

        @Override
        public void download(String remotePath, String fileName, File localFile) {
            ftp.download(remotePath, fileName, localFile);
        }
    }

    /**
     * SFTP 连接实现
     */
    private static class SftpConnectionImpl implements FtpConnection {
        private final ChannelSftp sftp;
        private final Session session;

        public SftpConnectionImpl(ChannelSftp sftp, Session session) {
            this.sftp = sftp;
            this.session = session;
        }

        @Override
        public void disconnect() {
            if (sftp != null && sftp.isConnected()) {
                sftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }

        @Override
        public List<String> listFiles(String path) {
            try {
                Vector<ChannelSftp.LsEntry> list = sftp.ls(path);
                List<String> result = new java.util.ArrayList<>();
                for (ChannelSftp.LsEntry entry : list) {
                    String fileName = entry.getFilename();
                    // 跳过 . 和 ..
                    if (!".".equals(fileName) && !"..".equals(fileName)) {
                        result.add(fileName);
                    }
                }
                return result;
            } catch (Exception e) {
                log.error("SFTP获取文件列表失败：{}", e.getMessage());
                return null;
            }
        }

        @Override
        public void download(String remotePath, String fileName, File localFile) {
            try {
                String remoteFilePath = remotePath.endsWith("/") ? remotePath + fileName : remotePath + "/" + fileName;
                sftp.get(remoteFilePath, localFile.getAbsolutePath());
            } catch (Exception e) {
                throw new RuntimeException("SFTP下载文件失败：" + e.getMessage(), e);
            }
        }
    }
    /**
     * 创建FTP/SFTP连接
     * 根据端口自动选择协议：21使用FTP，22使用SFTP
     **/
    private static FtpConnection createFtpConnection(String host, Integer port, String user, String pwd, Integer type) {
        // 根据端口判断协议类型
        if (type == SFTP_TYPE) {
            return createSftp(host, port, user, pwd);
        } else if (type == FTP_TYPE) {
            return createFtp(host, port, user, pwd);
        }
        return null;
    }

    /**
     * 创建FTP连接
     */
    private static FtpConnection createFtp(String host, Integer port, String user, String pwd) {
        Ftp ftp = null;
        try {
            port = Objects.isNull(port) ? FTP_TYPE : port;
            ftp = StringUtils.isEmpty(user) && StringUtils.isEmpty(pwd) ? new Ftp(host, port) : new Ftp(host, port, user, pwd);

            //一定要设置模式，不然查询不了文件
            ftp.setMode(Passive);
        } catch (Exception e) {
            log.error("创建FTP链接失败，host={}, port={}, user={}", host, port, user, e);
            throw new RuntimeException("连接FTP服务器失败: " + e.getMessage(), e);
        }
        if (ftp == null) {
            throw new RuntimeException("连接FTP服务器失败,请检查配置是否正确");
        }
        return new FtpConnectionImpl(ftp);
    }

    /**
     * 创建SFTP连接
     */
    private static FtpConnection createSftp(String host, Integer port, String user, String pwd) {
        Session session = null;
        ChannelSftp sftp = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(user, host, port);
            session.setPassword(pwd);
            // 设置严格主机密钥检查为no
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(30000); // 30秒超时
            session.connect();
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();
            log.info("SFTP连接成功，host={}, port={}, user={}", host, port, user);
            return new SftpConnectionImpl(sftp, session);
        } catch (Exception e) {
            log.error("创建SFTP链接失败，host={}, port={}, user={}", host, port, user, e);
            // 清理资源
            if (sftp != null && sftp.isConnected()) {
                sftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
            throw new RuntimeException("连接SFTP服务器失败: " + e.getMessage(), e);
        }
    }

    /**
     * 关旧版ftp连接（已废弃，保留用于兼容）
     * @deprecated 使用 FtpConnection.disconnect() 代替
     **/
    @Deprecated
    private static void close(Ftp ftp) {
        try {
            if (!Objects.isNull(ftp)) {
                ftp.close();//断开连接
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取FTP/SFTP文件列表
     * 根据端口自动选择协议：21使用FTP，22使用SFTP
     **/
    public static List<String> getFileList(String host, Integer port, String user, String pwd, Integer type, String ftpPath) {
        FtpConnection conn = createFtpConnection(host, port, user, pwd, type);
        List<String> files = conn.listFiles(ftpPath);
        conn.disconnect();
        return files;
    }

    /**
     * FTP/SFTP文件下载
     * 根据端口自动选择协议：21使用FTP，22使用SFTP
     **/
    public static void download(String host, Integer port, String user, String pwd, Integer type, String fileName, String localPath) {
        FtpConnection conn = null;
        try {
            conn = createFtpConnection(host, port, user, pwd, type);

            //新建文件
            String filePath = localPath + "/" + fileName;
            File file = new File(filePath);
            if (!file.exists()) {
                if (!Objects.isNull(file.getParentFile()) && !file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                try {
                    file.createNewFile();
                } catch (IOException ex) {
                    log.error("创建文件异常：{}", fileName);
                }
            }
            conn.download(".", fileName, file);
        } catch (Exception e) {
            log.error("下载文件异常：{}", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
