package com.finanscore.query.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

//Guarda temporalmente consultas frecuentes y reduce accesos a PostgreSQL.
//Primero busco en Redis; si no está, voy al read model.

@Component
public class RedisQueryCache {
	private final StringRedisTemplate redis;
	private final ObjectMapper om;
	private final Duration ttl;
	private final Counter hits, misses, errors, writes, evictions;

	public RedisQueryCache(StringRedisTemplate redis, ObjectMapper om, MeterRegistry meter,
			@Value("${app.cache.ttl-seconds:60}") long ttlSeconds) {
		this.redis = redis;
		this.om = om;
		this.ttl = Duration.ofSeconds(Math.max(1, ttlSeconds));
		this.hits = Counter.builder("query_cache_hits_total").description("Redis cache hits").register(meter);
		this.misses = Counter.builder("query_cache_misses_total").description("Redis cache misses").register(meter);
		this.errors = Counter.builder("query_cache_errors_total")
				.description("Redis cache errors with PostgreSQL fallback").register(meter);
		this.writes = Counter.builder("query_cache_writes_total").description("Redis cache writes").register(meter);
		this.evictions = Counter.builder("query_cache_evictions_total").description("Redis cache evictions")
				.register(meter);
	}

	public <T> Optional<T> get(String key, Class<T> type) {
		try {
			String raw = redis.opsForValue().get(key);
			if (raw == null) {
				misses.increment();
				return Optional.empty();
			}
			hits.increment();
			return Optional.of(om.readValue(raw, type));
		} catch (Exception ex) {
			errors.increment();
			return Optional.empty();
		}
	}

	public <T> Optional<T> get(String key, TypeReference<T> type) {
		try {
			String raw = redis.opsForValue().get(key);
			if (raw == null) {
				misses.increment();
				return Optional.empty();
			}
			hits.increment();
			return Optional.of(om.readValue(raw, type));
		} catch (Exception ex) {
			errors.increment();
			return Optional.empty();
		}
	}

	public void put(String key, Object value) {
		try {
			redis.opsForValue().set(key, om.writeValueAsString(value), ttl);
			writes.increment();
		} catch (Exception ex) {
			errors.increment();
		}
	}

	public void evict(String key) {
		try {
			Boolean deleted = redis.delete(key);
			if (Boolean.TRUE.equals(deleted))
				evictions.increment();
		} catch (Exception ex) {
			errors.increment();
		}
	}
}
