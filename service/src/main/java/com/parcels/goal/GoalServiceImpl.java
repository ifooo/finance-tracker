package com.parcels.goal;

import com.parcels.domain.Goal;
import com.parcels.goal.dto.GoalDto;
import com.parcels.goal.dto.GoalUpdateCommand;
import com.parcels.repository.GoalRepository;
import com.parcels.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GoalServiceImpl implements GoalService{
    private final GoalRepository goalRepository;
    private final ConversionService conversionService;
    private final UserAccountRepository userAccountRepository;

    @Override
    public GoalDto save(GoalUpdateCommand goalPersistCommand) {
        final Goal goal = new Goal();
        goal.setName(goalPersistCommand.goalName());
        goal.setDeadline(goalPersistCommand.deadline());
        goal.setCurrentAmount(goalPersistCommand.currentAmount());
        goal.setTargetAmount(goalPersistCommand.targetAmount());
        goal.setUserAccount(userAccountRepository.findById(1L).orElseThrow());
        Goal saved = goalRepository.save(goal);
        GoalDto result = conversionService.convert(saved, GoalDto.class);
        return result;
    }
}
