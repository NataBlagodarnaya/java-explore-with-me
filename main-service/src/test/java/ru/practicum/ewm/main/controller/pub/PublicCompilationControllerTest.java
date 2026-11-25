package ru.practicum.ewm.main.controller.pub;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.main.dto.category.CategoryDto;
import ru.practicum.ewm.main.dto.compilation.CompilationDto;
import ru.practicum.ewm.main.dto.event.EventShortDto;
import ru.practicum.ewm.main.dto.user.UserShortDto;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.service.CompilationService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PublicCompilationController.class)
class PublicCompilationControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CompilationService compilationService;

    @Test
    void getCompilations_shouldUseDefaultParams_whenNothingIsPassed() throws Exception {
        when(compilationService.getCompilations(null, 0, 10)).thenReturn(List.of(createCompilation(1L, true)));

        mvc.perform(get("/compilations")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].title", is("Летние концерты")))
                .andExpect(jsonPath("$[0].pinned", is(true)))
                .andExpect(jsonPath("$[0].events", hasSize(1)))
                .andExpect(jsonPath("$[0].events[0].title", is("Концерт группы")));

        verify(compilationService).getCompilations(null, 0, 10);
    }

    @Test
    void getCompilations_shouldPassPinnedAndPaginationToService() throws Exception {
        when(compilationService.getCompilations(true, 2, 5)).thenReturn(List.of());

        mvc.perform(get("/compilations")
                        .param("pinned", "true")
                        .param("from", "2")
                        .param("size", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(compilationService).getCompilations(true, 2, 5);
    }

    @Test
    void getCompilations_shouldReturnBadRequest_whenSizeIsZero() throws Exception {
        mvc.perform(get("/compilations")
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    void getCompilations_shouldReturnBadRequest_whenFromIsNegative() throws Exception {
        mvc.perform(get("/compilations")
                        .param("from", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    void getCompilations_shouldReturnBadRequest_whenPinnedIsNotBoolean() throws Exception {
        mvc.perform(get("/compilations")
                        .param("pinned", "abc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    void getCompilation_shouldReturnCompilationAndStatus200() throws Exception {
        when(compilationService.getCompilation(1L)).thenReturn(createCompilation(1L, false));

        mvc.perform(get("/compilations/1")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.pinned", is(false)))
                .andExpect(jsonPath("$.events[0].initiator.name", is("Иван Петров")))
                .andExpect(jsonPath("$.events[0].eventDate", is("2030-01-15 12:00:00")));
    }

    @Test
    void getCompilation_shouldReturnNotFound_whenCompilationDoesNotExist() throws Exception {
        when(compilationService.getCompilation(7L))
                .thenThrow(new NotFoundException("Compilation with id=7 was not found"));

        mvc.perform(get("/compilations/7")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("Compilation with id=7 was not found")));
    }

    @Test
    void getCompilation_shouldReturnBadRequest_whenIdIsNotNumber() throws Exception {
        mvc.perform(get("/compilations/abc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    private CompilationDto createCompilation(Long id, boolean pinned) {
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
                .id(id)
                .title("Летние концерты")
                .pinned(pinned)
                .events(List.of(event))
                .build();
    }
}