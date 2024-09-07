package com.parcels.endpoint.mapper;

import com.parcels.endpoint.dto.out.GoalOut;
import com.parcels.goal.dto.GoalDto;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class GoalDtoToOutConverter implements Converter<GoalDto, GoalOut> {

    @Override
    public GoalOut convert(GoalDto source) {
        if (source == null) {
            return null;
        }
        return new GoalOut(source.id(), source.fullName(), source.goalName(), source.targetAmount(), source.currentAmount(), source.deadline());
    }
}
