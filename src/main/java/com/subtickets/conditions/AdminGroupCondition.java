package com.subtickets.conditions;

import static com.subtickets.Constants.Groups.ADMIN;

public class AdminGroupCondition extends UserInGroupCondition {

    @Override
    protected String getGroupName() {
        return ADMIN;
    }

}
