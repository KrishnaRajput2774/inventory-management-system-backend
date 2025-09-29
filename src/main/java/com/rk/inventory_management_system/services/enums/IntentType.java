package com.rk.inventory_management_system.services.enums;

public enum IntentType {
    DATABASE_QUERY,    // Needs SQL generation
    SIMPLE_CHAT,       // Direct streaming without DB
    COMPLEX_ANALYSIS   // Needs tools + analysis
}