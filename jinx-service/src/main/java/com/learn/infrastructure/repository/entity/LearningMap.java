package com.learn.infrastructure.repository.entity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.Objects;

/**
 * 学习地图表
 * @TableName learning_map
 */
@TableName(value ="learning_map")
@Data
@Accessors(chain = true)
public class LearningMap {

    public static final String ATTRIBUTES_KEY_ENABLE_AUTO_ASSIGN = "ENABLE_AUTO_ASSIGN";
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 学习地图名称
     */
    private String name;

    /**
     * 封面图片地址
     */
    private String cover;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 发放学分规则：0-按整个学习地图给分 1-按每个学习阶段给分
     */
    private Integer creditRule;

    /**
     * 选修学分
     */
    private Integer electiveCredit;

    /**
     * 必修学分
     */
    private Integer requiredCredit;

    /**
     * 分类ids 1,2,3,4
     */
    private String categoryIds;

    /**
     * 发放证书规则：0-不发放证书 1-按整个学习地图发证 2-按每个学习阶段发证
     */
    private Integer certificateRule;

    /**
     * 证书ID
     */
    private Long certificateId;

    /**
     * 是否同步创建钉钉培训群：0-否 1-是
     */
    private Integer dingtalkGroup;

    /**
     * 钉钉群ID
     */
    private String dingtalkGroupId;

    /**
     * 开放开始时间
     */
    private Date startTime;

    /**
     * 开放结束时间
     */
    private Date endTime;

    /**
     * 解锁方式：0-按阶段和任务解锁 1-按阶段解锁 2-自由模式
     */
    private Integer unlockMode;

    /**
     * 地图主题：business-商务简约 tech-动感科技 farm-农场时光 chinese-中国元素 list-列表模式
     */
    private String theme;

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
     * 扩展属性字段，内容为json
     */
    private String attributes;

    /**
     * 更新时间
     */
    private Date gmtModified;

    /**
     * 逻辑删除: 0-正常 1-删除
     */
    private Integer isDel;
    /**
     * 添加属性
     * @param key
     * @param value
     */
    public void appendAttributes(String key, Object value) {
        if (StringUtils.isBlank(attributes)) {
            attributes = "{}";
        }
        JSONObject jsonObject = JSON.parseObject(attributes);
        jsonObject.put(key, value);
        attributes = jsonObject.toJSONString();
    }
    /**
     * 获取属性
     * @param key
     * @param <T>
     * @return
     */
    public <T> T getAttributes(String key) {
        if (StringUtils.isBlank(attributes)) {
            return null;
        }
        JSONObject jsonObject = JSON.parseObject(attributes);
        if (Objects.isNull(jsonObject.get(key))) {
            return null;
        }
        return jsonObject.getObject(key, (Class<T>) Object.class);
    }
}
