package com.learn.infrastructure.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.learn.constants.BizType;
import com.learn.infrastructure.repository.BaseAttribute;
import lombok.Data;

import java.util.Date;

/**
 * 用户证书表
 * @TableName user_certificate
 */
@TableName(value ="user_certificate")
@Data
public class UserCertificate extends BaseAttribute {
    /**
     * 自增id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 证书id
     */
    private Long certificateId;

    /**
     * 证书来源: 考试、培训、学习地图
     */
    private String sourceType;

    /**
     * 关联来源id
     */
    private Long sourceId;

    /**
     * 颁发时间
     */
    private Date issueTime;

    /**
     * 过期时间，null表示永不过期
     */
    private Date expireTime;

    /**
     * 证书编号
     */
    private String certificateNo;

    /**
     * 状态：0-有效，1-已过期，2-已撤销
     */
    private Integer status;

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
     * 更新时间
     */
    private Date gmtModified;

    /**
     * 逻辑删除字段: 是否删除 1-已删 0-正常
     */
    private Integer isDel;

    public String getSourceTypeName() {
        if (BizType.LEARNING_MAP.equals(this.getSourceType())) {
            return "学习地图";
        } else if (BizType.TRAIN.equals(this.getSourceType())) {
            return "培训";
        } else if (BizType.MAP_STAGE.equals(this.getSourceType())) {
            return "学习地图阶段";
        }
        return "";
    }
}
