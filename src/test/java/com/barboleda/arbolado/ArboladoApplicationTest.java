package com.barboleda.arbolado;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheManager;

import com.barboleda.arbolado.domain.ArbolResponse;
import com.barboleda.arbolado.domain.SearchLimits;

/**
 * Application smoke: full context wires with lazy datastores, no servers needed.
 *
 * <p>The template is mocked so no Mongo is touched; the app itself performs no
 * index writes (read-only). Real Mongo coverage lives in the CI-gated
 * {@code GeoSpatialIT}.
 */
@SpringBootTest(properties = {
        "MONGO_URI=mongodb://localhost:27017/arbolado_db",
        "REDIS_HOST=localhost",
        "REDIS_PASSWORD=",
        "REDIS_SSL=false"})
class ArboladoApplicationTest
{

    @MockBean
    private MongoTemplate mongoTemplate;

    @MockBean
    private GridFsTemplate gridFsTemplate;

    @Test
    @DisplayName("context loads with the Redis manager and the fail-open resolver")
    void contextLoads(ApplicationContext context)
    {
        // Given the full application context
        // When inspecting cache wiring
        // Then the explicit JSON Redis manager and our resolver back the cache path
        assertThat(context.getBean(CacheManager.class)).isInstanceOf(RedisCacheManager.class);
        assertThat(context.containsBean("failOpenCacheResolver")).isTrue();
    }

    @Test
    @DisplayName("effective arboles values are JSON, never JDK serialization")
    void effectiveArbolesValuesAreJson(ApplicationContext context)
    {
        // Given the wired manager behind the fail-open resolver
        RedisCache cache = (RedisCache) context.getBean(CacheManager.class).getCache(SearchLimits.CACHE_NAME);
        List<ArbolResponse> dtos = List.of(new ArbolResponse(123, "Jacaranda mimosifolia", 8, 30, 1, -58.3816,
                -34.6037));

        // When round-tripping record DTOs through the effective pair
        assertThat(cache).isNotNull();
        ByteBuffer raw = cache.getCacheConfiguration().getValueSerializationPair().write(dtos);

        // Then JSON typing survives (a JDK pair would throw NotSerializableException)
        assertThat(cache.getCacheConfiguration().getValueSerializationPair().read(raw)).isEqualTo(dtos);
    }
}
