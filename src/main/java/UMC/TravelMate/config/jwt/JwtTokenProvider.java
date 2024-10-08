package UMC.TravelMate.config.jwt;

import UMC.TravelMate.domain.member.entity.Role;
import UMC.TravelMate.global.exception.CustomApiException;
import UMC.TravelMate.global.exception.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {
    private static final String AUTHORITIES_KEY = "auth";
    private static final String BEARER_TYPE = "bearer";
    private static final Long ACCESS_TOKEN_EXPIRE_LENGTH = 60L * 60 * 24 * 1000; // 1 Day
    private static final Long REFRESH_TOKEN_EXPIRE_LENGTH = 60L * 60 * 24 * 15 * 1000; // 15 Days
    private final Key key;

    public JwtTokenProvider(@Value("${jwt.secret-key}") String secretKey) {
        byte[] secretByteKey = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(secretByteKey);
    }

    public JwtToken generateToken(String memberId) {

        long now = new Date().getTime();

        // Access Token 생성
        String accessToken = Jwts.builder()
                .setSubject(memberId) // payload "sub" : name"
                .claim(AUTHORITIES_KEY, Role.USER) // payload "auth" : "USER"
                .setExpiration(new Date(now + ACCESS_TOKEN_EXPIRE_LENGTH))
                .signWith(key, SignatureAlgorithm.HS512) // header "alg" : 해싱 알고리즘 HS512
                .compact();

        // Refresh Token 생성
        String refreshToken = Jwts.builder()
                .setExpiration(new Date(now + REFRESH_TOKEN_EXPIRE_LENGTH))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

        return JwtToken.builder()
                .grantType(BEARER_TYPE)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public Authentication getAuthentication(String accessToken) {
        // 토큰 복호화
        Claims claims = parseClaims(accessToken);

        if(claims.get("auth") == null) {
            throw new CustomApiException(ErrorCode.UNAUTHORIZED_JWT_TOKEN);
        }

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("auth").toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .toList();

        UserDetails principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);

    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT Token", e);
            throw new CustomApiException(ErrorCode.INVALID_JWT_TOKEN);
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT Token", e);
            throw new CustomApiException(ErrorCode.EXPIRED_JWT_TOKEN);
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token", e);
            throw new CustomApiException(ErrorCode.UNSUPPORTED_JWT_TOKEN);
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty", e);
            throw new CustomApiException(ErrorCode.EMPTY_JWT_CLAIMS);
        }
    }

    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(accessToken)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            throw new CustomApiException(ErrorCode.INVALID_JWT_TOKEN);
        } catch (UnsupportedJwtException e) {
            throw new CustomApiException(ErrorCode.UNSUPPORTED_JWT_TOKEN);
        } catch (IllegalArgumentException e) {
            throw new CustomApiException(ErrorCode.EMPTY_JWT_CLAIMS);
        }
    }
}