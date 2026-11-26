package ru.practicum.ewm.main.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.main.dto.category.CategoryDto;
import ru.practicum.ewm.main.dto.category.CategoryRequest;
import ru.practicum.ewm.main.model.Category;

@UtilityClass
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