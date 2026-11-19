package ru.practicum.ewm.main.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.main.dto.UserDto;
import ru.practicum.ewm.main.dto.UserRequest;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.service.UserService;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminUserController.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private UserService userService;

    @Test
    void register_shouldReturnStatus201AndUser_whenRequestIsValid() throws Exception {
        UserRequest request = new UserRequest("Иван Петров", "ivan@practicummail.ru");
        when(userService.register(any(UserRequest.class)))
                .thenReturn(new UserDto(1L, "Иван Петров", "ivan@practicummail.ru"));

        mvc.perform(post("/admin/users")
                        .content(mapper.writeValueAsString(request))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Иван Петров")))
                .andExpect(jsonPath("$.email", is("ivan@practicummail.ru")));
    }

    @Test
    void register_shouldReturnBadRequest_whenNameIsBlank() throws Exception {
        UserRequest request = new UserRequest("", "ivan@practicummail.ru");

        mvc.perform(post("/admin/users")
                        .content(mapper.writeValueAsString(request))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.reason", is("Incorrectly made request.")))
                .andExpect(jsonPath("$.message", containsString("Field: name")));

        verify(userService, never()).register(any(UserRequest.class));
    }

    @Test
    void register_shouldReturnBadRequest_whenEmailIsInvalid() throws Exception {
        UserRequest request = new UserRequest("Иван Петров", "not-an-email");

        mvc.perform(post("/admin/users")
                        .content(mapper.writeValueAsString(request))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Field: email")));

        verify(userService, never()).register(any(UserRequest.class));
    }

    @Test
    void register_shouldReturnBadRequest_whenBodyIsMissing() throws Exception {
        mvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    void register_shouldReturnConflict_whenEmailAlreadyExists() throws Exception {
        UserRequest request = new UserRequest("Иван Петров", "ivan@practicummail.ru");
        when(userService.register(any(UserRequest.class)))
                .thenThrow(new ConflictException("Email already exists: ivan@practicummail.ru"));

        mvc.perform(post("/admin/users")
                        .content(mapper.writeValueAsString(request))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is("CONFLICT")))
                .andExpect(jsonPath("$.reason", is("For the requested operation the conditions are not met.")))
                .andExpect(jsonPath("$.message", is("Email already exists: ivan@practicummail.ru")));
    }

    @Test
    void getUsers_shouldReturnUsersAndStatus200_whenIdsPassed() throws Exception {
        when(userService.getUsers(List.of(1L, 2L), 0, 10)).thenReturn(List.of(
                new UserDto(1L, "Иван Петров", "ivan@practicummail.ru"),
                new UserDto(2L, "Мария Сидорова", "maria@practicummail.ru")));

        mvc.perform(get("/admin/users")
                        .param("ids", "1", "2")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Иван Петров")))
                .andExpect(jsonPath("$[1].name", is("Мария Сидорова")));
    }

    @Test
    void getUsers_shouldUseDefaultPagination_whenParamsAreNotPassed() throws Exception {
        when(userService.getUsers(null, 0, 10)).thenReturn(List.of());

        mvc.perform(get("/admin/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(userService).getUsers(null, 0, 10);
    }

    @Test
    void getUsers_shouldReturnBadRequest_whenSizeIsZero() throws Exception {
        mvc.perform(get("/admin/users")
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    void getUsers_shouldReturnBadRequest_whenFromIsNegative() throws Exception {
        mvc.perform(get("/admin/users")
                        .param("from", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    void getUsers_shouldReturnBadRequest_whenIdsAreNotNumbers() throws Exception {
        mvc.perform(get("/admin/users")
                        .param("ids", "abc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    void delete_shouldReturnStatus204_whenUserExists() throws Exception {
        mvc.perform(delete("/admin/users/5"))
                .andExpect(status().isNoContent());

        verify(userService).delete(5L);
    }

    @Test
    void delete_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        doThrow(new NotFoundException("User with id=5 was not found")).when(userService).delete(5L);

        mvc.perform(delete("/admin/users/5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("NOT_FOUND")))
                .andExpect(jsonPath("$.reason", is("The required object was not found.")))
                .andExpect(jsonPath("$.message", is("User with id=5 was not found")));
    }

    @Test
    void delete_shouldReturnBadRequest_whenIdIsNotNumber() throws Exception {
        mvc.perform(delete("/admin/users/abc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    void register_shouldReturnConflict_whenDatabaseConstraintIsViolated() throws Exception {
        UserRequest request = new UserRequest("Иван Петров", "ivan@practicummail.ru");
        when(userService.register(any(UserRequest.class)))
                .thenThrow(new DataIntegrityViolationException("could not execute statement; constraint [uq_email]"));

        mvc.perform(post("/admin/users")
                        .content(mapper.writeValueAsString(request))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is("CONFLICT")))
                .andExpect(jsonPath("$.reason", is("Integrity constraint has been violated.")));
    }

    @Test
    void register_shouldReturnInternalServerError_whenUnexpectedExceptionIsThrown() throws Exception {
        UserRequest request = new UserRequest("Иван Петров", "ivan@practicummail.ru");
        when(userService.register(any(UserRequest.class))).thenThrow(new RuntimeException("Что-то пошло не так"));

        mvc.perform(post("/admin/users")
                        .content(mapper.writeValueAsString(request))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is("INTERNAL_SERVER_ERROR")))
                .andExpect(jsonPath("$.reason", is("Unexpected error occurred.")))
                .andExpect(jsonPath("$.message", is("Internal server error")));
    }
}