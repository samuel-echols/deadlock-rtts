package dim.deadlockrts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DeadLockRtsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeadLockRtsApplication.class, args);
    }

}
