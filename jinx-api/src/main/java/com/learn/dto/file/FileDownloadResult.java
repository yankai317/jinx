package com.learn.dto.file;

import lombok.Data;

import java.io.InputStream;

/**
 * @author qiujunting
 * @date 2022/05/16
 */
@Data
public class FileDownloadResult {
    private InputStream content;
}
