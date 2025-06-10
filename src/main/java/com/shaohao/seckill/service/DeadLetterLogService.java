package com.shaohao.seckill.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shaohao.seckill.entity.DeadLetterLog;
import com.shaohao.seckill.enums.OrderErrorCode;
import com.shaohao.seckill.mapper.DeadLetterLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeadLetterLogService extends ServiceImpl<DeadLetterLogMapper, DeadLetterLog> {


    public long checkCount() {
        long conut = baseMapper.countByErrorCodeNotOptimisticLockFailed();
        if (conut > 10) {
            sendMsg();
        }
        return conut;
    }

    public void sendMsg() {

    }

}
