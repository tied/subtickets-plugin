package com.subtickets.conditions;

import static com.subtickets.Constants.ACCOUNTANT_ROLE_NAME;

public class AccountantRoleCondition extends HasRoleCondition {

    @Override
    protected String getRole() {
        return ACCOUNTANT_ROLE_NAME;
    }
}
