package com.subtickets.servlet;

import javax.inject.Named;

import static com.subtickets.Constants.GET_VOTING_RESULTS_URL;

@Named
public class VotingResultsServlet extends RedirectingServlet {

    @Override
    protected String getURL() {
        return GET_VOTING_RESULTS_URL;
    }

}