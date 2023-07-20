package com.dmdev.junit.service;

import com.dmdev.junit.dao.UserDao;
import com.dmdev.junit.dto.User;
import com.dmdev.junit.extension.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;


import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@ExtendWith( {
        UserServiceParamResolver.class,
  /*      GlobalExtension.class,*/
        PostProcessingExtension.class,
        ConditionalExtension.class,
        MockitoExtension.class
        /*ThrowableExtension.class*/
})
public class UserServiceTest {

    private static final User IVAN = User.of(1, "Ivan", "123");
    private static final User PETR = User.of(2, "Petr", "111");

    @Captor
    private ArgumentCaptor<Integer> argumentCaptor;
    @InjectMocks
    private UserService userService;
    @Mock(lenient = true)
    private UserDao userDao;


    UserServiceTest(TestInfo testInfo) {
        System.out.println();
    }

    @BeforeAll
    static void init() {
        System.out.println("Before all");
    }

    @BeforeEach
    void prepare() {
        System.out.println("Before each " + this);
/*        lenient().when(userDao.delete(IVAN.getId())).thenReturn(true);*/
        doReturn(true).when(userDao).delete(IVAN.getId());
/*        Mockito.mock(UserDao.class, withSettings().lenient())*/
/*        this.userDao = Mockito.spy(new UserDao());
         this.userService = new UserService(userDao);*/
    }

    @Test
    void throwExceptionIfDatabaseIsNotAvailable() {
        doThrow(RuntimeException.class).when(userDao).delete(IVAN.getId());
        assertThrows(RuntimeException.class, () -> userService.delete(IVAN.getId()));
    }

    @Test
    void shouldDeleteExistedUser() {
        userService.add(IVAN);
/*        Mockito.doReturn(true).when(userDao).delete(IVAN.getId());*/
        /*Mockito.doReturn(true).when(userDao).delete(Mockito.any());*/

        /*Mockito.when(userDao.delete(IVAN.getId())).thenReturn(true);*/

        /*ArgumentCaptor<Integer> integerArgumentCaptor = ArgumentCaptor.forClass(Integer.class);*/
        boolean delete = userService.delete(IVAN.getId());

        verify(userDao).delete(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isEqualTo(IVAN.getId());
        /*Mockito.verifyNoInteractions(userDao);*/

        assertThat(delete).isTrue();
    }

    @Test
    void usersEmptyIfNoUserAdded() throws IOException {
/*        if (true) {
            throw new RuntimeException();
        }*/
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
        void checkLoginFunctionalityPerfomance() {
            var result = assertTimeout(Duration.ofMillis(200L), () -> {
                Thread.sleep(250L);
                return userService.login("dummy", IVAN.getPassword());
            });
        }

        @Test
        /*@RepeatedTest(value = 5, name =RepeatedTest.LONG_DISPLAY_NAME)*/
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

        @ParameterizedTest(name = "{arguments} test")
        @MethodSource("com.dmdev.junit.service.UserServiceTest#getArgumentsForLoginTest")
        @DisplayName("login param test")
        void loginParameterizedTest(String username, String password, Optional<User> user) {
            userService.add(IVAN, PETR);

            Optional<User> maybeUser = userService.login(username, password);
            assertThat(maybeUser).isEqualTo(user);
        }

    }

    static Stream<Arguments> getArgumentsForLoginTest() {
        return Stream.of(
                Arguments.of("Ivan", "123", Optional.of(IVAN)),
                Arguments.of("Petr", "111", Optional.of(PETR)),
                        Arguments.of("Petr", "dummy", Optional.empty()),
                                Arguments.of("dummy", "123", Optional.empty())
                );
    }
}