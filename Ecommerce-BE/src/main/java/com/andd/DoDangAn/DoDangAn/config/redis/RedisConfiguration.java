package com.andd.DoDangAn.DoDangAn.config.redis;


import com.andd.DoDangAn.DoDangAn.services.Cache.RedisMessageSubscriber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfiguration {
    @Value("6379")
    private String RedisPost;
    @Value("127.0.0.1")
    private String RedisHost;

    @Bean
    JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(RedisHost);
        redisStandaloneConfiguration.setPort(Integer.parseInt(RedisPost));
        return new JedisConnectionFactory(redisStandaloneConfiguration);
    }
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String,Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(jedisConnectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return redisTemplate;
    }

    @Bean
    public ChannelTopic commentsTopic() {
        return new ChannelTopic("comments");
    }

    @Bean
    public ChannelTopic movieListTopic() {
        return new ChannelTopic("movielist");
    }

    @Bean
    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory,
                                                        MessageListenerAdapter commentsListenerAdapter,
                                                        MessageListenerAdapter movieListListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(commentsListenerAdapter, commentsTopic());
        container.addMessageListener(movieListListenerAdapter, movieListTopic());
        return container;
    }

    @Bean
    public MessageListenerAdapter commentsListenerAdapter(RedisMessageSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber);
    }

    @Bean
    public MessageListenerAdapter movieListListenerAdapter(RedisMessageSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber);
    }
}