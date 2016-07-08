/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.fabao.http;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import org.springframework.util.Assert;

/**
 * Mock implementation of the {@link javax.servlet.http.HttpSession} interface.
 *
 * <p>As of Spring 4.0, this set of mocks is designed on a Servlet 3.0 baseline.
 *
 * <p>Used for testing the web framework; also useful for testing application
 * controllers.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Mark Fisher
 * @author Sam Brannen
 * @since 1.0.2
 */
@SuppressWarnings("deprecation")
public class NettyHttpSession implements HttpSession {

    public static final String SESSION_COOKIE_NAME = "JSESSION";


    private static int nextId = 1;

    private String id;

    private final long creationTime = System.currentTimeMillis();

    private int maxInactiveInterval;

    private long lastAccessedTime = System.currentTimeMillis();

    private final ServletContext servletContext;

    private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();

    private boolean invalid =  false;

    private boolean isNew = true;

    private Lock lock = new ReentrantLock();

    /**
     * Create a new MockHttpSession.
     *
     * @param servletContext the ServletContext that the session runs in
     */
    public NettyHttpSession(ServletContext servletContext) {
        this(servletContext, null);
    }

    /**
     * Create a new MockHttpSession.
     *
     * @param servletContext the ServletContext that the session runs in
     * @param id a unique identifier for this session
     */
    public NettyHttpSession(ServletContext servletContext, String id) {
        this.servletContext = servletContext;
        this.id = (id != null ? id : Integer.toString(nextId++));
        this.maxInactiveInterval = 30 * 1000;
    }

    @Override
    public long getCreationTime() {
        assertIsValid();
        return this.creationTime;
    }

    @Override
    public String getId() {
        return this.id;
    }

    /**
     * As of Servlet 3.1 the id of a session can be changed.
     * @return the new session id.
     * @since 4.0.3
     */
    public String changeSessionId() {
        this.id = Integer.toString(nextId++);
        return this.id;
    }

    public void access() {
        if(invalid)return ;
        lock.lock();
        try{
            this.lastAccessedTime = System.currentTimeMillis();
            this.isNew = false;
        }finally {
            lock.unlock();
        }
    }

    @Override
    public long getLastAccessedTime() {
        return this.lastAccessedTime;
    }

    @Override
    public ServletContext getServletContext() {
        return this.servletContext;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.maxInactiveInterval = interval;
    }

    @Override
    public int getMaxInactiveInterval() {
        return this.maxInactiveInterval;
    }

    @Override
    public javax.servlet.http.HttpSessionContext getSessionContext() {
        throw new UnsupportedOperationException("getSessionContext");
    }

    @Override
    public Object getAttribute(String name) {
        assertIsValid();
        Assert.notNull(name, "Attribute name must not be null");
        return this.attributes.get(name);
    }

    @Override
    public Object getValue(String name) {
        return getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        assertIsValid();
        return Collections.enumeration(new LinkedHashSet<String>(this.attributes.keySet()));
    }

    @Override
    public String[] getValueNames() {
        assertIsValid();
        return this.attributes.keySet().toArray(new String[this.attributes.size()]);
    }

    @Override
    public void setAttribute(String name, Object value) {
        assertIsValid();
        Assert.notNull(name, "Attribute name must not be null");
        if (value != null) {
            this.attributes.put(name, value);
            if (value instanceof HttpSessionBindingListener) {
                ((HttpSessionBindingListener) value).valueBound(new HttpSessionBindingEvent(this, name, value));
            }
        }
        else {
            removeAttribute(name);
        }
    }

    @Override
    public void putValue(String name, Object value) {
        setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        assertIsValid();
        Assert.notNull(name, "Attribute name must not be null");
        Object value = this.attributes.remove(name);
        if (value instanceof HttpSessionBindingListener) {
            ((HttpSessionBindingListener) value).valueUnbound(new HttpSessionBindingEvent(this, name, value));
        }
    }

    @Override
    public void removeValue(String name) {
        removeAttribute(name);
    }

    /**
     * Clear all of this session's attributes.
     */
    public void clearAttributes() {
        for (Iterator<Map.Entry<String, Object>> it = this.attributes.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Object> entry = it.next();
            String name = entry.getKey();
            Object value = entry.getValue();
            it.remove();
            if (value instanceof HttpSessionBindingListener) {
                ((HttpSessionBindingListener) value).valueUnbound(new HttpSessionBindingEvent(this, name, value));
            }
        }
    }

    /**
     * Invalidates this session then unbinds any objects bound to it.
     *
     * @throws IllegalStateException if this method is called on an already invalidated session
     */
    @Override
    public void invalidate() {
        assertIsValid();
        this.invalid = true;
        //this.invalid.set(true);
        clearAttributes();
    }

    public boolean isInvalid(){
        if(invalid)return invalid;
        try{
            lock.lock();
            Long lastAccessedTime =  this.getLastAccessedTime();
            Long currentTime = System.currentTimeMillis();
            if (currentTime - lastAccessedTime > this.getMaxInactiveInterval()){
                invalid = true;
                return invalid;
            }

        }finally {
            lock.unlock();
        }
        return invalid;
    }

    /**
     * Convenience method for asserting that this session has not been
     * {@linkplain #invalidate() invalidated}.
     *
     * @throws IllegalStateException if this session has been invalidated
     */
    private void assertIsValid() {
        if (isInvalid()) {
            throw new IllegalStateException("The session has already been invalidated");
        }
    }

    public void setNew(boolean value) {
        this.isNew = value;
    }

    @Override
    public boolean isNew() {
        assertIsValid();
        return this.isNew;
    }

    /**
     * Serialize the attributes of this session into an object that can be
     * turned into a byte array with standard Java serialization.
     *
     * @return a representation of this session's serialized state
     */
    public Serializable serializeState() {
        HashMap<String, Serializable> state = new HashMap<String, Serializable>();
        for (Iterator<Map.Entry<String, Object>> it = this.attributes.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Object> entry = it.next();
            String name = entry.getKey();
            Object value = entry.getValue();
            it.remove();
            if (value instanceof Serializable) {
                state.put(name, (Serializable) value);
            }
            else {
                // Not serializable... Servlet containers usually automatically
                // unbind the attribute in this case.
                if (value instanceof HttpSessionBindingListener) {
                    ((HttpSessionBindingListener) value).valueUnbound(new HttpSessionBindingEvent(this, name, value));
                }
            }
        }
        return state;
    }

    /**
     * Deserialize the attributes of this session from a state object created by
     * {@link #serializeState()}.
     *
     * @param state a representation of this session's serialized state
     */
    @SuppressWarnings("unchecked")
    public void deserializeState(Serializable state) {
        Assert.isTrue(state instanceof Map, "Serialized state needs to be of type [java.util.Map]");
        this.attributes.putAll((Map<String, Object>) state);
    }



}
