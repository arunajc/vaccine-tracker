package com.arun.vaccine.vaccinetracker.processor;

import com.arun.vaccine.vaccinetracker.helpers.DistanceHelper;
import com.arun.vaccine.vaccinetracker.model.AvailableCenter;
import com.arun.vaccine.vaccinetracker.model.AvailableSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SlotSchedulerProcessor {

    @Autowired
    private InvokeCowinAPI invokeCowinAPI;

    @Autowired
    private DistanceHelper distanceHelper;

    @Value("${find.slot.min.age:0}")
    private int minAge;

    @Value("${find.slot.distance.within.km:0}")
    private int maxDistance;

    @Value("${find.slot.near.lat:0}")
    private double nearLat;

    @Value("${find.slot.near.long:0}")
    private double nearLong;

    @Async
    public void process(String districtId, String startDate){
        log.info("Start processing for districtId={}, startDate={}", districtId, startDate);
        Map<String, Object> cowinApiResponse = invokeCowinAPI.invokeCalendarByDistrict(districtId, startDate);

        List<AvailableCenter> availableCenters = findAvailableCenters(districtId, cowinApiResponse);

        if(null!= availableCenters && !availableCenters.isEmpty()) {
            log.info("Total number of available centers with the given filters are {}", availableCenters.size());
            availableCenters.stream().forEach(c -> log.info("Final availableCenter: {}", c));
        }

    }

    private  List<AvailableCenter> findAvailableCenters(String districtId, Map<String, Object> cowinApiResponse){
        log.info("getAvailableCenter: start processing for district={}", districtId);

        if(null!= cowinApiResponse.get("centers") &&
                !((List)cowinApiResponse.get("centers")).isEmpty()) {
            List<Map<String, Object>> centers = (List<Map<String, Object>>) cowinApiResponse.get("centers");
            if(centers.size() > 0){
                return centers.stream()
                        .filter(center -> filterCenterWithDistance(center))
                        .map(c -> getAvailableCenter(districtId, c))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } else{
                log.warn("Cowin response processing Err1: No centers available for districtId: {}", districtId);
                return null;
            }
        } else{
            log.warn("Cowin response processing Err2:  No centers available for districtId: {}", districtId);
            return null;
        }
    }

    //TODO: lat, long filtering
    private AvailableCenter getAvailableCenter(String districtId, Map<String, Object> center){
        String centerName;
        if(null!= center && !center.isEmpty()){
             centerName = (String) center.get("name");
        } else{
            log.warn("Cowin response processing :  Center is null for districtId: {}", districtId);
            return null;
        }
        log.info("getAvailableCenter: start processing center={}, district={}", centerName, districtId);
        if(null!= center.get("sessions") &&
                !((List)center.get("sessions")).isEmpty()){
            List<Map<String, Object>> sessions = (List<Map<String, Object>>) center.get("sessions");

            if(sessions.size() > 0){
                List<Map<String, Object>> sessionsFiltered = sessions.stream()
                        .filter(s -> (int)s.get("available_capacity") > 0)
                        .filter(minAge> 0? s ->  minAge >= (int)s.get("min_age_limit"):s -> true)
                        .collect(Collectors.toList());

                if(null!= sessionsFiltered && !sessionsFiltered.isEmpty() && sessionsFiltered.size() > 0){
                    log.info("getAvailableCenter::  Found {} session(s) with slots for center: {}",
                            sessionsFiltered.size(), centerName);

                   return mapCenterToAvailableCenter(districtId, centerName, center, sessionsFiltered);
                } else{
                    log.warn("Cowin response processing:  No sessions available with given filters(capacity>0, ageLimit etc.) for districtId: {}", districtId);
                    return null;
                }

            } else{
                log.warn("Cowin response processing Err1:  No sessions available for districtId: {}", districtId);
                return null;
            }
        } else{
            log.warn("Cowin response processing Err2:  No sessions available for districtId: {}", districtId);
            return null;
        }

    }

    private boolean filterCenterWithDistance(Map<String, Object> center ){

        if(maxDistance > 0 && nearLat !=0 &&nearLong !=0){
            log.info("filterCenterWithDistance:: Filter with distance enabled. maxDistance={} from lat={}, long={}", maxDistance, nearLat, nearLong);
            double centerLat = (Double) center.get("lat");
            log.info("filterCenterWithDistance:: Center lat={}", centerLat);
            double centerLong = (double) center.get("long");
            log.info("filterCenterWithDistance:: Center lat={}, long={}", centerLat, centerLong);
            double distance = distanceHelper.calculateDistance(nearLat, nearLong, centerLat, centerLong);
            if(distance > maxDistance){
                return false;
            }
        }
        return true;
    }

    private AvailableCenter mapCenterToAvailableCenter(String districtId, String centerName,
                                                       Map<String, Object> center,
                                                       List<Map<String, Object>> sessions){
        log.info("mapCenterToAvailableCenter: start mapping center={}, district={}", centerName, districtId);

        AvailableCenter availableCenter = new AvailableCenter();
        availableCenter.setCenterId((Integer) center.get("center_id"));
        availableCenter.setCenterName(centerName);
        availableCenter.setCenterAddress((String) center.get("address"));
        availableCenter.setCenterDistrict((String) center.get("district_name"));
        availableCenter.setCenterPin((Integer) center.get("pincode"));
        availableCenter.setVaccineFeeType((String) center.get("fee_type"));

        List<AvailableSession> availableSessions = new ArrayList<>();
        sessions.forEach(session -> {
            AvailableSession availableSession = new AvailableSession();
            availableSession.setSessionId((String) session.get("session_id"));
            availableSession.setDate((String) session.get("date"));
            availableSession.setAvailableCapacity((Integer) session.get("available_capacity"));
            availableSession.setAvailableCapacityDose1((Integer) session.get("available_capacity_dose1"));
            availableSession.setAvailableCapacityDose2((Integer) session.get("available_capacity_dose2"));
            availableSession.setAgeLimit((Integer) session.get("min_age_limit"));
            availableSession.setVaccine((String) session.get("vaccine"));
            availableSession.setSlots((List<String>) session.get("slots"));

            availableSessions.add(availableSession);

        });
        availableCenter.setAvailableSessions(availableSessions);

        log.info("mapCenterToAvailableCenter: done mapping center={}, district={}, availableCenter={}",
                centerName, districtId, availableCenter);
        return availableCenter;
    }
}
