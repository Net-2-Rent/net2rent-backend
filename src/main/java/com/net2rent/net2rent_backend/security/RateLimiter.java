package com.net2rent.net2rent_backend.security;

public interface RateLimiter {

    /**
    @param key email or IP
    @return true if it's blocked, false if they can still try
    */
    boolean isBlocked(String key);

    /**
    @param key email or IP
    */
    void registerFailure(String key);

    /**
    @param key email or IP
    */
    void reset(String key);
}