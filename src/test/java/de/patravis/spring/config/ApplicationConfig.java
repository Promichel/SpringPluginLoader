package de.patravis.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Bean
    public SampleClass sampleClass() {
        return new SampleClass();
    }

}
