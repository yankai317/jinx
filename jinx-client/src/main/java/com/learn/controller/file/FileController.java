package com.learn.controller.file;


import com.learn.dto.common.ApiResponse;
import com.learn.dto.file.FileUploadRequest;
import com.learn.dto.file.FileUploadResult;
import com.learn.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * 文件上传控制器
 *
 */
@RestController
@RequestMapping("/api/file")
@CrossOrigin
@Slf4j
public class FileController {

    @Value("${file.upload.path:uploads}")
    private String uploadPath;

    @Value("${file.access.path:/uploads/}")
    private String accessPath;


    @Autowired
    private FileService fileService;

    @GetMapping("/test")
    public void test() {
        System.out.println("test");
        fileService.getVideoDuration("", "0739d369-c0b5-4387-9c20-bffa35ad4b5b.mp4");
    }


    /**
     * 上传文件
     *
     * @param file 上传的文件
     * @return 文件上传结果
     */
    @PostMapping("/upload")
    public ApiResponse<FileUploadResult> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ApiResponse.error("文件不能为空");
            }

            // 获取原始文件名和后缀
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            // 生成新的文件名：UUID + 原始后缀
            String newFileName = UUID.randomUUID() + extension;

            // 创建FileUploadRequest对象
            FileUploadRequest fileReq = new FileUploadRequest();
            fileReq.setName("default"); // 使用默认上传服务
            fileReq.setFileKey(newFileName);

            // 将MultipartFile转换为ByteArrayInputStream，避免流关闭问题
            byte[] fileBytes = file.getBytes();
            InputStream inputStream = new ByteArrayInputStream(fileBytes);
            fileReq.setInputStream(inputStream);

            FileUploadResult result = fileService.upload(fileReq);
            return ApiResponse.success("上传成功", result);
        } catch (IOException e) {
            log.error("Error processing uploaded file", e);
            return ApiResponse.error("文件上传失败");
        } catch (Exception e) {
            log.error("Error upload files", e);
            return ApiResponse.error("文件上传失败");
        }
    }
}
