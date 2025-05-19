package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({UserDbStorage.class})
class UserDbStorageTest {

    private final UserDbStorage userStorage;
    private final JdbcTemplate jdbcTemplate;

    private User createTestUser(String email, String login, String name, LocalDate birthday) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName(name);
        user.setBirthday(birthday);
        user.setFriends(new HashSet<>());
        return user;
    }

    private int insertUser(User user) {
        String sql = "INSERT INTO Users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, user.getEmail(), user.getLogin(), user.getName(), user.getBirthday());
        return jdbcTemplate.queryForObject("SELECT id FROM Users WHERE email = ? ORDER BY id DESC LIMIT 1", Integer.class, user.getEmail());
    }

    private User mapRowToUserForTest(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setLogin(rs.getString("login"));
        user.setEmail(rs.getString("email"));
        user.setName(rs.getString("name"));
        java.sql.Date birthdaySql = rs.getDate("birthday");
        user.setBirthday(birthdaySql != null ? birthdaySql.toLocalDate() : null);
        user.setFriends(new HashSet<>());
        return user;
    }

    @Test
    @DisplayName("Add new user")
    void testAddUser() {
        User newUser = createTestUser("test@example.com", "test_login", "Test User", LocalDate.of(1990, 1, 15));

        User addedUser = userStorage.addUser(newUser);

        assertThat(addedUser).isNotNull();
        assertThat(addedUser.getId()).isPositive();
        assertThat(addedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(addedUser.getLogin()).isEqualTo("test_login");
        assertThat(addedUser.getName()).isEqualTo("Test User");
        assertThat(addedUser.getBirthday()).isEqualTo(LocalDate.of(1990, 1, 15));
        assertThat(addedUser.getFriends()).isEmpty();

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM Users WHERE id = ?", Integer.class, addedUser.getId());
        assertThat(count).isEqualTo(1);

        User userFromDb = jdbcTemplate.queryForObject(
                "SELECT id, email, login, name, birthday FROM Users WHERE id = ?",
                this::mapRowToUserForTest, addedUser.getId());

        assertThat(userFromDb).isNotNull();
        assertThat(userFromDb.getEmail()).isEqualTo("test@example.com");
        assertThat(userFromDb.getLogin()).isEqualTo("test_login");
        assertThat(userFromDb.getName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("Update existing user")
    void testUpdateUser() {
        User originalUser = createTestUser("original@example.com", "original_login", "Original User", LocalDate.of(1985, 3, 10));
        int userId = insertUser(originalUser);
        originalUser.setId(userId);

        User updatedUser = createTestUser("updated@example.com", "updated_login", "Updated User Name", LocalDate.of(1986, 4, 11));
        updatedUser.setId(userId);

        User resultUser = userStorage.updateUser(updatedUser);

        assertThat(resultUser).isNotNull();
        assertThat(resultUser.getId()).isEqualTo(userId);
        assertThat(resultUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(resultUser.getLogin()).isEqualTo("updated_login");
        assertThat(resultUser.getName()).isEqualTo("Updated User Name");
        assertThat(resultUser.getBirthday()).isEqualTo(LocalDate.of(1986, 4, 11));

        User userFromDb = jdbcTemplate.queryForObject(
                "SELECT id, email, login, name, birthday FROM Users WHERE id = ?",
                this::mapRowToUserForTest, userId);

        assertThat(userFromDb).isNotNull();
        assertThat(userFromDb.getEmail()).isEqualTo("updated@example.com");
        assertThat(userFromDb.getLogin()).isEqualTo("updated_login");
        assertThat(userFromDb.getName()).isEqualTo("Updated User Name");
    }

    @Test
    @DisplayName("Update non-existent user throws NotFoundException")
    void testUpdateNonExistentUserThrowsException() {
        User nonExistentUser = createTestUser("nonexistent@example.com", "nonexistent", "Non Existent", LocalDate.now());
        nonExistentUser.setId(999);

        assertThatThrownBy(() -> userStorage.updateUser(nonExistentUser))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Get user by existing ID without friends")
    void testGetUserByIdExistingWithoutFriends() {
        User userToInsert = createTestUser("getbyid@example.com", "getbyid", "Get By Id User", LocalDate.of(1992, 6, 20));
        int userId = insertUser(userToInsert);

        Optional<User> userOptional = userStorage.getUserById(userId);

        assertThat(userOptional).isPresent();
        User retrievedUser = userOptional.get();

        assertThat(retrievedUser.getId()).isEqualTo(userId);
        assertThat(retrievedUser.getLogin()).isEqualTo("getbyid");
        assertThat(retrievedUser.getName()).isEqualTo("Get By Id User");
        assertThat(retrievedUser.getFriends()).isEmpty();
    }

    @Test
    @DisplayName("Get user by existing ID with friends")
    void testGetUserByIdExistingWithFriends() {
        User mainUser = createTestUser("main@example.com", "main_user", "Main User", LocalDate.of(1990, 1, 1));
        User friend1 = createTestUser("friend1@example.com", "friend1", "Friend One", LocalDate.of(1991, 1, 1));
        User friend2 = createTestUser("friend2@example.com", "friend2", "Friend Two", LocalDate.of(1992, 1, 1));

        int mainUserId = insertUser(mainUser);
        int friend1Id = insertUser(friend1);
        int friend2Id = insertUser(friend2);

        jdbcTemplate.update("INSERT INTO user_friends (user_id, friend_id, status) VALUES (?, ?, ?)", mainUserId, friend1Id, "CONFIRMED");
        jdbcTemplate.update("INSERT INTO user_friends (user_id, friend_id, status) VALUES (?, ?, ?)", mainUserId, friend2Id, "CONFIRMED");

        Optional<User> userOptional = userStorage.getUserById(mainUserId);

        assertThat(userOptional).isPresent();
        User retrievedUser = userOptional.get();

        assertThat(retrievedUser.getId()).isEqualTo(mainUserId);
        assertThat(retrievedUser.getLogin()).isEqualTo("main_user");
        assertThat(retrievedUser.getFriends()).hasSize(2);
        assertThat(retrievedUser.getFriends()).extracting(User::getId).containsExactlyInAnyOrder(friend1Id, friend2Id);
    }

    @Test
    @DisplayName("Get user by non-existent ID")
    void testGetUserByIdNonExistent() {
        Optional<User> userOptional = userStorage.getUserById(999);

        assertThat(userOptional).isEmpty();
    }

    @Test
    @DisplayName("Get all users from empty database")
    void testGetUsersEmptyDatabase() {
        jdbcTemplate.update("DELETE FROM user_friends");
        jdbcTemplate.update("DELETE FROM Users");

        Collection<User> users = userStorage.getUsers();

        assertThat(users).isEmpty();
    }

    @Test
    @DisplayName("Get all users with and without friends")
    void testGetUsers() {
        User user1 = createTestUser("u1@example.com", "user1", "User One", LocalDate.of(1990, 1, 1));
        User user2 = createTestUser("u2@example.com", "user2", "User Two", LocalDate.of(1991, 1, 1));
        User user3 = createTestUser("u3@example.com", "user3", "User Three", LocalDate.of(1992, 1, 1));

        int user1Id = insertUser(user1);
        int user2Id = insertUser(user2);
        int user3Id = insertUser(user3);

        jdbcTemplate.update("INSERT INTO user_friends (user_id, friend_id, status) VALUES (?, ?, ?)", user1Id, user2Id, "CONFIRMED");
        jdbcTemplate.update("INSERT INTO user_friends (user_id, friend_id, status) VALUES (?, ?, ?)", user1Id, user3Id, "CONFIRMED");
        jdbcTemplate.update("INSERT INTO user_friends (user_id, friend_id, status) VALUES (?, ?, ?)", user2Id, user3Id, "CONFIRMED");

        Collection<User> users = userStorage.getUsers();

        assertThat(users).hasSize(3);

        Map<Integer, User> userMap = new HashMap<>();
        for (User user : users) {
            userMap.put(user.getId(), user);
        }

        User retrievedUser1 = userMap.get(user1Id);
        assertThat(retrievedUser1).isNotNull();
        assertThat(retrievedUser1.getLogin()).isEqualTo("user1");
        assertThat(retrievedUser1.getName()).isEqualTo("User One");
        assertThat(retrievedUser1.getFriends()).hasSize(2);
        assertThat(retrievedUser1.getFriends()).extracting(User::getId).containsExactlyInAnyOrder(user2Id, user3Id);

        User retrievedUser2 = userMap.get(user2Id);
        assertThat(retrievedUser2).isNotNull();
        assertThat(retrievedUser2.getLogin()).isEqualTo("user2");
        assertThat(retrievedUser2.getName()).isEqualTo("User Two");
        assertThat(retrievedUser2.getFriends()).hasSize(1);
        assertThat(retrievedUser2.getFriends()).extracting(User::getId).containsExactlyInAnyOrder(user3Id);

        User retrievedUser3 = userMap.get(user3Id);
        assertThat(retrievedUser3).isNotNull();
        assertThat(retrievedUser3.getLogin()).isEqualTo("user3");
        assertThat(retrievedUser3.getName()).isEqualTo("User Three");
        assertThat(retrievedUser3.getFriends()).isEmpty();
    }

    @Test
    @DisplayName("Add and remove friend relation")
    void testAddAndRemoveFriend() {
        User user1 = createTestUser("friendtest1@example.com", "ft_user1", "FT User One", LocalDate.of(1990, 1, 1));
        User user2 = createTestUser("friendtest2@example.com", "ft_user2", "FT User Two", LocalDate.of(1991, 1, 1));
        int user1Id = insertUser(user1);
        int user2Id = insertUser(user2);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_friends WHERE user_id = ? AND friend_id = ?", Integer.class, user1Id, user2Id);
        assertThat(count).isEqualTo(0);

        userStorage.addFriend(user1Id, user2Id);

        count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_friends WHERE user_id = ? AND friend_id = ?", Integer.class, user1Id, user2Id);
        assertThat(count).isEqualTo(1);

        userStorage.removeFriend(user1Id, user2Id);

        count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_friends WHERE user_id = ? AND friend_id = ?", Integer.class, user1Id, user2Id);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("Remove non-existent friend relation does not throw exception")
    void testRemoveNonExistentFriend() {
        User user1 = createTestUser("remove_nonexistent1@example.com", "rn_user1", "RN User One", LocalDate.of(1990, 1, 1));
        User user2 = createTestUser("remove_nonexistent2@example.com", "rn_user2", "RN User Two", LocalDate.of(1991, 1, 1));
        int user1Id = insertUser(user1);
        int user2Id = insertUser(user2);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_friends WHERE user_id = ? AND friend_id = ?", Integer.class, user1Id, user2Id);
        assertThat(count).isEqualTo(0);

        assertThatCode(() -> userStorage.removeFriend(user1Id, user2Id)).doesNotThrowAnyException();

        count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_friends WHERE user_id = ? AND friend_id = ?", Integer.class, user1Id, user2Id);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("Get common friends when they exist")
    void testGetCommonFriendsWhenTheyExist() {
        User user1 = createTestUser("common1@example.com", "common1", "Common User One", LocalDate.of(1990, 1, 1));
        User user2 = createTestUser("common2@example.com", "common2", "Common User Two", LocalDate.of(1991, 1, 1));
        User commonFriend1 = createTestUser("cf1@example.com", "cf1", "Common Friend One", LocalDate.of(1995, 1, 1));
        User commonFriend2 = createTestUser("cf2@example.com", "cf2", "Common Friend Two", LocalDate.of(1996, 1, 1));
        User nonCommonFriend = createTestUser("ncf@example.com", "ncf", "Non-Common Friend", LocalDate.of(1997, 1, 1));

        int user1Id = insertUser(user1);
        int user2Id = insertUser(user2);
        int cf1Id = insertUser(commonFriend1);
        int cf2Id = insertUser(commonFriend2);
        int ncfId = insertUser(nonCommonFriend);

        jdbcTemplate.update("INSERT INTO user_friends (user_id, friend_id, status) VALUES (?, ?, ?)", user1Id, cf1Id, "CONFIRMED");
        jdbcTemplate.update("INSERT INTO user_friends (user_id, friend_id, status) VALUES (?, ?, ?)", user1Id, cf2Id, "CONFIRMED");
        jdbcTemplate.update("INSERT INTO user_friends (user_id, friend_id, status) VALUES (?, ?, ?)", user1Id, ncfId, "CONFIRMED");

        jdbcTemplate.update("INSERT INTO user_friends (user_id, friend_id, status) VALUES (?, ?, ?)", user2Id, cf1Id, "CONFIRMED");
        jdbcTemplate.update("INSERT INTO user_friends (user_id, friend_id, status) VALUES (?, ?, ?)", user2Id, cf2Id, "CONFIRMED");

        Set<User> commonFriends = userStorage.getCommonFriends(user1Id, user2Id);

        assertThat(commonFriends).hasSize(2);
        Set<Integer> commonFriendIds = commonFriends.stream().map(User::getId).collect(java.util.stream.Collectors.toSet());
        assertThat(commonFriendIds).containsExactlyInAnyOrder(cf1Id, cf2Id);

        Optional<User> retrievedCf1 = commonFriends.stream().filter(u -> u.getId() == cf1Id).findFirst();
        assertThat(retrievedCf1).isPresent();
        assertThat(retrievedCf1.get().getLogin()).isEqualTo("cf1");
        assertThat(retrievedCf1.get().getName()).isEqualTo("Common Friend One");

        Optional<User> retrievedCf2 = commonFriends.stream().filter(u -> u.getId() == cf2Id).findFirst();
        assertThat(retrievedCf2).isPresent();
        assertThat(retrievedCf2.get().getLogin()).isEqualTo("cf2");
        assertThat(retrievedCf2.get().getName()).isEqualTo("Common Friend Two");
    }

    @Test
    @DisplayName("Get common friends when they do not exist")
    void testGetCommonFriendsWhenTheyDoNotExist() {
        User user1 = createTestUser("nocommon1@example.com", "nc1", "No Common One", LocalDate.of(1990, 1, 1));
        User user2 = createTestUser("nocommon2@example.com", "nc2", "No Common Two", LocalDate.of(1991, 1, 1));
        User friend1 = createTestUser("nc_f1@example.com", "ncf1", "NC Friend One", LocalDate.of(1995, 1, 1));
        User friend2 = createTestUser("nc_f2@example.com", "ncf2", "NC Friend Two", LocalDate.of(1996, 1, 1));

        int user1Id = insertUser(user1);
        int user2Id = insertUser(user2);
        int friend1Id = insertUser(friend1);
        int friend2Id = insertUser(friend2);

        jdbcTemplate.update("INSERT INTO user_friends (user_id, friend_id, status) VALUES (?, ?, ?)", user1Id, friend1Id, "CONFIRMED");
        jdbcTemplate.update("INSERT INTO user_friends (user_id, friend_id, status) VALUES (?, ?, ?)", user2Id, friend2Id, "CONFIRMED");

        Set<User> commonFriends = userStorage.getCommonFriends(user1Id, user2Id);

        assertThat(commonFriends).isEmpty();
    }

    @Test
    @DisplayName("Get common friends when one or both users do not exist")
    void testGetCommonFriendsWhenUsersDoNotExist() {
        User user1 = createTestUser("exists@example.com", "exists_user", "Exists User", LocalDate.of(1990, 1, 1));
        int user1Id = insertUser(user1);

        Set<User> commonFriends1 = userStorage.getCommonFriends(user1Id, 999);
        assertThat(commonFriends1).isEmpty();

        Set<User> commonFriends2 = userStorage.getCommonFriends(998, user1Id);
        assertThat(commonFriends2).isEmpty();

        Set<User> commonFriends3 = userStorage.getCommonFriends(998, 999);
        assertThat(commonFriends3).isEmpty();
    }
}