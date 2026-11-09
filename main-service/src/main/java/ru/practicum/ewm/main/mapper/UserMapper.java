package ru.practicum.ewm.main.mapper;

import ru.practicum.ewm.main.dto.UserDto;
import ru.practicum.ewm.main.dto.UserRequest;
import ru.practicum.ewm.main.model.User;

public class UserMapper {

    public static User toUser(UserRequest request) {
        return User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .build();
    }

    public static UserDto toUserDto(User user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail());
    }
}