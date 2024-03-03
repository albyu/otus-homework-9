package ru.boldyrev.otus.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import ru.boldyrev.otus.exception.ValidationErrorException;
import ru.boldyrev.otus.model.transfer.TransportableUser;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    private final ObjectMapper objectMapper;

    @Value("${application.redis.session-ttl}")
    private Long timeToLive;

    private static final String SESSIONS_KEY = "SESSION";



    public void saveSession(String key, TransportableUser transportableUser) throws ValidationErrorException {
        try {
            String jsonValue = objectMapper.writeValueAsString(transportableUser);
            redisTemplate.opsForValue().set(buildSessionKey(key), jsonValue);
            redisTemplate.expire(buildSessionKey(key), timeToLive, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            throw  new ValidationErrorException("Impossible to save session");
        }
    }

    public Optional<TransportableUser> getTransportableUserBySessionId(String key) throws ValidationErrorException {
        String jsonValue = (String) redisTemplate.opsForValue().get(buildSessionKey(key));
        if (jsonValue != null) {
            try {
                return Optional.ofNullable(objectMapper.readValue(jsonValue, TransportableUser.class));
            } catch (JsonProcessingException e) {
                throw  new ValidationErrorException("Impossible to get session");
            }
        }
        return Optional.empty();
    }

    public void deleteSession(String key) throws ValidationErrorException {
        Boolean deletedCount = redisTemplate.delete(buildSessionKey(key));
    }

    private String buildSessionKey(String sessionId) {
        return SESSIONS_KEY + "_" + sessionId;
    }

}
