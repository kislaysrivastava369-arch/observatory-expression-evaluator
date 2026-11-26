package com.observatory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Observatory — Expression Evaluation and Visualization System.
 * <p>
 * <b>API mode</b> (default): starts a web server on port 8080.<br>
 * <b>CLI mode</b>: run with {@code --spring.profiles.active=cli} for
 * an interactive terminal interface.
 */
@SpringBootApplication
public class ObservatoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ObservatoryApplication.class, args);
    }
}
