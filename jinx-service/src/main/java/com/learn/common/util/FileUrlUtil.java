/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2018 All Rights Reserved.
 */
package com.learn.common.util;

import com.alibaba.fastjson.JSON;
import com.learn.dto.file.FileEncryptMeta;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.SecureRandom;

@Slf4j
public class FileUrlUtil {
    public static final String ALGORITHM = "AES";
    public static final String RANDOM_ALGORITHM = "SHA1PRNG";

    public static final String SPLIT_CHARTER = "%$";

    public static final String SPLIT_CHARTER_T = "\\%\\$";

    public static Cipher cipher;

    static {
        try {
            cipher = Cipher.getInstance(ALGORITHM);
        } catch (Exception e) {
            log.error("decode rule create failed", e);
        }
    }

    public static FileEncryptMeta decodeUrl(String key, String value) {
        return JSON.parseObject(decode(key, value), FileEncryptMeta.class);
    }

    /**
     * 解密数据
     *
     * @param cipherKey
     * @param cipherValue
     * @return
     */
    public static String decode(String cipherKey, String cipherValue) {
        try {
            if (StringUtils.isBlank(cipherKey)) {
                throw new RuntimeException("decode key can not be null");
            }
            byte[] key = cipherKey.getBytes();
            BigInteger n = new BigInteger(cipherValue, 16);
            byte[] encoding = n.toByteArray();
            SecretKey keySpec = generateKey(key);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decode = cipher.doFinal(encoding);
            return URLDecoder.decode(new String(decode), "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("decode database config error", e);
        }

    }

    /**
     * 加密数据
     *
     * @param encKey
     * @param value
     * @return
     */
    public static String encode(String encKey, String value) {
        try {
            value = URLEncoder.encode(value, "UTF-8");
            if (StringUtils.isBlank(encKey)) {
                throw new RuntimeException("encode key can not be null");
            }
            byte[] key = encKey.getBytes();
            SecretKey keySpec = generateKey(key);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encoding = cipher.doFinal(value.getBytes());
            BigInteger n = new BigInteger(encoding);
            return n.toString(16);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String encode(FileEncryptMeta fileEncryptMeta) {
        return encode(fileEncryptMeta.getName(), JSON.toJSONString(fileEncryptMeta));
    }

    public static String appendKeyAndValue(String key, String value) {
        return key + SPLIT_CHARTER + value;
    }

    public static String[] splitKeyAndValue(String keyAndValue) {
        return keyAndValue.split(SPLIT_CHARTER_T);
    }

    /**
     * 生成密钥对象
     */
    private static SecretKey generateKey(byte[] key) throws Exception {
        SecureRandom random = SecureRandom.getInstance(RANDOM_ALGORITHM);
        random.setSeed(key);
        KeyGenerator gen = KeyGenerator.getInstance(ALGORITHM);
        gen.init(128, random);
        return gen.generateKey();
    }
}
