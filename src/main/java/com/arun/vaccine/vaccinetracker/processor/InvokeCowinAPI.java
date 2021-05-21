package com.arun.vaccine.vaccinetracker.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class InvokeCowinAPI {

    private RestTemplate restTemplate;

    private String cowinCalendarByDistrictUrl;
    private ObjectMapper objectMapper;

    @Value("${api.user.agent}")
    private String userAgent;

    @Autowired
    public InvokeCowinAPI(RestTemplate restTemplate,
                          @Value("${cowin.url.calendarByDistrict}") String cowinCalendarByDistrictUrl,
                          ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.cowinCalendarByDistrictUrl = cowinCalendarByDistrictUrl;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> invokeCalendarByDistrict(String districtId, String startDate){

        log.info("invokeCalendarByDistrict:: Start for districtId={}, startDate={}", districtId, startDate);
        HttpHeaders headers = new HttpHeaders();
        //headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set("User-agent", userAgent);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(cowinCalendarByDistrictUrl)
                .queryParam("district_id", districtId)
                .queryParam("date", startDate);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response;
        try {
            String url = builder.toUriString();
            log.info("invokeCalendarByDistrict:: invoking CalendarByDistrict API {}", url);
            response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class);

            log.info("invokeCalendarByDistrict:: CalendarByDistrict API invoked with httpStatusCode: {} for districtId={}",
                    null!= response? response.getStatusCodeValue(): "NULL", districtId);
        } catch(Exception ex){
            log.error("invokeCalendarByDistrict:: Exception while invoking CalendarByDistrict for districtId={}",
                    districtId, ex);
            return null;
        }

        if(response!= null && response.hasBody()){
            log.debug("invokeCalendarByDistrict:: CalendarByDistrict API response (districtId={}): {}",
                    districtId, response.getBody());
            try {
                return objectMapper.readValue(response.getBody(), HashMap.class);
            } catch (JsonProcessingException e) {
                log.error("invokeCalendarByDistrict:: Exception while converting CalendarByDistrict response to Map for districtId={}",
                        districtId, e);
            }

        } else{
            log.error("invokeCalendarByDistrict:: No response from CalendarByDistrict API for districtId={}", districtId);
        }
        return null;
    }
}
