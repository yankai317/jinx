package com.learn.service.third.bo;

import lombok.Data;
import java.util.Date;

/**
 * 三方企业信息业务对象
 */
@Data
public class ThirdCropInfoBO {
    /**
     * 自增id
     */
    private Long id;

    /**
     * 三方唯一id，比如钉钉就是企业crop_id
     */
    private String thirdId;

    /**
     * 第三方平台类型，如：dingtalk、feishu
     */
    private String thirdType;

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
    
    /**
     * 属性对象，由attributes字段转换而来
     */
    private ThirdCropAttributes attributes;
    
    /**
     * 三方企业属性对象
     */
    @Data
    public static class ThirdCropAttributes {
        /**
         * 机器人编码
         */
        private String robotCode;
        
        /**
         * 套件票据
         */
        private String suiteTicket;
        
        /**
         * 卡片模板ID
         */
        private String cardTemplateId;
    }
}
