package com.subtickets.servlet;

import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ReturningResponseHandler;
import com.subtickets.roomers.Roomers;

public class RoomersResponseHandler implements ReturningResponseHandler<Response, Roomers> {

    public Roomers handle(com.atlassian.sal.api.net.Response response) throws ResponseException {
        return response.getEntity(Roomers.class);
    }

}
