package ru.practicum.ewm.main.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.main.dto.user.UserDto;
import ru.practicum.ewm.main.dto.user.UserRequest;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.model.User;
import ru.practicum.ewm.main.repository.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void register_shouldSaveUser_whenEmailIsFree() {
        UserRequest request = new UserRequest("Иван Петров", "ivan@practicummail.ru");
        when(userRepository.existsByEmail("ivan@practicummail.ru")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        UserDto result = userService.register(request);

        assertEquals(1L, result.getId());
        assertEquals("Иван Петров", result.getName());
        assertEquals("ivan@practicummail.ru", result.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrowConflict_whenEmailAlreadyExists() {
        UserRequest request = new UserRequest("Иван Петров", "ivan@practicummail.ru");
        when(userRepository.existsByEmail("ivan@practicummail.ru")).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.register(request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUsers_shouldReturnPageFromRepository_whenIdsNotPassed() {
        User second = User.builder().id(2L).name("Второй").email("second@practicummail.ru").build();
        when(userRepository.findAllWithOffset(1, 1)).thenReturn(List.of(second));

        List<UserDto> result = userService.getUsers(null, 1, 1);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getId());
    }

    @Test
    void getUsers_shouldReturnOnlyRequestedPage_whenIdsPassed() {
        User first = User.builder().id(1L).name("Первый").email("first@practicummail.ru").build();
        when(userRepository.findAllByIdsWithOffset(List.of(1L), 0, 10)).thenReturn(List.of(first));

        List<UserDto> result = userService.getUsers(List.of(1L), 0, 10);

        assertEquals(1, result.size());
        assertEquals("Первый", result.get(0).getName());
    }

    @Test
    void delete_shouldDeleteUser_whenUserExists() {
        when(userRepository.existsById(5L)).thenReturn(true);

        userService.delete(5L);

        verify(userRepository).deleteById(5L);
    }

    @Test
    void delete_shouldThrowNotFound_whenUserDoesNotExist() {
        when(userRepository.existsById(5L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> userService.delete(5L));

        verify(userRepository, never()).deleteById(any());
    }
}