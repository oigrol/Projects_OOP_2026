package com.weather.report.repositories;

import com.weather.report.model.entities.User;

public class UserRepository extends CRUDRepository<User, String> {

    public UserRepository() {
        super(User.class);
    }
    
}
