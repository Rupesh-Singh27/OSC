package org.orosoft;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Hello world!
 *
 */

@SpringBootApplication
@EnableScheduling
public class ProductApplication
{
    public static void main( String[] args )
    {
        SpringApplication.run(ProductApplication.class);
    }
}
