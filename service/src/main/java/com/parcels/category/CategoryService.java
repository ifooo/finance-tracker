package com.parcels.category;

import com.parcels.category.dto.CategoryDto;
import com.parcels.category.dto.CategoryUpdateCommand;
import org.springframework.data.domain.Page;

public interface CategoryService {

    CategoryDto save(CategoryUpdateCommand categoryUpdateCommand);

    Boolean delete(Long id);

    Page<CategoryDto> getCategories(int page, int size);

}
