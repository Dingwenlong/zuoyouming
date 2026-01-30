package com.library.seat.common.utils;

import com.alibaba.fastjson.JSON;
import java.util.List;

public class JsonUtils {

    public static <T> List<T> parseArray(String text, Class<T> clazz) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        return JSON.parseArray(text, clazz);
    }
    
    public static String toJsonString(Object object) {
        return JSON.toJSONString(object);
    }
}
