package com.rk.inventory_management_system.config;

import com.rk.inventory_management_system.dtos.ProductDto;
import com.rk.inventory_management_system.entities.Product;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ModelMapperConfig {


    @Bean
    public ModelMapper getModelMapper() {
        return new ModelMapper();
    }

}

