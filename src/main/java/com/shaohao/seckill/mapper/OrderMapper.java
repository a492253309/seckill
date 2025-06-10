package com.shaohao.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.shaohao.seckill.entity.Order;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 订单表_Order Mapper 接口
 * </p>
 *
 * @author shaohao
 * @since 2023-07-03
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 查询超时未支付订单，按分片条件过滤
     *
     * @param time  超时时间阈值
     * @param total 总分片数
     * @param index 当前分片索引
     * @return 符合条件的订单列表
     */
    @Select("SELECT order_Id FROM order WHERE status = 'UNPAY' AND create_time < #{time} AND order_id % #{total} = #{index}")
    List<String> checkUnPay(@Param("time") LocalDateTime time, @Param("total") int total, @Param("index") int index);

    @Update("<script>UPDATE `order` SET status = #{status} WHERE order_id IN " +
            "<foreach collection='orderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "AND status = 'UNPAY'</script>")
    int batchUpdateStatus(@Param("orderIds") List<String> orderIds, @Param("status") String status);
}


