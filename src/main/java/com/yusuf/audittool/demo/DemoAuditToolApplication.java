package com.yusuf.audittool.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication(scanBasePackages = "com.yusuf.audittool")
@PropertySource("classpath:demo/application-demo.properties")
public class DemoAuditToolApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoAuditToolApplication.class, args);
    }
}
