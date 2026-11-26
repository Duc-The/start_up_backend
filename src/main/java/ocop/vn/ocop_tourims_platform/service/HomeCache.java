package ocop.vn.ocop_tourims_platform.service;

import java.time.Duration;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeCache {
  private final StringRedisTemplate redis;
  private final ObjectMapper mapper;

  public <T> T get(String key, Class<T> type, Duration ttl, Supplier<T> loader) {
    try {
      String generation = redis.opsForValue().get("home:cache-generation");
      key = key + ":g" + (generation == null ? "0" : generation);
    } catch (RuntimeException e) {
      return loader.get();
    }
    try {
      String value = redis.opsForValue().get(key);
      if (value != null) return mapper.readValue(value, type);
    } catch (RuntimeException e) {
      log.warn("Không đọc được cache home, chuyển sang dữ liệu gốc");
    }
    T value = loader.get();
    try {
      redis.opsForValue().set(key, mapper.writeValueAsString(value), ttl);
    } catch (RuntimeException e) {
      log.warn("Không ghi được cache home");
    }
    return value;
  }

  public void invalidate() {
    try {
      redis.opsForValue().increment("home:cache-generation");
    } catch (RuntimeException e) {
      log.warn("Không làm mới được cache home, dữ liệu sẽ được cập nhật khi hết TTL");
    }
  }
}
