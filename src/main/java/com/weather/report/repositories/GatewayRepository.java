package com.weather.report.repositories;

import com.weather.report.model.entities.Gateway;

public class GatewayRepository extends CRUDRepository<Gateway, String> {

    public GatewayRepository() {
        super(Gateway.class);
    }

}
