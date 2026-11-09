package ru.practicum.ewm.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.dto.UserDto;
import ru.practicum.ewm.main.dto.UserRequest;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.mapper.UserMapper;
import ru.practicum.ewm.main.model.User;
import ru.practicum.ewm.main.repository.UserRepository;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto register(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists: " + request.getEmail());
        }
        User saved = userRepository.save(UserMapper.toUser(request));
        log.info("Зарегистрирован пользователь id={}", saved.getId());
        return UserMapper.toUserDto(saved);
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        List<User> users = (ids == null || ids.isEmpty())
                ? userRepository.findAll()
                : userRepository.findAllById(ids);
        return users.stream()
                .sorted(Comparator.comparing(User::getId))
                .skip(from)
                .limit(size)
                .map(UserMapper::toUserDto)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
        userRepository.deleteById(userId);
        log.info("Удалён пользователь id={}", userId);
    }
}