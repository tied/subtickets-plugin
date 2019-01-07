package com.subtickets.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public abstract class RedirectingServlet extends JiraServlet {

    protected abstract String getURL();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String issueId = req.getParameter("id");
        sendRequest(getURL() + "?id=" + issueId);
        System.out.println(issueId);
        resp.sendRedirect(req.getHeader("referer"));
    }

}
