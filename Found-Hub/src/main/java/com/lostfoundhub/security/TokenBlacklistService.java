package com.lostfoundhub.security;

import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWTs are stateless, so "logout" works by remembering the token id (jti) until the token
 * would have expired anyway. Kept in memory: after an application restart, old tokens that
 * were logged out would be valid again until they expire (fine for a college project;
 * use Redis or a database table in production).
 */
@Service
public class TokenBlacklistService {

    private final Map<String, Date> revoked = new ConcurrentHashMap<>();

    public void revoke(String jti, Date expiresAt) {
        purgeExpired();
        revoked.put(jti, expiresAt);
    }

    public boolean isRevoked(String jti) {
        return jti != null && revoked.containsKey(jti);
    }

    private void purgeExpired() {
        Date now = new Date();
        revoked.entrySet().removeIf(entry -> entry.getValue().before(now));
    }
}
