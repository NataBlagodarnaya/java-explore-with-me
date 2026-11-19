package ru.practicum.ewm.main.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.main.dto.CategoryDto;
import ru.practicum.ewm.main.dto.CompilationDto;
import ru.practicum.ewm.main.dto.EventShortDto;
import ru.practicum.ewm.main.dto.NewCompilationRequest;
import ru.practicum.ewm.main.dto.UpdateCompilationRequest;
import ru.practicum.ewm.main.dto.UserShortDto;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.service.CompilationService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminCompilationController.class)
class AdminCompilationControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private CompilationService compilationService;

    @Test
    void saveCompilation_shouldReturnStatus201AndCompilation() throws Exception {
        when(compilationService.saveCompilation(any(NewCompilationRequest.class)))
                .thenReturn(createCompilation("Летние концерты", true));

        mvc.perform(post("/admin/compilations")
                        .content(mapper.writeValueAsString(
                                new NewCompilationRequest(Set.of(10L), true, "Летние концерты")))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Летние концерты")))
                .andExpect(jsonPath("$.pinned", is(true)))
                .andExpect(jsonPath("$.events", hasSize(1)))
                .andExpect(jsonPath("$.events[0].id", is(10)))
                .andExpect(jsonPath("$.events[0].title", is("Концерт группы")));
    }

    @Test
    void saveCompilation_shouldUseDefaults_whenOnlyTitleIsPassed() throws Exception {
        when(compilationService.saveCompilation(any(NewCompilationRequest.class)))
                .thenReturn(createCompilation("Выходные в городе", false));

        mvc.perform(post("/admin/compilations")
                        .content("{\"title\": \"Выходные в городе\"}")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        ArgumentCaptor<NewCompilationRequest> captor = ArgumentCaptor.forClass(NewCompilationRequest.class);
        verify(compilationService).saveCompilation(captor.capture());
        assertFalse(captor.getValue().getPinned());
        assertNull(captor.getValue().getEvents());
    }

    @Test
    void saveCompilation_shouldReturnBadRequest_whenTitleIsBlank() throws Exception {
        mvc.perform(post("/admin/compilations")
                        .content(mapper.writeValueAsString(new NewCompilationRequest(null, false, "")))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Field: title")));

        verify(compilationService, never()).saveCompilation(any(NewCompilationRequest.class));
    }

    @Test
    void saveCompilation_shouldReturnBadRequest_whenTitleIsLongerThan50Characters() throws Exception {
        mvc.perform(post("/admin/compilations")
                        .content(mapper.writeValueAsString(new NewCompilationRequest(null, false, "а".repeat(51))))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Field: title")));
    }

    @Test
    void saveCompilation_shouldReturnConflict_whenTitleAlreadyExists() throws Exception {
        when(compilationService.saveCompilation(any(NewCompilationRequest.class)))
                .thenThrow(new ConflictException("Compilation title already exists: Летние концерты"));

        mvc.perform(post("/admin/compilations")
                        .content(mapper.writeValueAsString(new NewCompilationRequest(null, false, "Летние концерты")))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is("CONFLICT")));
    }

    @Test
    void saveCompilation_shouldReturnNotFound_whenEventDoesNotExist() throws Exception {
        when(compilationService.saveCompilation(any(NewCompilationRequest.class)))
                .thenThrow(new NotFoundException("Event with id=99 was not found"));

        mvc.perform(post("/admin/compilations")
                        .content(mapper.writeValueAsString(new NewCompilationRequest(Set.of(99L), false, "Летние концерты")))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Event with id=99 was not found")));
    }

    @Test
    void deleteCompilation_shouldReturnStatus204() throws Exception {
        mvc.perform(delete("/admin/compilations/1"))
                .andExpect(status().isNoContent());

        verify(compilationService).deleteCompilation(1L);
    }

    @Test
    void deleteCompilation_shouldReturnNotFound_whenCompilationDoesNotExist() throws Exception {
        doThrow(new NotFoundException("Compilation with id=7 was not found"))
                .when(compilationService).deleteCompilation(7L);

        mvc.perform(delete("/admin/compilations/7")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("Compilation with id=7 was not found")));
    }

    @Test
    void deleteCompilation_shouldReturnBadRequest_whenIdIsNotNumber() throws Exception {
        mvc.perform(delete("/admin/compilations/abc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    void updateCompilation_shouldReturnUpdatedCompilationAndStatus200() throws Exception {
        when(compilationService.updateCompilation(eq(1L), any(UpdateCompilationRequest.class)))
                .thenReturn(createCompilation("Зимние концерты", false));

        mvc.perform(patch("/admin/compilations/1")
                        .content(mapper.writeValueAsString(
                                new UpdateCompilationRequest(Set.of(10L), false, "Зимние концерты")))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Зимние концерты")))
                .andExpect(jsonPath("$.pinned", is(false)));
    }

    @Test
    void updateCompilation_shouldAcceptEmptyBody() throws Exception {
        when(compilationService.updateCompilation(eq(1L), any(UpdateCompilationRequest.class)))
                .thenReturn(createCompilation("Летние концерты", true));

        mvc.perform(patch("/admin/compilations/1")
                        .content("{}")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void updateCompilation_shouldReturnBadRequest_whenTitleIsLongerThan50Characters() throws Exception {
        mvc.perform(patch("/admin/compilations/1")
                        .content(mapper.writeValueAsString(new UpdateCompilationRequest(null, null, "а".repeat(51))))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Field: title")));

        verify(compilationService, never()).updateCompilation(any(), any(UpdateCompilationRequest.class));
    }

    @Test
    void updateCompilation_shouldReturnConflict_whenTitleIsTaken() throws Exception {
        when(compilationService.updateCompilation(eq(1L), any(UpdateCompilationRequest.class)))
                .thenThrow(new ConflictException("Compilation title already exists: Зимние концерты"));

        mvc.perform(patch("/admin/compilations/1")
                        .content(mapper.writeValueAsString(new UpdateCompilationRequest(null, null, "Зимние концерты")))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is("CONFLICT")));
    }

    @Test
    void updateCompilation_shouldReturnNotFound_whenCompilationDoesNotExist() throws Exception {
        when(compilationService.updateCompilation(eq(7L), any(UpdateCompilationRequest.class)))
                .thenThrow(new NotFoundException("Compilation with id=7 was not found"));

        mvc.perform(patch("/admin/compilations/7")
                        .content(mapper.writeValueAsString(new UpdateCompilationRequest(null, true, null)))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Compilation with id=7 was not found")));
    }

    private CompilationDto createCompilation(String title, boolean pinned) {
        EventShortDto event = EventShortDto.builder()
                .id(10L)
                .annotation("Концерт под открытым небом")
                .category(new CategoryDto(2L, "Концерты"))
                .confirmedRequests(0L)
                .eventDate(LocalDateTime.of(2030, 1, 15, 12, 0, 0))
                .initiator(new UserShortDto(1L, "Иван Петров"))
                .paid(false)
                .title("Концерт группы")
                .views(0L)
                .build();
        return CompilationDto.builder()
                .id(1L)
                .title(title)
                .pinned(pinned)
                .events(List.of(event))
                .build();
    }
}