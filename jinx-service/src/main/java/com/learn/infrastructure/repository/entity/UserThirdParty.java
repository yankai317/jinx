package com.learn.infrastructure.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户第三方授权关联表
 * @TableName user_third_party
 */
@TableName(value ="user_third_party")
@Data
public class UserThirdParty {
    /**
     * 自增id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID，关联user表
     */
    private Long userId;

    /**
     * 第三方平台类型，如：dingtalk、feishu
     */
    private String thirdPartyType;

    /**
     * 第三方平台用户唯一标识
     */
    private String thirdPartyUserId;

    /**
     * 第三方平台用户名
     */
    private String thirdPartyUsername;

    /**
     * 创建时间
     */
    private Date gmtCreate;

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
     * 扩展属性
     */
    private String attributes;

    /**
     * 更新时间
     */
    private Date gmtModified;

    /**
     * 逻辑删除字段: 是否删除 1-已删 0-正常
     */
    private Integer isDel;
}
