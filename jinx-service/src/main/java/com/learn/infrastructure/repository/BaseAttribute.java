package com.learn.infrastructure.repository;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.annotation.JSONField;
import org.apache.commons.lang3.StringUtils;

import java.beans.Transient;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yujintao
 * @date 2025/5/8
 */

public class BaseAttribute {

    private String attributes;

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public String getAttribute(String key) {
        try {
            if (StringUtils.isNotBlank(this.attributes)) {
                JSONObject obj = JSON.parseObject(this.attributes, JSONObject.class);
                if (obj != null) {
                    return obj.getString(key);
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public <T> T getAttribute(String key, Class<T> clazz) {
        String attribute = getAttribute(key);
        if (StringUtils.isBlank(attribute)) {
            return null;
        }

        if(String.class.equals(clazz)) {
            return (T) attribute;
        }

        return JSON.parseObject(attribute, clazz);
    }

    public <T> T getAttribute(String key, TypeReference<T> typeReference) {
        String attribute = getAttribute(key);
        if (StringUtils.isBlank(attribute)) {
            return null;
        }
        return JSON.parseObject(attribute, typeReference);
    }

    public void putAttribute(String key, Object value) {
        if (value == null) {
            return;
        }
        if (getAttributeMap() == null) {
            setAttributeMap(new HashMap<>());
        }
        Map<String, Object> m = JSON.parseObject(getAttributes(), new TypeReference<Map<String, Object>>() {
        });
        m.put(key, value);
        setAttributes(JSON.toJSONString(m));
    }

    public void removeAttribute(String key) {
        if (StringUtils.isBlank(this.attributes)) {
            return;
        }

        Map<String, Object> m = JSON.parseObject(getAttributes(), new TypeReference<Map<String, Object>>() {
        });
        m.remove(key);
        setAttributes(JSON.toJSONString(m));
    }

    @JSONField(serialize = false)
    @Transient
    public Map<String, Object> getAttributeMap() {
        if (StringUtils.isBlank(getAttributes())) {
            return null;
        }
        return JSON.parseObject(getAttributes(), new TypeReference<Map<String, Object>>() {
        });
    }

    public void setAttributeMap(Map<String, Object> attributeMap) {
        if (attributeMap == null) {
            return;
        }
        setAttributes(JSON.toJSONString(attributeMap));
    }

    public void addAttributes(Map<String, Object> attributeMap) {
        if (attributeMap == null || attributeMap.isEmpty()) {
            return;
        }
        if (StringUtils.isBlank(attributes)) {
            setAttributeMap(attributeMap);
        }
        for (Map.Entry<String, Object> entry : attributeMap.entrySet()) {
            putAttribute(entry.getKey(), entry.getValue());
        }
    }
}
