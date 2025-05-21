package com.learn.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

/**
 * @author yujintao
 * @date 2025/4/25
 */
public class JinxDateUtil {

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static String formatDateToString(Date date) {
        if (Objects.isNull(date)) {
            return "";
        }
        // 定义日期格式
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        // 格式化日期
        return dateFormat.format(date);
    }
}
