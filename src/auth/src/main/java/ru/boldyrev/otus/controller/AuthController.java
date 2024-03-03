package ru.boldyrev.otus.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.boldyrev.otus.exception.*;
import ru.boldyrev.otus.model.transfer.TransportableUser;
import ru.boldyrev.otus.service.RedisService;
import ru.boldyrev.otus.service.UserService;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;

@RestController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Api(tags = "Аутентификация/авторизация клиента")
public class AuthController {
    //private Map<String, TransportableUser> SESSIONS = new HashMap<>();

    private final UserService userService;

    private final RedisService redisService;

    private String createSession(TransportableUser u) throws ValidationErrorException {
        String sessionId = generateSessionId(40);
        redisService.saveSession(sessionId, u);
        //SESSIONS.put(sessionId, u);
        return sessionId;
    }

    private void deleteSession(String sessionId) throws ValidationErrorException {
        //SESSIONS.remove(sessionId);
        redisService.deleteSession(sessionId);
    }

    private TransportableUser getSessionUser(String sessionId) throws NotAuthorizedException, ValidationErrorException {
        //if (sessionId != null && SESSIONS.containsKey(sessionId)) {
        if (sessionId != null) {
            Optional<TransportableUser> tUser = redisService.getTransportableUserBySessionId(sessionId);
            if (tUser.isPresent()) {
                return tUser.get();
            }
        }
        //} else throw new NotAuthorizedException("User not authorized");
        throw new NotAuthorizedException("User not authorized");
    }

    private HttpHeaders convertUserToHeaders(TransportableUser tUser) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Name", tUser.getUsername());
        headers.add("X-Email", tUser.getEmail());
        headers.add("X-Phone", tUser.getPhone());
        headers.add("X-First-Name", tUser.getFirstName());
        headers.add("X-Last-Name", tUser.getLastName());

        return headers;
    }

    private String generateSessionId(int size) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789" + "abcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            int index = (int) (chars.length() * Math.random());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    @ApiOperation(value = "Зарегистрировать нового клиента")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = TransportableUser.class),
            @ApiResponse(code = 400, message = "Username, password and email should be provided"),
            @ApiResponse(code = 409, message = "User already exists")
    })
    @PostMapping(value = "/register", consumes = "application/json")
    public ResponseEntity<TransportableUser> register(@RequestBody TransportableUser u) throws ValidationErrorException, AlreadyExistsException {
        TransportableUser newUser = userService.createUser(u);

        return ResponseEntity.status(HttpStatus.OK).body(newUser);
    }

    @ApiOperation(value = "Запрос на авторизацию")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = TransportableUser.class),
            @ApiResponse(code = 400, message = "Username and password should be provided"),
            @ApiResponse(code = 401, message = "Invalid login/password")
    })
    @PostMapping(value = "/login", consumes = "application/json")
    public ResponseEntity<TransportableUser> login(@RequestBody TransportableUser tUser, @CookieValue(value = "session_id", defaultValue = "") String oldSessionId) throws ValidationErrorException, BadCredentialsException {

        deleteSession(oldSessionId);

        TransportableUser loggedUser = userService.getUserByCredentials(tUser);

        String newSessionId = createSession(loggedUser);

        HttpHeaders headers = convertUserToHeaders(loggedUser);

        headers.add(HttpHeaders.SET_COOKIE, "session_id=" + newSessionId + "; Path=/; HttpOnly");
        log.info("Session_id {} generated", newSessionId);
        return ResponseEntity.ok().headers(headers).body(loggedUser);
    }

    @ApiOperation(value = "Завершение пользовательской сессии")
    @RequestMapping(value = "/logout", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse response, @CookieValue(value = "session_id", defaultValue = "") String sessionId) throws ValidationErrorException {

        deleteSession(sessionId);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, "session_id=; Max-Age=0");

        return ResponseEntity.ok().headers(headers).body(Map.of("status", "ok"));
    }

    @ApiOperation(value = "Внутренний ендпойнт: запрос на подтвеждение авторизации")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = TransportableUser.class),
            @ApiResponse(code = 401, message = "User not authorized")
    })
    @GetMapping(value = "/check")
    public ResponseEntity<TransportableUser> auth(@CookieValue(name = "session_id", required = false) String sessionId) throws NotAuthorizedException, ValidationErrorException {
        TransportableUser tUser = getSessionUser(sessionId);
        HttpHeaders headers = convertUserToHeaders(tUser);
        return ResponseEntity.ok().headers(headers).body(tUser);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> authExceptionProcessing(NotFoundException notFoundException) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("errorReason", notFoundException.getMessage()));
    }

    @ExceptionHandler(ValidationErrorException.class)
    public ResponseEntity<Map<String, String>> authExceptionProcessing(ValidationErrorException notFoundException) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("errorReason", notFoundException.getMessage()));
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> authExceptionProcessing(AlreadyExistsException alreadyExistsException) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("errorReason", alreadyExistsException.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> authExceptionProcessing(BadCredentialsException badCredentialsException) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, "session_id=; Max-Age=0");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).headers(headers).body(Map.of("errorReason", badCredentialsException.getMessage()));
    }

    @ExceptionHandler(NotAuthorizedException.class)
    public ResponseEntity<Map<String, String>> authExceptionProcessing(NotAuthorizedException notAuthorizedException) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("errorReason", notAuthorizedException.getMessage()));
    }


}
