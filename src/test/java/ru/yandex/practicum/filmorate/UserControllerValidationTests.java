package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerValidationTests {

    private UserController userController;
    private User validUser;
    private static final LocalDate MIN_BIRTHDATE = LocalDate.of(1910, 1, 1);

    @BeforeEach
    void setUp() {
        userController = new UserController();
        validUser = new User();
        validUser.setEmail("test@example.com");
        validUser.setLogin("testLogin");
        validUser.setName("Test User");
        validUser.setBirthday(LocalDate.of(2003, 9, 22));
    }

    @Test
    void createValidUser_shouldCreateUserSuccessfully() {
        User createdUser = userController.create(validUser);
        assertNotNull(createdUser.getId(), "Created user should have a non-null ID");
        assertEquals(validUser.getEmail(), createdUser.getEmail(), "Created user should have the same email as the input user");
    }

    @Test
    void createUser_whenEmailIsInvalid_shouldThrowValidationException() {
        validUser.setEmail("invalid-email");
        ValidationException exception = assertThrows(ValidationException.class, () -> userController.create(validUser),
                "Creating a user with an invalid email should throw a ValidationException");
        assertEquals("Invalid email format.", exception.getMessage(), "Exception message should indicate an invalid email format");
    }

    @Test
    void createUser_whenEmailIsBlank_shouldThrowValidationException() {
        validUser.setEmail(" ");
        ValidationException exception = assertThrows(ValidationException.class, () -> userController.create(validUser),
                "Creating a user with a blank email should throw a ValidationException");
        assertEquals("Invalid email format.", exception.getMessage(), "Exception message should indicate an invalid email format");
    }

    @Test
    void createUser_whenEmailIsNull_shouldThrowValidationException() {
        validUser.setEmail(null);
        ValidationException exception = assertThrows(ValidationException.class, () -> userController.create(validUser),
                "Creating a user with a null email should throw a ValidationException");
        assertEquals("Invalid email format.", exception.getMessage(), "Exception message should indicate an invalid email format");
    }

    @Test
    void createUser_whenLoginContainsSpaces_shouldThrowValidationException() {
        validUser.setLogin("invalid login");
        ValidationException exception = assertThrows(ValidationException.class, () -> userController.create(validUser),
                "Creating a user with a login containing spaces should throw a ValidationException");
        assertEquals("Login cannot be blank or contain spaces.", exception.getMessage(), "Exception message should indicate that login contains spaces");
    }

    @Test
    void createUser_whenLoginIsBlank_shouldThrowValidationException() {
        validUser.setLogin(" ");
        ValidationException exception = assertThrows(ValidationException.class, () -> userController.create(validUser),
                "Creating a user with a blank login should throw a ValidationException");
        assertEquals("Login cannot be blank or contain spaces.", exception.getMessage(), "Exception message should indicate that login is blank");
    }

    @Test
    void createUser_whenLoginIsNull_shouldThrowValidationException() {
        validUser.setLogin(null);
        ValidationException exception = assertThrows(ValidationException.class, () -> userController.create(validUser),
                "Creating a user with a null login should throw a ValidationException");
        assertEquals("Login cannot be blank or contain spaces.", exception.getMessage(), "Exception message should indicate that login is null");
    }

    @Test
    void createUser_whenBirthdayIsInFuture_shouldThrowValidationException() {
        validUser.setBirthday(LocalDate.now().plusDays(1));
        ValidationException exception = assertThrows(ValidationException.class, () -> userController.create(validUser),
                "Creating a user with a birthday in the future should throw a ValidationException");
        assertTrue(exception.getMessage().contains("Birthday cannot be null, before"), "Exception message should indicate that the birthday is in the future");
    }

    @Test
    void createUser_whenBirthdayIsTooOld_shouldThrowValidationException() {
        validUser.setBirthday(LocalDate.of(1817, 1, 1)); // Before MIN_BIRTHDATE
        ValidationException exception = assertThrows(ValidationException.class, () -> userController.create(validUser),
                "Creating a user with a birthday before MIN_BIRTHDATE should throw a ValidationException");
        assertTrue(exception.getMessage().contains("Birthday cannot be null, before"), "Exception message should indicate that birthday is too old");

    }

    @Test
    void createUser_whenBirthdayIsNull_shouldThrowValidationException() {
        validUser.setBirthday(null);
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.create(validUser),
                "Creating a user with a null birthday should throw a ValidationException");
        assertTrue(exception.getMessage().contains("Birthday cannot be null, before"));
    }

    @Test
    void updateUser_whenUserIsValid_shouldUpdateUserSuccessfully() {
        User createdUser = userController.create(validUser);
        createdUser.setName("Updated Name");
        User updatedUser = userController.update(createdUser);
        assertEquals("Updated Name", updatedUser.getName(), "Updated user should have the new name");
    }

    @Test
    void updateUser_whenEmailIsInvalid_shouldThrowValidationException() {
        User createdUser = userController.create(validUser);
        createdUser.setEmail("invalid-email");
        ValidationException exception = assertThrows(ValidationException.class, () -> userController.update(createdUser),
                "Updating a user with an invalid email should throw a ValidationException");
        assertEquals("Invalid email format.", exception.getMessage(), "Exception message should indicate an invalid email format");
    }

    @Test
    void updateUser_whenIdIsInvalid_shouldThrowValidationException() {
        validUser.setId(9999);  // Non-existent ID
        ValidationException exception = assertThrows(ValidationException.class, () -> userController.update(validUser),
                "Updating user with non-existent id should throw ValidationException");
        assertEquals("User with ID 9999 not found.", exception.getMessage());
    }

    @Test
    void updateUser_whenBirthdayIsBeforeMinBirthdate_shouldThrowValidationException() {
        User createdUser = userController.create(validUser);
        createdUser.setBirthday(MIN_BIRTHDATE.minusDays(1));
        ValidationException exception = assertThrows(ValidationException.class, () -> userController.update(createdUser),
                "Updating a user with a birthday before MIN_BIRTHDATE should throw a ValidationException");
        assertTrue(exception.getMessage().contains("Birthday cannot be null, before"), "Exception message should indicate an invalid birthday");
    }

    @Test
    void updateUser_whenBirthdayIsInFuture_shouldThrowValidationException() {
        User createdUser = userController.create(validUser);
        createdUser.setBirthday(LocalDate.now().plusDays(1));
        ValidationException exception = assertThrows(ValidationException.class, () -> userController.update(createdUser),
                "Updating a user with a birthday in the future should throw a ValidationException");
        assertTrue(exception.getMessage().contains("Birthday cannot be null, before"), "Exception message should indicate an invalid birthday");
    }

    @Test
    void updateUser_whenBirthdayIsNull_shouldThrowValidationException() {
        User createdUser = userController.create(validUser);
        createdUser.setBirthday(null);
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.update(createdUser),
                "Updating a user with a null birthday should throw a ValidationException");
        assertTrue(exception.getMessage().contains("Birthday cannot be null, before"));
    }

    @Test
    void updateUser_whenLoginIsNull_shouldThrowValidationException() {
        User createdUser = userController.create(validUser);
        createdUser.setLogin(null);
        ValidationException exception = assertThrows(ValidationException.class, () -> userController.update(createdUser),
                "Updating a user with a null login should throw a ValidationException");
        assertEquals("Login cannot be blank or contain spaces.", exception.getMessage(), "Exception message should indicate that login is null");
    }

    @Test
    void updateUser_whenLoginIsBlank_shouldThrowValidationException() {
        User createdUser = userController.create(validUser);
        createdUser.setLogin(" ");
        ValidationException exception = assertThrows(ValidationException.class, () -> userController.update(createdUser),
                "Updating a user with a blank login should throw a ValidationException");
        assertEquals("Login cannot be blank or contain spaces.", exception.getMessage(), "Exception message should indicate that login is blank");
    }

    @Test
    void updateUser_whenLoginContainsSpaces_shouldThrowValidationException() {
        User createdUser = userController.create(validUser);
        createdUser.setLogin("invalid login");
        ValidationException exception = assertThrows(ValidationException.class, () -> userController.update(createdUser),
                "Updating a user with a login containing spaces should throw a ValidationException");
        assertEquals("Login cannot be blank or contain spaces.", exception.getMessage(), "Exception message should indicate that login contains spaces");
    }

    @Test
    void updateUser_whenEmailIsNull_shouldThrowValidationException() {
        User createdUser = userController.create(validUser);
        createdUser.setEmail(null);
        ValidationException exception = assertThrows(ValidationException.class, () -> userController.update(createdUser),
                "Updating user with null email should throw ValidationException"
        );
        assertEquals("Invalid email format.", exception.getMessage());
    }

    @Test
    void updateUser_whenEmailIsBlank_shouldThrowValidationException() {
        User createdUser = userController.create(validUser);
        createdUser.setEmail(" ");
        ValidationException exception = assertThrows(ValidationException.class, () -> userController.update(createdUser),
                "Updating user with blank email should throw ValidationException"
        );
        assertEquals("Invalid email format.", exception.getMessage());
    }
}