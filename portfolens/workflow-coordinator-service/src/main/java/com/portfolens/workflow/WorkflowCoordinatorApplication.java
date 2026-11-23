package com.portfolens.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WorkflowCoordinatorApplication {
    public static void main(String[] args) { SpringApplication.run(WorkflowCoordinatorApplication.class, args); }
}
