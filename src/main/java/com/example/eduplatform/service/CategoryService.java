package com.example.eduplatform.service;

import com.example.eduplatform.dto.CategoryRequest;
import com.example.eduplatform.dto.CategoryResponse;
import com.example.eduplatform.entity.Category;
import com.example.eduplatform.exception.ConflictException;
import com.example.eduplatform.exception.DuplicateResourceException;
import com.example.eduplatform.exception.NotFoundException;
import com.example.eduplatform.repository.CategoryRepository;
import com.example.eduplatform.repository.CourseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// категории курсов, обычный crud
@Service
@Transactional(readOnly = true)
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;

    public CategoryService(CategoryRepository categoryRepository, CourseRepository courseRepository) {
        this.categoryRepository = categoryRepository;
        this.courseRepository = courseRepository;
    }

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream().map(CategoryService::toResponse).toList();
    }

    public CategoryResponse findById(Long id) {
        return toResponse(getCategory(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Категория '" + request.name() + "' уже существует");
        }
        Category category = new Category(request.name(), request.description());
        Category saved = categoryRepository.save(category);
        log.info("Создана категория '{}'", saved.getName());
        return toResponse(saved);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getCategory(id);
        if (request.name() != null && !request.name().equalsIgnoreCase(category.getName())) {
            categoryRepository.findByName(request.name())
                    .filter(other -> !other.getId().equals(id))
                    .ifPresent(other -> {
                        throw new DuplicateResourceException("Категория '" + request.name() + "' уже существует");
                    });
            category.setName(request.name());
        }
        if (request.description() != null) {
            category.setDescription(request.description());
        }
        return toResponse(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = getCategory(id);
        if (courseRepository.existsByCategoryId(id)) {
            throw new ConflictException("Нельзя удалить категорию '" + category.getName()
                    + "': в ней есть курсы. Сначала удалите или перенесите курсы.");
        }
        categoryRepository.delete(category);
        log.info("Удалена категория '{}'", category.getName());
    }

    private Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + id + " не найдена"));
    }

    public static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }
}
