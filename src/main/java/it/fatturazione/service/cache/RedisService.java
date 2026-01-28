package it.fatturazione.service.cache;

import java.util.concurrent.TimeUnit;

public interface RedisService {
    void save(String key, Object value);
    void saveWithExpiration(String key, Object value, long timeout, TimeUnit unit);
    Object get(String key);
    boolean exists(String key);
    boolean delete(String key);
}
