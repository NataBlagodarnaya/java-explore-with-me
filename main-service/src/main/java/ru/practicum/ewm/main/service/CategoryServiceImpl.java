package ru.practicum.ewm.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.dto.category.CategoryDto;
import ru.practicum.ewm.main.dto.category.CategoryRequest;
import ru.practicum.ewm.main.exception.ConflictException;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.mapper.CategoryMapper;
import ru.practicum.ewm.main.model.Category;
import ru.practicum.ewm.main.repository.CategoryRepository;
import ru.practicum.ewm.main.repository.EventRepository;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CategoryDto create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new ConflictException("Category name already exists: " + request.getName());
        }
        Category saved = categoryRepository.save(CategoryMapper.toCategory(request));
        log.info("Создана категория id={}", saved.getId());
        return CategoryMapper.toCategoryDto(saved);
    }

    @Override
    @Transactional
    public CategoryDto update(Long catId, CategoryRequest request) {
        Category category = findCategory(catId);
        if (categoryRepository.existsByNameAndIdNot(request.getName(), catId)) {
            throw new ConflictException("Category name already exists: " + request.getName());
        }
        category.setName(request.getName());
        log.info("Обновлена категория id={}", catId);
        return CategoryMapper.toCategoryDto(category);
    }

    @Override
    @Transactional
    public void delete(Long catId) {
        findCategory(catId);
        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("The category is not empty");
        }
        categoryRepository.deleteById(catId);
        log.info("Удалена категория id={}", catId);
    }

    @Override
    public List<CategoryDto> getAll(int from, int size) {
        return categoryRepository.findAllWithOffset(from, size).stream()
                .map(CategoryMapper::toCategoryDto)
                .toList();
    }

    @Override
    public CategoryDto getById(Long catId) {
        return CategoryMapper.toCategoryDto(findCategory(catId));
    }

    private Category findCategory(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
    }
}
