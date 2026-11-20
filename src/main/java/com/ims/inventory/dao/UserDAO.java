package com.ims.inventory.dao;

import java.util.List;
import java.util.Optional;
import com.ims.inventory.model.User;

public interface UserDAO {
    void saveUser(User user);
    Optional<User> findByUsername(String username);
    List<User> getAllUsers();
    boolean deleteUser(long userId);
    boolean updatePassword(String username, String newHashedPassword);
    boolean updateUserStatus(long userId, String status);
    boolean updateUserProfile(String username, String email, String phone);
}