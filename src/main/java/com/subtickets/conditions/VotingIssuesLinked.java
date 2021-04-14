package com.subtickets.conditions;

import static com.subtickets.Constants.IssueTypesNames.VOTING;

public class VotingIssuesLinked extends LinkedIssuesPresent {

    String getLinkedIssueType() {
        return VOTING;
    }

}
