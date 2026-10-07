package com.ridehailing.dto;

import com.ridehailing.model.User;

public class UserResponse {
    private String id;
    private String name;
    private String phone;

    public UserResponse() {
    }

    public UserResponse(String id, String name, String phone) {
        this.id = id;
        this.name = name;
        this.phone = phone;
    }

    public static UserResponse fromModel(User user) {
        if (user == null) return null;
        return new UserResponse(user.getId(), user.getName(), user.getPhone());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
