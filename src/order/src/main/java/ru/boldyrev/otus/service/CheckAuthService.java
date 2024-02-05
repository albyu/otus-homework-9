package ru.boldyrev.otus.service;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.boldyrev.otus.exception.AuthErrorException;
import ru.boldyrev.otus.exception.NotAuthorizedException;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CheckAuthService {

    @Value("${application.auth-url}")
    String authUrl;

    public void checkAuth(String sessionId, String username) throws NotAuthorizedException, AuthErrorException {
        String loggedUsername = checkAuth(sessionId);
        if (!loggedUsername.equals(username)) throw new NotAuthorizedException("Client not authorized");
    }

    public String checkAuth(String sessionId) throws NotAuthorizedException, AuthErrorException {
        Map<String, String> sessionData = getSessionData(sessionId);
        String loggedUsername = sessionData.get("X-User-Name");
        if (loggedUsername == null) throw new NotAuthorizedException("Client not authorized");
        return loggedUsername;
    }

    private Map<String, String> getSessionData(String sessionId) throws NotAuthorizedException, AuthErrorException {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", "session_id=" + sessionId);
        HttpEntity<String> request = new HttpEntity<>(null, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(authUrl, HttpMethod.GET, request, Void.class);
            return response.getHeaders().entrySet().stream().filter(e -> e.getKey().startsWith("X-")).collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
        } catch (HttpClientErrorException.Unauthorized ex){
            throw new NotAuthorizedException("Client not authorized");
        } catch (Exception e) {
            throw new AuthErrorException("Authentication error");
        }
    }


}
