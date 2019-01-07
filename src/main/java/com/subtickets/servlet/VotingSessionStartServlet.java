package com.subtickets.servlet;

import javax.inject.Named;

import static com.subtickets.Constants.START_VOTING_SESSION_URL;

@Named
public class VotingSessionStartServlet extends RedirectingServlet {

    @Override
    protected String getURL() {
        return START_VOTING_SESSION_URL;
    }

}