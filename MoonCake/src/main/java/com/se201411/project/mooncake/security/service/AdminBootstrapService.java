package com.se201411.project.mooncake.security.service;

public interface AdminBootstrapService {

    boolean createIfAbsent(String email, String password);
}
