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
public class AvailableCenter {

    private int centerId;
    private String centerName;
    private String centerAddress;
    private String centerDistrict;
    private int centerPin;
    private String vaccineFeeType;
    List<AvailableSession> availableSessions;
}
