package com.learn.dto.toc.share;

/**
 * 内容分享信息响应类
 */
public class ShareContentResponse {
    /**
     * 内容标题
     */
    private String title;
    
    /**
     * 内容描述
     */
    private String description;
    
    /**
     * 封面图URL
     */
    private String coverImage;
    
    /**
     * 分享链接
     */
    private String shareUrl;
    
    /**
     * 二维码URL
     */
    private String qrCodeUrl;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public String getShareUrl() {
        return shareUrl;
    }

    public void setShareUrl(String shareUrl) {
        this.shareUrl = shareUrl;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }
}
