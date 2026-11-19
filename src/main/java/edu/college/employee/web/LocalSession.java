package edu.college.employee.web;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LocalSession {
    private static final Map<String, LocalSession> SESSIONS = new ConcurrentHashMap<>();
    private final Map<String, Object> values = new ConcurrentHashMap<>();
    private volatile String id;
    private volatile long touched = System.currentTimeMillis();
    private LocalSession(String id) { this.id = id; }
    static LocalSession find(String id) {
        LocalSession s = id == null ? null : SESSIONS.get(id);
        if (s != null && System.currentTimeMillis() - s.touched > 1_800_000) { s.invalidate(); return null; }
        if (s != null) s.touched = System.currentTimeMillis();
        return s;
    }
    static LocalSession create() { String id = UUID.randomUUID().toString(); LocalSession s = new LocalSession(id); SESSIONS.put(id, s); return s; }
    public String id() { return id; }
    public Object getAttribute(String key) { return values.get(key); }
    public void setAttribute(String key, Object value) { values.put(key, value); }
    public void setMaxInactiveInterval(int seconds) {}
    void rotate() { SESSIONS.remove(id); id = UUID.randomUUID().toString(); SESSIONS.put(id, this); }
    static void invalidateOtherSessions(String username, LocalSession keep) {
        SESSIONS.values().stream().filter(session -> session != keep)
                .filter(session -> session.getAttribute("user") instanceof Web.UserSession user
                        && user.username().equalsIgnoreCase(username))
                .toList().forEach(LocalSession::invalidate);
    }
    public void invalidate() { SESSIONS.remove(id); values.clear(); }
}
