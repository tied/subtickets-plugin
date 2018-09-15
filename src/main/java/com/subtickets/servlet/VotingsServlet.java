package com.subtickets.servlet;

import javax.inject.Named;

import static com.subtickets.Constants.CREATE_SUBS_URL;

@Named
public class VotingsServlet extends RedirectingServlet {

    @Override
    protected String getURL() {
        return CREATE_SUBS_URL;
    }

}