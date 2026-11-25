package ru.practicum.ewm.main.service;

import ru.practicum.ewm.main.dto.category.CategoryDto;
import ru.practicum.ewm.main.dto.category.CategoryRequest;

import java.util.List;

public interface CategoryService {

    CategoryDto create(CategoryRequest request);

    CategoryDto update(Long catId, CategoryRequest request);

    void delete(Long catId);

    List<CategoryDto> getAll(int from, int size);

    CategoryDto getById(Long catId);
}