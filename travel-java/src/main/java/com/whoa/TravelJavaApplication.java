package com.whoa;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.whoa.mapper")
public class TravelJavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelJavaApplication.class, args);
    }

}
