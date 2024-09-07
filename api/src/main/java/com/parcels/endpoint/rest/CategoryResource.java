package com.parcels.endpoint.rest;

import com.parcels.category.CategoryService;
import com.parcels.category.dto.CategoryDto;
import com.parcels.category.dto.CategoryUpdateCommand;
import com.parcels.endpoint.dto.PagedResponse;
import com.parcels.endpoint.dto.out.CategoryOut;
import com.parcels.endpoint.mapper.PageToPagedResponseOutConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/categories")
@CrossOrigin(maxAge = 3600, origins = "*")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Category Resource", description = "Endpoints for handling categories")
public class CategoryResource {
    private final CategoryService categoryService;
    private final ConversionService conversionService;
    private final PageToPagedResponseOutConverter<CategoryOut> pageToPagedResponseOutConverter;

    @Operation(summary = "Add a new category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category added"),
            @ApiResponse(responseCode = "400", description = "Invalid Category Provided")})
    @PostMapping
    public ResponseEntity<CategoryOut> addCategory(@Valid @RequestBody @Parameter(description = "Persist Category command must be valid") CategoryUpdateCommand categoryPersistCommand) {
        final CategoryDto categoryDto = categoryService.save(categoryPersistCommand);
        final CategoryOut categoryOut = conversionService.convert(categoryDto, CategoryOut.class);
        return ResponseEntity.ok(categoryOut);
    }

    @Operation(summary = "Delete a category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category deleted"),
            @ApiResponse(responseCode = "400", description = "Invalid Category Provided")})
    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteCategory(@PathVariable(name = "id") Long id, HttpServletRequest httpServletRequest) {
        final Boolean deleted = categoryService.delete(id);
        return ResponseEntity.ok(deleted);
    }

    @Operation(summary = "Get a list of categories")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of categories fetched")})
    @GetMapping
    public PagedResponse<CategoryOut> getCategories(
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "10") int size) {
        Page<CategoryDto> pagedCategories = categoryService.getCategories(page, size);
        PagedResponse<CategoryOut> pagedCategoryOut = convertToPagedResponseOut(pagedCategories);
        return pagedCategoryOut;
    }

    private PagedResponse<CategoryOut> convertToPagedResponseOut(Page<CategoryDto> pagedCategories) {
        final Page<CategoryOut> pagedOut = pagedCategories.map(categoryDto -> conversionService.convert(categoryDto, CategoryOut.class));
        return pageToPagedResponseOutConverter.convert(pagedOut);
    }
}