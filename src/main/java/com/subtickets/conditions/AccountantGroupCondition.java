package com.subtickets.conditions;

import static com.subtickets.Constants.Groups.ACCOUNTANT;

public class AccountantGroupCondition extends UserInGroupCondition {

    @Override
    protected String getGroupName() {
        return ACCOUNTANT;
    }
}
