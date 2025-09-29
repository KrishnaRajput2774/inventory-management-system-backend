package com.rk.inventory_management_system.dtos.ChatBotDtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class QueryRequest {

    private String table;
    private List<String> columns;
    private Map<String, Object> filters;
    private Integer limit;

}
