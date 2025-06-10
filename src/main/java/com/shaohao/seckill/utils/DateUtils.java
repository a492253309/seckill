package com.shaohao.seckill.utils;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public class DateUtils {

    /**
     * 获取当前系统时间
     *
     * @return
     */
    public static int currentSecond() {
        return Long.valueOf(System.currentTimeMillis() / 1000).intValue();
    }

    /**
     * 时间戳（秒）加上小时
     *
     * @return
     */
    public static int SecondAddHours(Integer unixSecond,Integer hours) {
        Instant instant = Instant.ofEpochSecond(unixSecond);
        Duration duration = Duration.ofHours(hours);
        Instant result = instant.plus(duration);
        return (int) result.getEpochSecond();
    }

    /**
     * 时间戳（秒）加上分钟
     *
     * @return
     */
    public static int SecondAddMinutes(Integer unixSecond,Integer minutes) {
        // 将分钟转换为秒
        int times = minutes * 60;
        return unixSecond+times;
    }


    /**
     * 时间戳（秒）转时间
     *
     * @return
     */
    public static String getStringDateByUnix(Integer unixSecond) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        if (unixSecond != null) {
            date.setTime(unixSecond.longValue() * 1000L);
        }
        return sdf.format(date);
    }
}
