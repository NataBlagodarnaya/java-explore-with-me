package ru.practicum.ewm.main.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.main.dto.CategoryDto;
import ru.practicum.ewm.main.dto.CategoryRequest;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.service.CategoryService;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
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

@WebMvcTest(controllers = AdminCategoryController.class)
class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private CategoryService categoryService;

    @Test
    void create_shouldReturnStatus201AndCategory_whenRequestIsValid() throws Exception {
        when(categoryService.create(any(CategoryRequest.class))).thenReturn(new CategoryDto(1L, "Концерты"));

        mvc.perform(post("/admin/categories")
                        .content(mapper.writeValueAsString(new CategoryRequest("Концерты")))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Концерты")));
    }

    @Test
    void create_shouldReturnBadRequest_whenNameIsBlank() throws Exception {
        mvc.perform(post("/admin/categories")
                        .content(mapper.writeValueAsString(new CategoryRequest("")))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.reason", is("Incorrectly made request.")))
                .andExpect(jsonPath("$.message", containsString("Field: name")));

        verify(categoryService, never()).create(any(CategoryRequest.class));
    }

    @Test
    void create_shouldReturnBadRequest_whenNameIsLongerThan50Characters() throws Exception {
        mvc.perform(post("/admin/categories")
                        .content(mapper.writeValueAsString(new CategoryRequest("а".repeat(51))))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Field: name")));

        verify(categoryService, never()).create(any(CategoryRequest.class));
    }

    @Test
    void create_shouldReturnConflict_whenNameAlreadyExists() throws Exception {
        when(categoryService.create(any(CategoryRequest.class)))
                .thenThrow(new ConflictException("Category name already exists: Концерты"));

        mvc.perform(post("/admin/categories")
                        .content(mapper.writeValueAsString(new CategoryRequest("Концерты")))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is("CONFLICT")))
                .andExpect(jsonPath("$.reason", is("For the requested operation the conditions are not met.")));
    }

    @Test
    void update_shouldReturnStatus200AndCategory_whenRequestIsValid() throws Exception {
        when(categoryService.update(eq(1L), any(CategoryRequest.class)))
                .thenReturn(new CategoryDto(1L, "Театры"));

        mvc.perform(patch("/admin/categories/1")
                        .content(mapper.writeValueAsString(new CategoryRequest("Театры")))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Театры")));
    }

    @Test
    void update_shouldReturnBadRequest_whenNameIsBlank() throws Exception {
        mvc.perform(patch("/admin/categories/1")
                        .content(mapper.writeValueAsString(new CategoryRequest("")))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));

        verify(categoryService, never()).update(any(), any(CategoryRequest.class));
    }

    @Test
    void update_shouldReturnNotFound_whenCategoryDoesNotExist() throws Exception {
        when(categoryService.update(eq(7L), any(CategoryRequest.class)))
                .thenThrow(new NotFoundException("Category with id=7 was not found"));

        mvc.perform(patch("/admin/categories/7")
                        .content(mapper.writeValueAsString(new CategoryRequest("Театры")))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("NOT_FOUND")))
                .andExpect(jsonPath("$.reason", is("The required object was not found.")))
                .andExpect(jsonPath("$.message", is("Category with id=7 was not found")));
    }

    @Test
    void update_shouldReturnConflict_whenNameIsTakenByAnotherCategory() throws Exception {
        when(categoryService.update(eq(1L), any(CategoryRequest.class)))
                .thenThrow(new ConflictException("Category name already exists: Театры"));

        mvc.perform(patch("/admin/categories/1")
                        .content(mapper.writeValueAsString(new CategoryRequest("Театры")))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is("CONFLICT")));
    }

    @Test
    void delete_shouldReturnStatus204_whenCategoryIsEmpty() throws Exception {
        mvc.perform(delete("/admin/categories/1"))
                .andExpect(status().isNoContent());

        verify(categoryService).delete(1L);
    }

    @Test
    void delete_shouldReturnNotFound_whenCategoryDoesNotExist() throws Exception {
        doThrow(new NotFoundException("Category with id=7 was not found")).when(categoryService).delete(7L);

        mvc.perform(delete("/admin/categories/7")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Category with id=7 was not found")));
    }

    @Test
    void delete_shouldReturnConflict_whenCategoryHasEvents() throws Exception {
        doThrow(new ConflictException("The category is not empty")).when(categoryService).delete(1L);

        mvc.perform(delete("/admin/categories/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is("CONFLICT")))
                .andExpect(jsonPath("$.message", is("The category is not empty")));
    }

    @Test
    void delete_shouldReturnBadRequest_whenIdIsNotNumber() throws Exception {
        mvc.perform(delete("/admin/categories/abc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }
}