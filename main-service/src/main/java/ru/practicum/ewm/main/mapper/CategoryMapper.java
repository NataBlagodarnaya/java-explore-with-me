package ru.practicum.ewm.main.mapper;

import ru.practicum.ewm.main.dto.CategoryDto;
import ru.practicum.ewm.main.dto.CategoryRequest;
import ru.practicum.ewm.main.model.Category;

public class CategoryMapper {

    public static Category toCategory(CategoryRequest request) {
        return Category.builder()
                .name(request.getName())
                .build();
    }

    public static CategoryDto toCategoryDto(Category category) {
        return new CategoryDto(category.getId(), category.getName());
    }
}