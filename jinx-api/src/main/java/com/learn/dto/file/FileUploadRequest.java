package com.learn.dto.file;


import lombok.Data;

import java.io.InputStream;


@Data
public class FileUploadRequest {

    /**
     * 使用哪个上传服务，不传默认使用default
     */
    private String name = "default";

    /**
     * 文件唯一标识，支持目录形式：exampledir/exampleobject.txt
     */
    private String fileKey;

    private InputStream inputStream;


}
