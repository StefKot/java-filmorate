package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.filmorate.exceptions.ContentNotException;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.services.UserService;
import ru.yandex.practicum.filmorate.storage.interfaces.UserStorage;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserStorage mockUserStorage;

    @InjectMocks
    private UserService userService;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        user1 = new User(1, "user1@example.com", "user1", "User One", LocalDate.of(1990, 1, 1), new HashSet<>());
        user2 = new User(2, "user2@example.com", "user2", "User Two", LocalDate.of(1991, 2, 2), new HashSet<>());
        user3 = new User(3, "user3@example.com", "user3", "User Three", LocalDate.of(1992, 3, 3), new HashSet<>());
    }


    @Test
    @DisplayName("Add user with blank name - name should be set to login")
    void testAddUserBlankName() {
        User newUser = new User(0, "new@example.com", "new_user", "", LocalDate.of(2000, 1, 1), new HashSet<>());
        User userAfterAdd = new User(10, "new@example.com", "new_user", "new_user", LocalDate.of(2000, 1, 1), new HashSet<>());

        when(mockUserStorage.addUser(any(User.class))).thenReturn(userAfterAdd);

        User addedUser = userService.addUser(newUser);

        assertThat(addedUser.getName()).isEqualTo("new_user");
        verify(mockUserStorage, times(1)).addUser(newUser);
    }

    @Test
    @DisplayName("Add user with null name - name should be set to login")
    void testAddUserNullName() {
        User newUser = new User(0, "new@example.com", "new_user", null, LocalDate.of(2000, 1, 1), new HashSet<>());
        User userAfterAdd = new User(10, "new@example.com", "new_user", "new_user", LocalDate.of(2000, 1, 1), new HashSet<>());

        when(mockUserStorage.addUser(any(User.class))).thenReturn(userAfterAdd);

        User addedUser = userService.addUser(newUser);

        assertThat(addedUser.getName()).isEqualTo("new_user");
        verify(mockUserStorage, times(1)).addUser(newUser);
    }

    @Test
    @DisplayName("Add user with invalid email throws ValidationException")
    void testAddUserInvalidEmail() {
        User newUser = new User(0, "invalid-email", "new_user", "New User", LocalDate.of(2000, 1, 1), new HashSet<>());

        assertThatThrownBy(() -> userService.addUser(newUser))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid email address");

        verify(mockUserStorage, never()).addUser(any(User.class));
    }

    @Test
    @DisplayName("Add user with invalid login (spaces) throws ValidationException")
    void testAddUserInvalidLoginSpaces() {
        User newUser = new User(0, "new@example.com", "new user", "New User", LocalDate.of(2000, 1, 1), new HashSet<>());

        assertThatThrownBy(() -> userService.addUser(newUser))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Login is empty or contains spaces");

        verify(mockUserStorage, never()).addUser(any(User.class));
    }

    @Test
    @DisplayName("Add user with blank login throws ValidationException")
    void testAddUserBlankLogin() {
        User newUser = new User(0, "new@example.com", "", "New User", LocalDate.of(2000, 1, 1), new HashSet<>());

        assertThatThrownBy(() -> userService.addUser(newUser))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Login is empty or contains spaces");

        verify(mockUserStorage, never()).addUser(any(User.class));
    }


    @Test
    @DisplayName("Add user with future birthday throws ValidationException")
    void testAddUserFutureBirthday() {
        User newUser = new User(0, "new@example.com", "new_user", "New User", LocalDate.now().plusDays(1), new HashSet<>());

        assertThatThrownBy(() -> userService.addUser(newUser))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid date of birth");

        verify(mockUserStorage, never()).addUser(any(User.class));
    }


    @Test
    @DisplayName("Add friend successfully")
    void testAddFriendSuccessfully() {

        User user1BeforeAdd = new User(user1.getId(), user1.getEmail(), user1.getLogin(), user1.getName(), user1.getBirthday(), new HashSet<>());
        User user2Exists = new User(user2.getId(), user2.getEmail(), user2.getLogin(), user2.getName(), user2.getBirthday(), new HashSet<>());


        User user1AfterAdd = new User(user1.getId(), user1.getEmail(), user1.getLogin(), user1.getName(), user1.getBirthday(), new HashSet<>(Set.of(user2Exists)));


        when(mockUserStorage.getUserById(user1.getId()))
                .thenReturn(Optional.of(user1BeforeAdd))
                .thenReturn(Optional.of(user1AfterAdd));

        when(mockUserStorage.getUserById(user2.getId())).thenReturn(Optional.of(user2Exists));

        doNothing().when(mockUserStorage).addFriend(user1.getId(), user2.getId());

        User updatedUser1 = userService.addFriends(user1.getId(), user2.getId());

        assertThat(updatedUser1).isNotNull();
        assertThat(updatedUser1.getId()).isEqualTo(user1.getId());
        assertThat(updatedUser1.getFriends()).hasSize(1);
        assertThat(updatedUser1.getFriends()).extracting(User::getId).containsExactly(user2.getId());

        verify(mockUserStorage, times(2)).getUserById(user1.getId());
        verify(mockUserStorage, times(1)).getUserById(user2.getId());
        verify(mockUserStorage, times(1)).addFriend(user1.getId(), user2.getId());
    }

    @Test
    @DisplayName("Add friend when user1 not found throws NotFoundException")
    void testAddFriendUser1NotFound() {
        when(mockUserStorage.getUserById(user1.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.addFriends(user1.getId(), user2.getId()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with ID " + user1.getId() + " not found");

        verify(mockUserStorage, times(1)).getUserById(user1.getId());
        verify(mockUserStorage, never()).getUserById(user2.getId());
        verify(mockUserStorage, never()).addFriend(anyInt(), anyInt());
    }

    @Test
    @DisplayName("Add friend when user2 not found throws NotFoundException")
    void testAddFriendUser2NotFound() {
        User user1Exists = new User(user1.getId(), user1.getEmail(), user1.getLogin(), user1.getName(), user1.getBirthday(), new HashSet<>());

        when(mockUserStorage.getUserById(user1.getId())).thenReturn(Optional.of(user1Exists));
        when(mockUserStorage.getUserById(user2.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.addFriends(user1.getId(), user2.getId()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with ID " + user2.getId() + " not found");

        verify(mockUserStorage, times(1)).getUserById(user1.getId());
        verify(mockUserStorage, times(1)).getUserById(user2.getId());
        verify(mockUserStorage, never()).addFriend(anyInt(), anyInt());
    }

    @Test
    @DisplayName("Add friend when they are already friends throws ValidationException")
    void testAddFriendAlreadyFriends() {

        User user1AlreadyFriends = new User(user1.getId(), user1.getEmail(), user1.getLogin(), user1.getName(), user1.getBirthday(), new HashSet<>(Set.of(user2)));
        User user2Exists = new User(user2.getId(), user2.getEmail(), user2.getLogin(), user2.getName(), user2.getBirthday(), new HashSet<>());

        when(mockUserStorage.getUserById(user1.getId())).thenReturn(Optional.of(user1AlreadyFriends));
        when(mockUserStorage.getUserById(user2.getId())).thenReturn(Optional.of(user2Exists));

        assertThatThrownBy(() -> userService.addFriends(user1.getId(), user2.getId()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("User " + user2.getId() + " is already added as a friend");

        verify(mockUserStorage, times(1)).getUserById(user1.getId());
        verify(mockUserStorage, times(1)).getUserById(user2.getId());
        verify(mockUserStorage, never()).addFriend(anyInt(), anyInt());
    }


    @Test
    @DisplayName("Delete friend successfully")
    void testDeleteFriendSuccessfully() {

        User user1BeforeDelete = new User(user1.getId(), user1.getEmail(), user1.getLogin(), user1.getName(), user1.getBirthday(), new HashSet<>(Set.of(user2)));
        User user2Exists = new User(user2.getId(), user2.getEmail(), user2.getLogin(), user2.getName(), user2.getBirthday(), new HashSet<>());


        User user1AfterDelete = new User(user1.getId(), user1.getEmail(), user1.getLogin(), user1.getName(), user1.getBirthday(), new HashSet<>());

        when(mockUserStorage.getUserById(user1.getId()))
                .thenReturn(Optional.of(user1BeforeDelete))
                .thenReturn(Optional.of(user1AfterDelete));

        when(mockUserStorage.getUserById(user2.getId())).thenReturn(Optional.of(user2Exists));

        doNothing().when(mockUserStorage).removeFriend(user1.getId(), user2.getId());

        User updatedUser1 = userService.deleteFriends(user1.getId(), user2.getId());

        assertThat(updatedUser1).isNotNull();
        assertThat(updatedUser1.getId()).isEqualTo(user1.getId());
        assertThat(updatedUser1.getFriends()).isEmpty();

        verify(mockUserStorage, times(2)).getUserById(user1.getId());
        verify(mockUserStorage, times(1)).getUserById(user2.getId());
        verify(mockUserStorage, times(1)).removeFriend(user1.getId(), user2.getId());
    }


    @Test
    @DisplayName("Delete friend when they are not friends throws ContentNotException")
    void testDeleteFriendNotFriends() {
        User user1NotFriends = new User(user1.getId(), user1.getEmail(), user1.getLogin(), user1.getName(), user1.getBirthday(), new HashSet<>());
        User user2Exists = new User(user2.getId(), user2.getEmail(), user2.getLogin(), user2.getName(), user2.getBirthday(), new HashSet<>());


        when(mockUserStorage.getUserById(user1.getId())).thenReturn(Optional.of(user1NotFriends));
        when(mockUserStorage.getUserById(user2.getId())).thenReturn(Optional.of(user2Exists));

        assertThatThrownBy(() -> userService.deleteFriends(user1.getId(), user2.getId()))
                .isInstanceOf(ContentNotException.class)
                .hasMessageContaining("User with ID " + user2.getId() + " not found in friends list of user with ID " + user1.getId());

        verify(mockUserStorage, times(1)).getUserById(user1.getId());
        verify(mockUserStorage, times(1)).getUserById(user2.getId());
        verify(mockUserStorage, never()).removeFriend(anyInt(), anyInt());
    }


    @Test
    @DisplayName("Get friends returns user object with empty friends list when no friends")
    void testGetFriendsReturnsUserWithEmptyFriends() {
        User userWithoutFriends = new User(user1.getId(), user1.getEmail(), user1.getLogin(), user1.getName(), user1.getBirthday(), new HashSet<>());

        when(mockUserStorage.getUserById(user1.getId())).thenReturn(Optional.of(userWithoutFriends));

        Collection<User> friendsCollection = userService.getFriends(user1.getId());

        assertThat(friendsCollection).isNotNull().isEmpty();

        verify(mockUserStorage, times(1)).getUserById(user1.getId());
    }

    @Test
    @DisplayName("Get friends returns user object with populated friends list when friends exist")
    void testGetFriendsReturnsUserWithFriends() {
        User userWithFriends = new User(user1.getId(), user1.getEmail(), user1.getLogin(), user1.getName(), user1.getBirthday(), new HashSet<>(Set.of(user2, user3)));

        when(mockUserStorage.getUserById(user1.getId())).thenReturn(Optional.of(userWithFriends));

        Collection<User> friendsCollection = userService.getFriends(user1.getId());

        assertThat(friendsCollection).isNotNull().hasSize(2);
        assertThat(friendsCollection).extracting(User::getId).containsExactlyInAnyOrder(user2.getId(), user3.getId());

        verify(mockUserStorage, times(1)).getUserById(user1.getId());
    }


    @Test
    @DisplayName("Get friends of non-existent user throws NotFoundException")
    void testGetFriendsNonExistentUser() {
        int nonExistentId = 999;
        when(mockUserStorage.getUserById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getFriends(nonExistentId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with ID " + nonExistentId + " not found");

        verify(mockUserStorage, times(1)).getUserById(nonExistentId);
    }


    @Test
    @DisplayName("Get common friends successfully")
    void testGetCommonFriends() {
        Set<User> commonFriendsFromStorage = new HashSet<>(Set.of(user3));

        when(mockUserStorage.getUserById(user1.getId())).thenReturn(Optional.of(user1));
        when(mockUserStorage.getUserById(user2.getId())).thenReturn(Optional.of(user2));
        when(mockUserStorage.getCommonFriends(user1.getId(), user2.getId())).thenReturn(commonFriendsFromStorage);

        Set<User> commonFriendsResult = userService.getCommonFriends(user1.getId(), user2.getId());

        assertThat(commonFriendsResult).isNotNull().hasSize(1);
        assertThat(commonFriendsResult).extracting(User::getId).containsExactly(user3.getId());

        verify(mockUserStorage, times(1)).getUserById(user1.getId());
        verify(mockUserStorage, times(1)).getUserById(user2.getId());
        verify(mockUserStorage, times(1)).getCommonFriends(user1.getId(), user2.getId());
    }


    @Test
    @DisplayName("Get common friends when first user does not exist throws NotFoundException")
    void testGetCommonFriendsWhenFirstUserDoesNotExist() {
        int nonExistentId1 = 998;
        int user2Id = user2.getId();

        when(mockUserStorage.getUserById(nonExistentId1)).thenReturn(Optional.empty());


        assertThatThrownBy(() -> userService.getCommonFriends(nonExistentId1, user2Id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with ID " + nonExistentId1 + " not found");

        verify(mockUserStorage, times(1)).getUserById(nonExistentId1);
        verify(mockUserStorage, never()).getUserById(user2Id);
        verify(mockUserStorage, never()).getCommonFriends(anyInt(), anyInt());
    }

    @Test
    @DisplayName("Get common friends when second user does not exist throws NotFoundException")
    void testGetCommonFriendsWhenSecondUserDoesNotExist() {
        User userA = new User(100, "a@e.com", "a", "A", LocalDate.now(), new HashSet<>());
        int userAId = userA.getId();
        int nonExistentIdB = 101;

        when(mockUserStorage.getUserById(userAId)).thenReturn(Optional.of(userA));
        when(mockUserStorage.getUserById(nonExistentIdB)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCommonFriends(userAId, nonExistentIdB))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with ID " + nonExistentIdB + " not found");

        verify(mockUserStorage, times(1)).getUserById(userAId);
        verify(mockUserStorage, times(1)).getUserById(nonExistentIdB);
        verify(mockUserStorage, never()).getCommonFriends(anyInt(), anyInt());
    }

    @Test
    @DisplayName("Get common friends when both users do not exist throws NotFoundException for first user")
    void testGetCommonFriendsWhenBothUsersDoNotExist() {
        int nonExistentIdC = 200;
        int nonExistentIdD = 201;

        when(mockUserStorage.getUserById(nonExistentIdC)).thenReturn(Optional.empty());


        assertThatThrownBy(() -> userService.getCommonFriends(nonExistentIdC, nonExistentIdD))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with ID " + nonExistentIdC + " not found");

        verify(mockUserStorage, times(1)).getUserById(nonExistentIdC);
        verify(mockUserStorage, never()).getUserById(nonExistentIdD);
        verify(mockUserStorage, never()).getCommonFriends(anyInt(), anyInt());
    }


    @Test
    @DisplayName("Add user successfully")
    void testAddUserSuccessfully() {
        User newUser = new User(0, "new@example.com", "new_user", "New User", LocalDate.of(2000, 1, 1), new HashSet<>());
        User userAfterAdd = new User(10, newUser.getEmail(), newUser.getLogin(), newUser.getName(), newUser.getBirthday(), new HashSet<>());

        when(mockUserStorage.addUser(any(User.class))).thenReturn(userAfterAdd);

        User addedUser = userService.addUser(newUser);

        assertThat(addedUser).isNotNull();
        assertThat(addedUser.getId()).isEqualTo(10);
        assertThat(addedUser.getEmail()).isEqualTo(newUser.getEmail());
        assertThat(addedUser.getLogin()).isEqualTo(newUser.getLogin());
        assertThat(addedUser.getName()).isEqualTo(newUser.getName());
        assertThat(addedUser.getBirthday()).isEqualTo(newUser.getBirthday());

        verify(mockUserStorage, times(1)).addUser(any(User.class));
    }

    @Test
    @DisplayName("Update user successfully")
    void testUpdateUserSuccessfully() {
        User existingUser = new User(1, "old@example.com", "old_user", "Old User", LocalDate.of(1990, 1, 1), new HashSet<>());
        User updatedUserData = new User(1, "updated@example.com", "updated_user", "Updated User", LocalDate.of(1991, 1, 1), new HashSet<>());

        when(mockUserStorage.getUserById(existingUser.getId())).thenReturn(Optional.of(existingUser));
        when(mockUserStorage.updateUser(any(User.class))).thenReturn(updatedUserData);

        User resultUser = userService.updateUser(updatedUserData);

        assertThat(resultUser).isNotNull();
        assertThat(resultUser.getId()).isEqualTo(existingUser.getId());
        assertThat(resultUser.getEmail()).isEqualTo(updatedUserData.getEmail());
        assertThat(resultUser.getLogin()).isEqualTo(updatedUserData.getLogin());
        assertThat(resultUser.getName()).isEqualTo(updatedUserData.getName());
        assertThat(resultUser.getBirthday()).isEqualTo(updatedUserData.getBirthday());

        verify(mockUserStorage, times(1)).getUserById(existingUser.getId());
        verify(mockUserStorage, times(1)).updateUser(any(User.class));
    }

    @Test
    @DisplayName("Update non-existent user throws NotFoundException")
    void testUpdateNonExistentUserThrowsNotFoundException() {
        int nonExistentUserId = 999;
        User userToUpdate = new User(nonExistentUserId, "u@e.com", "u", "U", LocalDate.now(), new HashSet<>());

        when(mockUserStorage.getUserById(nonExistentUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(userToUpdate))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with ID " + nonExistentUserId + " not found");

        verify(mockUserStorage, times(1)).getUserById(nonExistentUserId);
        verify(mockUserStorage, never()).updateUser(any(User.class));
    }

    @Test
    @DisplayName("Get all users successfully")
    void testGetAllUsersSuccessfully() {
        User userAWithFriends = new User(1, "a@e.com", "a", "A", LocalDate.now(), new HashSet<>(Set.of(user2)));
        User userBWithoutFriends = new User(2, "b@e.com", "b", "B", LocalDate.now(), new HashSet<>());


        List<User> usersFromStorage = List.of(userAWithFriends, userBWithoutFriends);


        when(mockUserStorage.getUsers()).thenReturn(usersFromStorage);

        Collection<User> resultUsers = userService.getUsers();

        assertThat(resultUsers).isNotNull().hasSize(2);
        assertThat(resultUsers).containsExactlyInAnyOrder(userAWithFriends, userBWithoutFriends);

        User retrievedUserAWithFriends = resultUsers.stream()
                .filter(u -> u.getId() == userAWithFriends.getId())
                .findFirst()
                .orElseThrow(() -> new AssertionError("User A not found in results"));

        assertThat(retrievedUserAWithFriends.getFriends()).isNotNull().hasSize(1);
        assertThat(retrievedUserAWithFriends.getFriends()).extracting(User::getId).containsExactly(user2.getId());

        User retrievedUserBWithoutFriends = resultUsers.stream()
                .filter(u -> u.getId() == userBWithoutFriends.getId())
                .findFirst()
                .orElseThrow(() -> new AssertionError("User B not found in results"));

        assertThat(retrievedUserBWithoutFriends.getFriends()).isNotNull().isEmpty();


        verify(mockUserStorage, times(1)).getUsers();
    }
}