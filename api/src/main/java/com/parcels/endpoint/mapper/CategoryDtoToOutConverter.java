package com.parcels.endpoint.mapper;

import com.parcels.category.dto.CategoryDto;
import com.parcels.endpoint.dto.out.CategoryOut;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CategoryDtoToOutConverter implements Converter<CategoryDto, CategoryOut> {
    @Override
    public CategoryOut convert(CategoryDto source) {
        if (source == null) {
            return null;
        }
        return new CategoryOut(source.id(), source.name());
    }
}
