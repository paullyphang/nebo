/*
 * Copyright 2015-2020 msun.com All right reserved.
 */
package cn.fabao.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class ServletNettyHttpSessionStore implements HttpSessionStore {

    private static final Logger log = LoggerFactory.getLogger(ServletNettyHttpSessionStore.class);

    public static Map<String, NettyHttpSession> sessions = new ConcurrentHashMap<String, NettyHttpSession>();
    private static ServletNettyHttpSessionStore servletNettyHttpSessionStore ;

    private ServletNettyHttpSessionStore(){

    }

    private static class SingletonHolder {
        private static final ServletNettyHttpSessionStore servletNettyHttpSessionStore = new ServletNettyHttpSessionStore();
    }


    public NettyHttpSession createSession(ServletContext servletContext) {
        String sessionId = this.generateNewSessionId();
        NettyHttpSession session = new NettyHttpSession(servletContext,sessionId);
        sessions.put(sessionId, session);
        return session;
    }


    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }

    public NettyHttpSession findSession(String sessionId) {
        if (sessionId == null) return null;
        return sessions.get(sessionId);
    }

    protected  String generateNewSessionId() {
        return  System.currentTimeMillis() + "";
    }


    public static ServletNettyHttpSessionStore init(){
        return SingletonHolder.servletNettyHttpSessionStore;
    }


}
