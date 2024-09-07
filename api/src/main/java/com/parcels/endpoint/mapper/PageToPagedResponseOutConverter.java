package com.parcels.endpoint.mapper;

import com.parcels.endpoint.dto.PagedResponse;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class PageToPagedResponseOutConverter<T> implements Converter<Page<T>, PagedResponse<T>> {
    @Override
    public PagedResponse<T> convert(Page<T> source) {
        if (source == null) {
            return null;
        }
        return new PagedResponse<>(
                source.getContent(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.isLast());
    }
}
