package ru.practicum.ewm.main.service;

import ru.practicum.ewm.main.dto.UserDto;
import ru.practicum.ewm.main.dto.UserRequest;

import java.util.List;

public interface UserService {

    UserDto register(UserRequest request);

    List<UserDto> getUsers(List<Long> ids, int from, int size);

    void delete(Long userId);
}