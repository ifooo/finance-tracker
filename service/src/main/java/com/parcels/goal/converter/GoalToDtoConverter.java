package com.parcels.goal.converter;

import com.parcels.domain.Goal;
import com.parcels.goal.dto.GoalDto;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class GoalToDtoConverter implements Converter<Goal, GoalDto> {
    @Override
    public GoalDto convert(Goal source) {
        if (source == null) {
            return null;
        }
        return new GoalDto(
                source.getId(),
                source.getUserAccount().getFullName(),
                source.getName(),
                source.getTargetAmount(),
                source.getCurrentAmount(),
                source.getDeadline());
    }
}
