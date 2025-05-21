package com.learn.common.util;

import com.learn.common.dto.UserTokenInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT工具类
 */
@Component
public class JwtUtil {

    // 密钥
    private final SecretKey secret;

    // token有效期（毫秒）
    private final long expiration = 1000 * 60 * 60 * 24; // 24小时

    public JwtUtil() {
        // 使用SHA-256生成安全的密钥
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest("11111".getBytes(StandardCharsets.UTF_8));
            this.secret = Keys.hmacShaKeyFor(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to initialize JWT secret key", e);
        }
    }

    /**
     * 从token中提取用户信息
     *
     * @param token JWT token
     * @return 用户信息对象，包含userId和corpId
     */
    public UserTokenInfo extractUserId(String token) {
        String userId = extractClaim(token, Claims::getSubject);
        String name = extractClaim(token, claims -> claims.get("username", String.class));
        String corpId = extractClaim(token, claims -> claims.get("corpId", String.class));
        
        return UserTokenInfo.builder()
                .userId(Long.valueOf(userId))
                .name(name)
                .corpId(corpId)
                .build();
    }

    /**
     * 从token中提取用户名
     *
     * @param token JWT token
     * @return 用户名
     */
    public String extractUsername(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("username", String.class);
    }

    /**
     * 从token中提取过期时间
     *
     * @param token JWT token
     * @return 过期时间
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 从token中提取指定的claim
     *
     * @param token          JWT token
     * @param claimsResolver claim解析函数
     * @param <T>            claim类型
     * @return claim值
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 从token中提取所有的claims
     *
     * @param token JWT token
     * @return claims
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secret)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }



    /**
     * 生成token
     *
     * @param userId 用户ID
     * @param username 用户名
     * @return JWT token
     */
    public String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        return createToken(claims, String.valueOf(userId));
    }
    
    /**
     * 生成token，包含企业ID
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param corpId 企业ID
     * @return JWT token
     */
    public String generateToken(Long userId, String username, String corpId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("corpId", corpId);
        return createToken(claims, String.valueOf(userId));
    }

    /**
     * 创建token
     *
     * @param claims  claims
     * @param subject 主题（用户ID）
     * @return JWT token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secret)
                .compact();
    }


}
