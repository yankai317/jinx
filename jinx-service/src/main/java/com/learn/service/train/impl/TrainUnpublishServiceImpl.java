package com.learn.service.train.impl;

import com.learn.common.dto.UserTokenInfo;
import com.learn.common.exception.CommonException;
import com.learn.common.util.UserContextHolder;
import com.learn.constants.BizType;
import com.learn.infrastructure.repository.entity.OperationLog;
import com.learn.infrastructure.repository.entity.Train;
import com.learn.infrastructure.repository.mapper.TrainMapper;
import com.learn.infrastructure.repository.mapper.TrainOperationLogMapper;
import com.learn.service.CommonRangeInterface;
import com.learn.service.train.TrainUnpublishService;
import com.learn.service.user.UserInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 培训取消发布服务实现类
 */
@Service
@Slf4j
public class TrainUnpublishServiceImpl implements TrainUnpublishService {

    @Autowired
    private TrainMapper trainMapper;

    /**
     * 取消发布培训
     *
     * @param id 培训ID
     * @return 是否取消发布成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unpublishTrain(Long id, UserTokenInfo userInfo) {
        // 参数校验
        if (id == null || id <= 0) {
            throw new CommonException("培训ID不能为空或小于等于0");
        }

        // 获取当前用户信息
        Long userId = userInfo.getUserId();
        String userName = userInfo.getName();

        // 获取培训信息
        Train train = trainMapper.selectById(id);
        if (train == null || train.getIsDel() == 1) {
            throw new CommonException("培训不存在或已删除");
        }

        // 检查培训状态
        if (!"published".equals(train.getStatus())) {
            throw new CommonException("只有已发布状态的培训才能取消发布");
        }

        // 更新培训状态为草稿
        Train updateTrain = new Train();
        updateTrain.setId(id);
        updateTrain.setStatus("draft");
        updateTrain.setUpdaterId(userId);
        updateTrain.setUpdaterName(userName);
        updateTrain.setGmtModified(new Date());
        trainMapper.updateById(updateTrain);

        return true;
    }

}
