package com.learn.service.third;

import com.learn.service.third.bo.ThirdCropInfoBO;

/**
 * 三方企业信息服务接口
 */
public interface ThirdCropInfoService {
    
    /**
     * 根据三方ID查询三方企业信息
     *
     * @param thirdId 三方唯一ID
     * @return 三方企业信息业务对象
     */
    ThirdCropInfoBO queryByThirdId(String thirdId);
}
