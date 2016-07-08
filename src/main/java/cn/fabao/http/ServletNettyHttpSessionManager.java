/*
 * Copyright 2015-2020 msun.com All right reserved.
 */
package cn.fabao.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.http.Cookie;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class ServletNettyHttpSessionManager {

    private static final Logger log = LoggerFactory.getLogger(ServletNettyHttpSessionManager.class);

    private static String JSESSIONID = "JSESSIONID";
    static  ServletNettyHttpSessionStore servletNettyHttpSessionStore;
    static {
        servletNettyHttpSessionStore = ServletNettyHttpSessionStore.init();
    }
    public static NettyHttpSession getSession(NettyHttpServletRequest request,boolean create) {
        String jsId = request.getCookie(JSESSIONID);
        if (jsId != null) {
            NettyHttpSession session = servletNettyHttpSessionStore.findSession(jsId);
            if(session != null){
                session.access();
                if (!session.isInvalid()){
                    return session;
                }
            }

        }

        if(create){
            return createSession(request);
        }
        return null;
    }


    public static NettyHttpSession createSession(NettyHttpServletRequest request) {
        NettyHttpSession session = servletNettyHttpSessionStore.createSession(request.getServletContext());
        NettyHttpServletResponse response = (NettyHttpServletResponse) request.getServletResponse();
        Cookie cookie = new Cookie(JSESSIONID, session.getId());
        cookie.setPath("/");
        response.addCookie(cookie);
        return session;
    }

    public static void start(){
        final Map<String,NettyHttpSession> sessions = ServletNettyHttpSessionStore.sessions;
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        while (true){
                            Set<Map.Entry<String,NettyHttpSession>> entrySet = sessions.entrySet();
                            for(Map.Entry<String,NettyHttpSession> entry : entrySet) {
                                String jsId = entry.getKey();
                                NettyHttpSession nettyHttpSession = entry.getValue();
                                if(nettyHttpSession.isInvalid()){
                                    sessions.remove(jsId);
                                }}
                            try {
                                Thread.sleep(60*1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

        ).start();
    }



}
