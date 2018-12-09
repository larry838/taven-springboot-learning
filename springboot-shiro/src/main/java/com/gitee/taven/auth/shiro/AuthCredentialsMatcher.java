package com.gitee.taven.auth.shiro;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.ExcessiveAttemptsException;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.util.ByteSource;

import java.util.concurrent.atomic.AtomicInteger;

public class AuthCredentialsMatcher extends HashedCredentialsMatcher {

    // 记录当前用户密码输入错误的次数
    // 使用AtomicInteger 确保获得线程安全的数值.如果使用负载均衡，需缓存在redis中
    private Cache<String, AtomicInteger> passwordRetryCache;

    public AuthCredentialsMatcher(CacheManager cacheManager) {
        passwordRetryCache = cacheManager.getCache("passwordRetryCache");
    }

    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
        String userName = (String) token.getPrincipal();
        AtomicInteger retryCount = passwordRetryCache.get(userName);

        if (null == retryCount) {
            retryCount = new AtomicInteger(0);
            passwordRetryCache.put(userName,retryCount);
        }

        if (retryCount.incrementAndGet() > 5) {
            throw new ExcessiveAttemptsException();
        }

        boolean matches = super.doCredentialsMatch(token, info);

        if (matches){
            passwordRetryCache.remove(userName);
        }

        return matches;
    }

    public static void main(String[] args) {
        // shiro 密码加密方法, 默认会使用 token 中的 username 作为 salt
        // hashAlgorithmName 与 hashIterations 我们在配置 @Bean CredentialsMatcher 时可以设置 保持一致即可
        String hashAlgorithmName = "MD5";
        int hashIterations = 1024;
        String username = "taven";
        String password = "6666";
        ByteSource salt = ByteSource.Util.bytes(username);// HashedCredentialsMatcher 默认使用 username 作为salt
        Object obj = new SimpleHash(hashAlgorithmName, password, salt, hashIterations);
        System.out.println(obj);
    }
}