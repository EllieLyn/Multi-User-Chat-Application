package org.example;

public interface UserStatusListener {
    void online(String login);
    void offline(String login);
}
