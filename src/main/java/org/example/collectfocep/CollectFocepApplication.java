package org.example.collectfocep;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;

@SpringBootApplication(exclude = {
		RedisAutoConfiguration.class,
})
@ComponentScan(basePackages = {
		"org.example.collectfocep",
		"org.example.collectfocep.services",
		"org.example.collectfocep.services.impl",
		"org.example.collectfocep.config"
})
public class CollectFocepApplication {
	public static void main(String[] args) {
		SpringApplication.run(CollectFocepApplication.class, args);
	}
}
