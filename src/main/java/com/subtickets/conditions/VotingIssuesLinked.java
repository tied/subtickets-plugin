package com.subtickets.conditions;

import static com.subtickets.Constants.VOTING_ISSUE_TYPE_NAME;

public class VotingIssuesLinked extends LinkedIssuesPresent {

    String getLinkedIssueType() {
        return VOTING_ISSUE_TYPE_NAME;
    }

}
