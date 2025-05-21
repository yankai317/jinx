package com.learn.dto.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 加密url的内容
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileEncryptMeta {

    private String name;
    
    private String bucket;

    private String fileKey;
}
