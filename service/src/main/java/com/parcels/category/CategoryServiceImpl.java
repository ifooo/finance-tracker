package com.parcels.category;

import com.parcels.category.dto.CategoryDto;
import com.parcels.category.dto.CategoryUpdateCommand;
import com.parcels.domain.Category;
import com.parcels.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final ConversionService conversionService;

    @Override
    public CategoryDto save(CategoryUpdateCommand categoryUpdateCommand) {

        Category category = new Category();
        category.setName(categoryUpdateCommand.name());
        Category saved = categoryRepository.save(category);

        CategoryDto convertedResult = conversionService.convert(saved, CategoryDto.class);

        return convertedResult;
    }

    @Override
    public Boolean delete(Long id) {
        Category category = categoryRepository.findById(id).orElseThrow();
        categoryRepository.delete(category);
        return true;
    }

    @Override
    public Page<CategoryDto> getCategories(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Category> pagedCategories = categoryRepository.findAll(pageRequest);
        return pagedCategories.map(category -> conversionService.convert(category, CategoryDto.class));

    }

}
