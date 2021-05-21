package com.arun.vaccine.vaccinetracker.scheduler;

import com.arun.vaccine.vaccinetracker.processor.InvokeCowinAPI;
import com.arun.vaccine.vaccinetracker.processor.SlotSchedulerProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

@Component
@Slf4j
public class FindSlotScheduler {

    @Value("#{${find.slot.districts}}")
    Map<String, String> districtsMap;

    @Autowired
    SlotSchedulerProcessor slotSchedulerProcessor;

    @Scheduled(fixedDelay = 120000)
    public void schedule(){
        log.info("Start scheduler..");

        districtsMap.entrySet().forEach(entry -> {
            log.info("Start scheduling for district: {}", entry.getKey());
            slotSchedulerProcessor.process(entry.getValue(), getDate());
        });

    }

    private String getDate(){
        String pattern = "dd-MM-yyyy";
        DateFormat df = new SimpleDateFormat(pattern);
        Date tomorrow = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(tomorrow);
        c.add(Calendar.DATE, 1);
        tomorrow = c.getTime();
       return df.format(tomorrow);
    }

}
