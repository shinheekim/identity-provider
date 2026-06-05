package com.pj.login.common.security.refresh.redis;

import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

final class RefreshTokenRedisScripts {

    static final RedisScript<Long> SAVE = RedisScript.of("""
            redis.call('HSET', KEYS[1],
                'userUuid', ARGV[1],
                'familyId', ARGV[2],
                'status', 'ACTIVE')
            redis.call('EXPIRE', KEYS[1], ARGV[3])
            redis.call('HSET', KEYS[2],
                'currentToken', KEYS[1],
                'status', 'ACTIVE')
            redis.call('EXPIRE', KEYS[2], ARGV[3])
            redis.call('SADD', KEYS[3], KEYS[1])
            redis.call('EXPIRE', KEYS[3], ARGV[3])
            return 1
            """, Long.class);

    static final RedisScript<List> ROTATE = RedisScript.of("""
            local status = redis.call('HGET', KEYS[1], 'status')
            if not status then
                return {'MISSING'}
            end

            local userUuid = redis.call('HGET', KEYS[1], 'userUuid')
            local familyId = redis.call('HGET', KEYS[1], 'familyId')
            if status ~= 'ACTIVE' then
                return {status, userUuid or '', familyId or ''}
            end

            local familyStatus = redis.call('HGET', KEYS[2], 'status')
            local currentToken = redis.call('HGET', KEYS[2], 'currentToken')
            if familyStatus ~= 'ACTIVE' or currentToken ~= KEYS[1] then
                return {'REVOKED', userUuid or '', familyId or ''}
            end

            redis.call('HSET', KEYS[1],
                'status', 'ROTATED',
                'rotatedTo', KEYS[4])
            redis.call('EXPIRE', KEYS[1], ARGV[1])
            redis.call('HSET', KEYS[4],
                'userUuid', userUuid,
                'familyId', familyId,
                'status', 'ACTIVE')
            redis.call('EXPIRE', KEYS[4], ARGV[1])
            redis.call('SADD', KEYS[3], KEYS[4])
            redis.call('EXPIRE', KEYS[3], ARGV[1])
            redis.call('HSET', KEYS[2],
                'currentToken', KEYS[4],
                'status', 'ACTIVE')
            redis.call('EXPIRE', KEYS[2], ARGV[1])
            return {'ACTIVE', userUuid, familyId}
            """, List.class);

    static final RedisScript<Long> REVOKE_FAMILY = RedisScript.of("""
            redis.call('HSET', KEYS[1], 'status', 'REVOKED')
            local tokenKeys = redis.call('SMEMBERS', KEYS[2])
            for _, tokenKey in ipairs(tokenKeys) do
                redis.call('HSET', tokenKey, 'status', 'REVOKED')
            end
            return 1
            """, Long.class);

    private RefreshTokenRedisScripts() {
    }
}
