package com.learn.dto.file;

import lombok.Data;

import java.io.Serializable;

@Data
public class FileContentResponse implements Serializable {

    private String content;

    private String message;
}
