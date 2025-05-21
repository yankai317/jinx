package com.learn.dto.file;

import lombok.Data;

@Data
public class FileDecryptUrlRequest {

    private String urlKey;

    private boolean needCdn;

    private int expire = 60 * 60 * 24 * 30;

}
