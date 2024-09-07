package com.parcels.category.converter;

import com.parcels.category.dto.CategoryDto;
import com.parcels.domain.Category;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CategoryToDtoConverter implements Converter<Category, CategoryDto> {
    @Override
    public CategoryDto convert(Category source) {
        if (source == null) {
            return null;
        }
        return new CategoryDto(
                source.getId(),
                source.getName());
    }
}
