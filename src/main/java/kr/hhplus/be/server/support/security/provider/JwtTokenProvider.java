package kr.hhplus.be.server.support.security.provider;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long validityInMilliseconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration:3600000}") long validityInMilliseconds) {
        // SecretKey 생성 (최신 버전)
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validityInMilliseconds = validityInMilliseconds;
    }

    /**
     * JWT 토큰 생성
     */
    public String createToken(String accountId, Long userId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
            .subject(accountId)  // setSubject 대신 subject 사용
            .claim("userId", userId)
            .issuedAt(now)
            .expiration(validity)  // setExpiration 대신 expiration 사용
            .signWith(key)  // SignatureAlgorithm 불필요
            .compact();
    }

    /**
     * 토큰에서 accountId 추출
     */
    public String getAccountId(String token) {
        return Jwts.parser()  // parserBuilder 대신 parser 사용
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)  // parseClaimsJws 대신 parseSignedClaims
            .getPayload()
            .getSubject();
    }

    /**
     * 토큰에서 userId 추출
     */
    public Long getUserId(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        return claims.get("userId", Long.class);
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}