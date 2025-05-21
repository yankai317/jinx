package com.learn.infrastructure.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 首页轮播图实体类
 */
@Data
@TableName("banner")
public class Banner {
    
    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 轮播图标题
     */
    private String title;
    
    /**
     * 轮播图片URL
     */
    private String imageUrl;
    
    /**
     * 链接URL
     */
    private String linkUrl;
    
    /**
     * 排序序号
     */
    private Integer sort;
    
    /**
     * 创建时间
     */
    private LocalDateTime gmtCreate;
    
    /**
     * 创建人id
     */
    private Long creatorId;
    
    /**
     * 创建人名称
     */
    private String creatorName;
    
    /**
     * 更新人id
     */
    private Long updaterId;
    
    /**
     * 更新人名称
     */
    private String updaterName;
    
    /**
     * 更新时间
     */
    private LocalDateTime gmtModified;
    
    /**
     * 扩展属性字段，内容为json
     */
    private String attributes;

    /**
     * @see
     */
    private String type;
    
    /**
     * 逻辑删除字段: 是否删除 1-已删 0-正常
     */
    @TableLogic
    private Integer isDel;
}
