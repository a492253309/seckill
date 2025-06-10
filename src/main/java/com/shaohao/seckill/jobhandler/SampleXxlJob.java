package com.shaohao.seckill.jobhandler;

import com.shaohao.seckill.service.DeadLetterLogService;
import com.shaohao.seckill.service.OrderService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * XxlJob开发示例（Bean模式）
 * <p>
 * 开发步骤：
 * 1、任务开发：在Spring Bean实例中，开发Job方法；
 * 2、注解配置：为Job方法添加注解 "@XxlJob(value="自定义jobhandler名称", init = "JobHandler初始化方法", destroy = "JobHandler销毁方法")"，注解value值对应的是调度中心新建任务的JobHandler属性的值。
 * 3、执行日志：需要通过 "XxlJobHelper.log" 打印执行日志；
 * 4、任务结果：默认任务结果为 "成功" 状态，不需要主动设置；如有诉求，比如设置任务结果为失败，可以通过 "XxlJobHelper.handleFail/handleSuccess" 自主设置任务结果；
 *
 * @Author shaohao
 * @Date 2024/2/15
 **/
@Slf4j
@AllArgsConstructor
@Component
public class SampleXxlJob {


    private DeadLetterLogService deadLetterLogService;
    private OrderService orderService;


    /**
     * 秒杀死信队列预警定时任务
     */
    @XxlJob("checkDeadLetter")
    public void checkDeadLetter() {
        try {
            XxlJobHelper.log("开始执行秒杀死信队列预警定时任务");
            deadLetterLogService.checkCount();
            XxlJobHelper.log("秒杀死信队列预警任务执行成功");
        } catch (Exception e) {
            XxlJobHelper.log("秒杀死信队列预警任务失败，错误: {}", e.getMessage());
            XxlJobHelper.handleFail();
        }
    }


    /**
     * 取消超时订单 定时任务
     */
    @XxlJob("cancelTimeoutOrders")
    public void cancelTimeoutOrders() {
        try {
            XxlJobHelper.log("开始执行取消超时订单任务");
            orderService.cancelTimeoutOrders();
            XxlJobHelper.log("取消超时订单任务执行成功");
        } catch (Exception e) {
            XxlJobHelper.log("取消超时订单任务失败，错误: {}", e.getMessage());
            XxlJobHelper.handleFail();
        }
    }

}
