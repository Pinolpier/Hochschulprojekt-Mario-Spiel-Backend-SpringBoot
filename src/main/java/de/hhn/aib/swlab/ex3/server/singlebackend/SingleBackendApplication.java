package de.hhn.aib.swlab.ex3.server.singlebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SingleBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SingleBackendApplication.class, args);
    }

}
