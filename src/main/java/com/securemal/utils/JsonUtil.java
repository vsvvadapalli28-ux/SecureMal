package com.securemal.utils;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class JsonUtil {
    private static final Gson gson = new Gson();

    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    public static List<Map<String, String>> parseTimelineArray(String timelineJson) {
        if (timelineJson == null || timelineJson.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Type listType = new TypeToken<List<Map<String, String>>>() {}.getType();
            List<Map<String, String>> result = gson.fromJson(timelineJson, listType);
            if (result == null) {
                return Collections.emptyList();
            }
            return result.stream()
                .distinct()
                .collect(Collectors.toList());
        } catch (RuntimeException e) {
            return Collections.emptyList();
        }
    }
}
