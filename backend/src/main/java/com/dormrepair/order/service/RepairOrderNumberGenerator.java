package com.dormrepair.order.service;
import org.springframework.stereotype.Component;
import java.time.LocalDate; import java.time.format.DateTimeFormatter; import java.util.UUID;
@Component public class RepairOrderNumberGenerator {
 public String next(){return "RO"+LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)+UUID.randomUUID().toString().replace("-","").substring(0,12).toUpperCase();}
}
