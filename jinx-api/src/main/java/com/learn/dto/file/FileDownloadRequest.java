package com.learn.dto.file;


import lombok.Data;

/**
 * @author qiujunting
 * @date 2022/05/16
 */
@Data
public class FileDownloadRequest {

    /**
     * 如果不传，则拿上传服务的第一个bucket
     */
    private String bucket;

    private String fileKey;

    /**
     * 如果只传 fileUrl，则按fileUrl下载
     */
    private String fileUrl;
}
