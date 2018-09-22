package com.subtickets.servlet;

import javax.inject.Named;

import static com.subtickets.Constants.GENERATE_VOTING_TEMPLATE_URL;

@Named
public class VotingTemplateGenServlet extends RedirectingServlet {

    @Override
    protected String getURL() {
        return GENERATE_VOTING_TEMPLATE_URL;
    }

}