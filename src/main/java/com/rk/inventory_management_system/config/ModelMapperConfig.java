package com.rk.inventory_management_system.config;

import com.rk.inventory_management_system.dtos.ProductDto;
import com.rk.inventory_management_system.entities.Product;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {


    @Bean
    public ModelMapper getModelMapper() {

        ModelMapper modelMapper = new ModelMapper();
//
//        modelMapper.getConfiguration().setAmbiguityIgnored(true);
//
//        modelMapper.typeMap(ProductDto.class, Product.class)
//                .addMappings(mapper->{
//                    mapper.skip(Product::setId);
//                    mapper.skip(Product::setSupplier);
//                    mapper.skip(Product::setCategory);
//                });

        return modelMapper;
    }

}
