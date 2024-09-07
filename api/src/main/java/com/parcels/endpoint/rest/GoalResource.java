package com.parcels.endpoint.rest;

import com.parcels.endpoint.dto.out.GoalOut;
import com.parcels.goal.GoalService;
import com.parcels.goal.dto.GoalDto;
import com.parcels.goal.dto.GoalUpdateCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
@Slf4j
public class GoalResource {
    private final GoalService goalService;
    private final ConversionService conversionService;

    @PostMapping
    public ResponseEntity<GoalOut> addGoal(@Valid @RequestBody GoalUpdateCommand goalPersistCommand) {
        GoalDto result = goalService.save(goalPersistCommand);
        GoalOut convertedResult = conversionService.convert(result, GoalOut.class);
        return ResponseEntity.ok(convertedResult);
    }
}
