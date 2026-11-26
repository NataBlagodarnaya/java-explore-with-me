package ru.practicum.ewm.main.controller.pub;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.main.dto.category.CategoryDto;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.service.CategoryService;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PublicCategoryController.class)
class PublicCategoryControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CategoryService categoryService;

    @Test
    void getAll_shouldReturnCategoriesAndStatus200() throws Exception {
        when(categoryService.getAll(0, 10)).thenReturn(List.of(
                new CategoryDto(1L, "Концерты"),
                new CategoryDto(2L, "Театры")));

        mvc.perform(get("/categories")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Концерты")))
                .andExpect(jsonPath("$[1].name", is("Театры")));
    }

    @Test
    void getAll_shouldPassPaginationParamsToService() throws Exception {
        when(categoryService.getAll(5, 3)).thenReturn(List.of());

        mvc.perform(get("/categories")
                        .param("from", "5")
                        .param("size", "3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(categoryService).getAll(5, 3);
    }

    @Test
    void getAll_shouldReturnBadRequest_whenSizeIsZero() throws Exception {
        mvc.perform(get("/categories")
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    void getAll_shouldReturnBadRequest_whenFromIsNegative() throws Exception {
        mvc.perform(get("/categories")
                        .param("from", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }

    @Test
    void getById_shouldReturnCategoryAndStatus200() throws Exception {
        when(categoryService.getById(1L)).thenReturn(new CategoryDto(1L, "Концерты"));

        mvc.perform(get("/categories/1")
                        .characterEncoding(StandardCharsets.UTF_8)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Концерты")));
    }

    @Test
    void getById_shouldReturnNotFound_whenCategoryDoesNotExist() throws Exception {
        when(categoryService.getById(7L)).thenThrow(new NotFoundException("Category with id=7 was not found"));

        mvc.perform(get("/categories/7")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("Category with id=7 was not found")));
    }

    @Test
    void getById_shouldReturnBadRequest_whenIdIsNotNumber() throws Exception {
        mvc.perform(get("/categories/abc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")));
    }
}