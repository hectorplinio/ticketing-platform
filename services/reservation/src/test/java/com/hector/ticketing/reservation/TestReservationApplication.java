package com.hector.ticketing.reservation;

import org.springframework.boot.SpringApplication;

public class TestReservationApplication {

    public static void main(String[] args) {
        SpringApplication.from(ReservationApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
