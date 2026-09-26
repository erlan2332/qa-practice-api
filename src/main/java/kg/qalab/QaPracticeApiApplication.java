package kg.qalab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class QaPracticeApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(QaPracticeApiApplication.class, args);
    }
}
