package ru.practicum.ewm.main.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.main.dto.category.CategoryDto;
import ru.practicum.ewm.main.dto.category.CategoryRequest;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.model.Category;
import ru.practicum.ewm.main.repository.CategoryRepository;
import ru.practicum.ewm.main.repository.EventRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void create_shouldSaveCategory_whenNameIsFree() {
        when(categoryRepository.existsByName("Концерты")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(1L);
            return category;
        });

        CategoryDto result = categoryService.create(new CategoryRequest("Концерты"));

        assertEquals(1L, result.getId());
        assertEquals("Концерты", result.getName());
    }

    @Test
    void create_shouldThrowConflict_whenNameAlreadyExists() {
        when(categoryRepository.existsByName("Концерты")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> categoryService.create(new CategoryRequest("Концерты")));

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void update_shouldChangeName_whenNameIsFree() {
        Category category = Category.builder().id(1L).name("Концерты").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameAndIdNot("Театры", 1L)).thenReturn(false);

        CategoryDto result = categoryService.update(1L, new CategoryRequest("Театры"));

        assertEquals(1L, result.getId());
        assertEquals("Театры", result.getName());
    }

    @Test
    void update_shouldThrowConflict_whenNameTakenByAnotherCategory() {
        Category category = Category.builder().id(1L).name("Концерты").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameAndIdNot("Театры", 1L)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> categoryService.update(1L, new CategoryRequest("Театры")));

        assertEquals("Концерты", category.getName());
    }

    @Test
    void update_shouldThrowNotFound_whenCategoryDoesNotExist() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> categoryService.update(1L, new CategoryRequest("Театры")));
    }

    @Test
    void delete_shouldDeleteCategory_whenItHasNoEvents() {
        Category category = Category.builder().id(1L).name("Концерты").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(eventRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.delete(1L);

        verify(categoryRepository).deleteById(1L);
    }

    @Test
    void delete_shouldThrowConflict_whenCategoryHasEvents() {
        Category category = Category.builder().id(1L).name("Концерты").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(eventRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.delete(1L));

        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    void delete_shouldThrowNotFound_whenCategoryDoesNotExist() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> categoryService.delete(1L));

        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    void getAll_shouldReturnPageFromRepository() {
        Category second = Category.builder().id(2L).name("Вторая").build();
        when(categoryRepository.findAllWithOffset(1, 1)).thenReturn(List.of(second));

        List<CategoryDto> result = categoryService.getAll(1, 1);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getId());
    }

    @Test
    void getById_shouldThrowNotFound_whenCategoryDoesNotExist() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> categoryService.getById(1L));
    }
}