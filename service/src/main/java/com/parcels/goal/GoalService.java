package com.parcels.goal;

import com.parcels.goal.dto.GoalDto;
import com.parcels.goal.dto.GoalUpdateCommand;

public interface GoalService {

    GoalDto save(GoalUpdateCommand goalPersistCommand);
}
