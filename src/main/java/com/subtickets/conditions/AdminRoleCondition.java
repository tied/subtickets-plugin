package com.subtickets.conditions;

import static com.subtickets.Constants.ADMIN_ROLE_NAME;

public class AdminRoleCondition extends HasRoleCondition {

    @Override
    protected String getRole() {
        return ADMIN_ROLE_NAME;
    }

}
