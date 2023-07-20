package com.dmdev.junit.service;

import com.dmdev.junit.dto.User;
import com.dmdev.junit.parameresolver.UserServiceParamResolver;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.collection.IsMapContaining;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;


import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.*;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@ExtendWith( {
        UserServiceParamResolver.class
})
public class UserServiceTest {

    private static final User IVAN = User.of(1, "Ivan", "123");
    private static final User PETR = User.of(2, "Petr", "111");
    private UserService userService;

    UserServiceTest(TestInfo testInfo) {
        System.out.println();
    }

    @BeforeAll
    static void init() {
        System.out.println("Before all");
    }

    @BeforeEach
    void prepare(UserService userService) {
        System.out.println("Before each " + this);
         this.userService = userService;
    }

    @Test
    void usersEmptyIfNoUserAdded() {
        System.out.println("Test1 " + this);
        var users = userService.getAll();
        assertTrue(users.isEmpty(), () -> "users list should be empty");
    }

    @Test
    void usersSizeIfUserAdded() {
        System.out.println("Test2 " + this);
        userService.add(IVAN);
        userService.add(PETR);
        var users = userService.getAll();
        /*MatcherAssert.assertThat(users, empty());*/
        assertThat(users).hasSize(2);
        /*assertEquals(2, users.size());*/
    }

    @Test
    void usersConvertedToMapById() {
        userService.add(IVAN, PETR);
        Map<Integer, User> users = userService.getAllConvertedById();
        /*MatcherAssert.assertThat(users, IsMapContaining.hasKey(IVAN.getId()));*/
        assertAll(
                () -> assertThat(users).containsKeys(IVAN.getId(), PETR.getId()),
                () -> assertThat(users).containsValues(IVAN, PETR)
        );
    }

    @AfterEach
    void deleteDataFromDatabase() {
        System.out.println("After each" + this);
    }

    @AfterAll
    static void closeConnection() {
        System.out.println("After all");
    }

    @Nested
    @DisplayName("test user login functionality")
    @Tag("login")
    class LoginTest {
        @Test
        void loginSuccessIfUserExists() {
            userService.add(IVAN);

            Optional<User> user = userService.login(IVAN.getUserName(), IVAN.getPassword());
            assertThat(user).isPresent();
            user.ifPresent(maybeUser -> assertThat(maybeUser).isEqualTo(IVAN));
            /*assertTrue(user.isPresent());*/
            /*user.ifPresent(maybeUser -> assertEquals(IVAN, maybeUser));*/
        }

        @Test
        void loginFailIfPasswordIsNotCorrect() {
            userService.add(IVAN);

            var maybeUser = userService.login(IVAN.getUserName(), "111");

            assertTrue(maybeUser.isEmpty());
        }

        @Test
        void loginFailIfUserDoesNotExist() {
            userService.add(IVAN);

            var maybeUser = userService.login("Dummy", "111");

            assertTrue(maybeUser.isEmpty());
        }

        @Test
        void throwExceptionIfUsernameOrPasswordIsNull() {
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class, () -> userService.login(null, "dummy")),
                    () -> assertThrows(IllegalArgumentException.class, () -> userService.login("dummy", null))
            );
        }
    }
}