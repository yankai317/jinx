package com.learn.service;

import com.learn.dto.file.*;

/**
 * @author yujintao
 * @date 2025/4/21
 */
public interface FileService {


    /**
     * 文件上传，获取加密后的urlKey
     * 前端页面调用完成后，将urlKey传过来。
     * 后端接口根据urlKey解析获得url
     * @see com.learn.service.FileService#getUrl(java.lang.String)
     * @param request
     * @return
     */
    FileUploadResult upload(FileUploadRequest request);

    /**
     * 文件上传后，根据加密urlKey获取可直接浏览器访问的url
     * @param urlKey
     * @return
     */
    FileDecryptUrlResult getUrl(String urlKey);

    /**
     * 用于后端服务，下载获取文件流
     * @param fileDownloadRequest
     * @return
     */
    FileDownloadResult download(FileDownloadRequest fileDownloadRequest);

    void getVideoDuration(String url, String key);
}
