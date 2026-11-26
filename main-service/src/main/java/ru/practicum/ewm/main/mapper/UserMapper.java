package ru.practicum.ewm.main.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.main.dto.user.UserDto;
import ru.practicum.ewm.main.dto.user.UserRequest;
import ru.practicum.ewm.main.dto.user.UserShortDto;
import ru.practicum.ewm.main.model.User;

@UtilityClass
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

    public static UserShortDto toUserShortDto(User user) {
        return new UserShortDto(user.getId(), user.getName());
    }
}