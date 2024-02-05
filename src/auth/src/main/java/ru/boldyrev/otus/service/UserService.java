package ru.boldyrev.otus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.boldyrev.otus.exception.AlreadyExistsException;
import ru.boldyrev.otus.exception.BadCredentialsException;
import ru.boldyrev.otus.exception.NotFoundException;
import ru.boldyrev.otus.exception.ValidationErrorException;
import ru.boldyrev.otus.model.entity.User;
import ru.boldyrev.otus.model.transfer.TransportableUser;
import ru.boldyrev.otus.listener.RabbitSender;
import ru.boldyrev.otus.repo.UserRepo;

import javax.transaction.Transactional;
import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserService {

    private final UserRepo userRepo;
    private final RabbitSender rabbitSender;

    public User getUserById(String username) throws NotFoundException {
        Optional<User> user = userRepo.findById(username);
        if (user.isPresent()) {
            return user.get();
        } else {
            throw new NotFoundException("User not found");
        }

    }

    @Transactional
    public TransportableUser createUser(TransportableUser tUser) throws AlreadyExistsException, ValidationErrorException {
        if (tUser.getUsername() == null || tUser.getPassword() == null || tUser.getEmail() == null) {
            throw new ValidationErrorException("Username, password and email should be provided");
        }
        if (tUser.getUsername().isEmpty() || tUser.getPassword().isEmpty() || tUser.getEmail().isEmpty()) {
            throw new ValidationErrorException("Username, password and email should be provided");
        }

        User user;
        if (userRepo.findById(tUser.getUsername()).isEmpty()) {
            user = new User().setUsername(tUser.getUsername())
                    .setPassword(tUser.getPassword())
                    .setFirstName(tUser.getFirstName())
                    .setLastName(tUser.getLastName())
                    .setPhone(tUser.getPhone())
                    .setEmail(tUser.getEmail());
            user = userRepo.save(user);
        } else {
            throw new AlreadyExistsException("User already exists");
        }

        /*Отправка уведомлений*/
        /* В account на открытие счета - достаточно username */
        TransportableUser ttu = new TransportableUser().setUsername(user.getUsername());
        rabbitSender.sendAccountRequest(ttu);


        /* В notification на регистрацию - кроме username нужен e-mail*/
        ttu.setEmail(user.getEmail());
        rabbitSender.sendNotificationRequest(ttu);

        /*И вернем результат для REST - все поля кроме password*/
        ttu.setPhone(user.getPhone())
                .setLastName(user.getLastName()).setFirstName(user.getLastName());

        return ttu;
    }

    public User updateUser(String username, User userDetails) throws NotFoundException {
        Optional<User> user = userRepo.findById(username);
        if (user.isPresent()) {
            User existingUser = user.get();

            if (userDetails.getFirstName() != null)
                existingUser.setFirstName(userDetails.getFirstName());

            if (userDetails.getLastName() != null)
                existingUser.setLastName(userDetails.getLastName());

            if (userDetails.getEmail() != null)
                existingUser.setEmail(userDetails.getEmail());

            if (userDetails.getPhone() != null)
                existingUser.setPhone(userDetails.getPhone());

            if (userDetails.getPassword() != null)
                existingUser.setPassword(userDetails.getPassword());


            return userRepo.save(existingUser);
        } else
            throw new NotFoundException("User not found");
    }

    public User deleteUser(String username) throws NotFoundException {
        Optional<User> user = userRepo.findById(username);
        if (user.isPresent()) {
            userRepo.deleteById(username);
            return null;
        } else
            throw new NotFoundException("User not found");
    }

    public TransportableUser getUserByCredentials(TransportableUser tUser) throws ValidationErrorException, BadCredentialsException {
        if (tUser.getUsername() == null || tUser.getPassword() == null) {
            throw new ValidationErrorException("Username and password should be provided");
        }
        if (tUser.getUsername().isEmpty() || tUser.getPassword().isEmpty()) {
            throw new ValidationErrorException("Username and password should be provided");
        }

        Optional<User> appUser = userRepo.findByUsernameAndPassword(tUser.getUsername(), tUser.getPassword());
        if (appUser.isPresent()) {
            TransportableUser ttu = new TransportableUser()
                    .setUsername(appUser.get().getUsername())
                    .setEmail(appUser.get().getEmail())
                    .setLastName(appUser.get().getLastName())
                    .setFirstName(appUser.get().getFirstName())
                    .setPhone(appUser.get().getPhone());
            return ttu;
        } else throw new BadCredentialsException("Invalid login/password");


    }
}
