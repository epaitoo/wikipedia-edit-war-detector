package com.epaitoo.springboot.entity;

public enum EditWarStatus {
    ACTIVE,      // War is currently happening
    RESOLVED,    // War has stopped (no recent edits)
    ESCALATING,  // Getting worse (more edits coming faster)
    COOLING_DOWN // Slowing down
}
