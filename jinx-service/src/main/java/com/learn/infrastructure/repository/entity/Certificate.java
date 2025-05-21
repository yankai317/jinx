package com.learn.infrastructure.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 证书表
 * @TableName certificate
 */
@TableName(value ="certificate")
@Data
public class Certificate {
    /**
     * 自增id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 证书名称
     */
    private String name;

    /**
     * 证书描述
     */
    private String description;

    /**
     * 证书模板URL
     */
    private String templateUrl;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 过期时间，null表示永不过期
     */
    private Date expireTime;

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
    private Date gmtModified;

    /**
     * 逻辑删除字段: 是否删除 1-已删 0-正常
     */
    private Integer isDel;
}
