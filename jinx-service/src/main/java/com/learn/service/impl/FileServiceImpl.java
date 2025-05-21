package com.learn.service.impl;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.auth.sts.AssumeRoleRequest;
import com.aliyuncs.auth.sts.AssumeRoleResponse;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.learn.common.util.FileUrlUtil;
import com.learn.config.OssConfig;
import com.learn.dto.file.*;
import com.learn.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

/**
 * 文件服务实现类
 *
 * @author yujintao
 * @date 2025/1/9
 */
@Slf4j
@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private OssConfig config;

    private OSS oss;

    @Override
    public FileUploadResult upload(FileUploadRequest request) {
        Assert.notNull(request, "request can't be null");
        Assert.notNull(request.getInputStream(), "file inputStream can't be null");
        Assert.isTrue(StringUtils.isNotBlank(request.getFileKey()), "fileKey can't be null");
        String fileKey = request.getFileKey();
        OSS client = getOssClient();
        String bucket = config.getBucket();
        client.putObject(bucket, request.getFileKey(), request.getInputStream());
//        FileEncryptMeta meta = new FileEncryptMeta(request.getName(), bucket, fileKey);
//        URL url = client.generatePresignedUrl(bucket, meta.getFileKey(),
//                DateUtils.addSeconds(new Date(), 60 * 60 * 24 * 30));
        // todo
        String urlStr = String.format("https://%s.%s/%s", bucket, config.getEndpoint(), fileKey);
        FileUploadResult fileUploadResult = new FileUploadResult();
        fileUploadResult.setUrl(urlStr);
        return fileUploadResult;
    }

    @Override
    public FileDecryptUrlResult getUrl(String urlKey) {
        String[] strings = FileUrlUtil.splitKeyAndValue(urlKey);
        FileEncryptMeta meta = FileUrlUtil.decodeUrl(strings[0], strings[1]);
        OSS client = getOssClient();
        Assert.notNull(client, String.format("文件服务%s不存在，请检查mos.file.name配置", meta.getName()));
        FileDecryptUrlResult fileDecryptUrlResult = new FileDecryptUrlResult();
        String bucket = meta.getBucket();
        URL url = client.generatePresignedUrl(bucket, meta.getFileKey(),
                DateUtils.addSeconds(new Date(), 60 * 60 * 24 * 30));
        String urlStr = url.toString();
        // 将http处理为https
        if (urlStr.startsWith("http:")) {
            urlStr = "https" + urlStr.substring(4);
        }
        fileDecryptUrlResult.setUrl(urlStr);
        return fileDecryptUrlResult;
    }

    @Override
    public FileDownloadResult download(FileDownloadRequest request) {
        OSS client = getOssClient();
        OSSObject ossObject = null;
        if (StringUtils.isNotEmpty(request.getFileUrl())) {
            try {
                ossObject = client.getObject(new URL(request.getFileUrl()), null);
            } catch (MalformedURLException e) {
                log.error("fileUrl格式不正确:{}", request.getFileUrl());
            }
        } else {
            Assert.notNull(request, "request can't be null");
            String fileKey = request.getFileKey();
            String bucket = config.getBucket();
            ossObject = client.getObject(bucket, fileKey);
        }
        FileDownloadResult fileDownloadResult = new FileDownloadResult();
        if (ossObject != null) {
            fileDownloadResult.setContent(ossObject.getObjectContent());
        }
        return fileDownloadResult;
    }

    @Override
    public void getVideoDuration(String url, String key) {
        OSS client = getOssClient();
        try {
            // 构建视频信息提取的处理指令。
            GetObjectRequest getObjectRequest = new GetObjectRequest(config.getBucket(), key);
            getObjectRequest.setProcess("video/info");

            // 使用getObject方法，并通过process参数传入处理指令。
            OSSObject ossObject = client.getObject(getObjectRequest);

            // 读取并打印视频信息。
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = ossObject.getObjectContent().read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            String videoInfo = baos.toString("UTF-8");
            System.out.println("Video Info:");
            System.out.println(videoInfo);
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            // 关闭OSSClient。
            client.shutdown();
        }
    }


    public OSS getOssClient() {
        if (oss != null) {
            return oss;
        }
        String endpoint = config.getEndpoint();
        String ak = config.getAk();
        String sk = config.getSk();
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        conf.setIdleConnectionTime(60 * 60 * 1000); //默认空闲时间延长到1小时，防止频繁重复重连
        oss = new OSSClientBuilder().build(endpoint, ak, sk, conf);
        log.info("创建oss客户端成功, endpoint={}", endpoint);
        return oss;
    }

}
