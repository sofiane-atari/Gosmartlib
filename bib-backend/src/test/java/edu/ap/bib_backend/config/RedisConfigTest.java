package edu.ap.bib_backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

@ExtendWith(MockitoExtension.class)
class RedisConfigTest {

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    @Test
    void givenConnectionFactory_WhenRedisTemplateCreated_ThenUseProvidedFactory() {
        // Arrange
        RedisConfig redisConfig = new RedisConfig();

        // Act
        RedisTemplate<String, String> redisTemplate = redisConfig.redisTemplate(redisConnectionFactory);

        // Assert
        assertSame(redisConnectionFactory, redisTemplate.getConnectionFactory());
    }

    @Test
    void givenConnectionFactory_WhenRedisTemplateCreated_ThenUseStringSerializersForAllKeyAndValueTypes() {
        // Arrange
        RedisConfig redisConfig = new RedisConfig();

        // Act
        RedisTemplate<String, String> redisTemplate = redisConfig.redisTemplate(redisConnectionFactory);

        // Assert
        assertInstanceOf(StringRedisSerializer.class, redisTemplate.getKeySerializer());
        assertInstanceOf(StringRedisSerializer.class, redisTemplate.getValueSerializer());
        assertInstanceOf(StringRedisSerializer.class, redisTemplate.getHashKeySerializer());
        assertInstanceOf(StringRedisSerializer.class, redisTemplate.getHashValueSerializer());
    }
}