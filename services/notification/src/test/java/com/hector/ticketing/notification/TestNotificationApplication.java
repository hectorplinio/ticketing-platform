package com.hector.ticketing.notification;

import org.springframework.boot.SpringApplication;

public class TestNotificationApplication {

    public static void main(String[] args) {
        SpringApplication.from(NotificationApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
