package com.arun.vaccine.vaccinetracker.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class AvailableSession {

    private String sessionId;
    private String date;
    private int availableCapacity;
    private int availableCapacityDose1;
    private int availableCapacityDose2;
    private int ageLimit;
    private String vaccine;
    private List<String> slots;
}
