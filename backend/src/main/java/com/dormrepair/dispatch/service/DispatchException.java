package com.dormrepair.dispatch.service;
import com.dormrepair.dispatch.model.DispatchFailureReason;
public class DispatchException extends RuntimeException {
 private final DispatchFailureReason reason;
 public DispatchException(DispatchFailureReason reason,String message){super(message);this.reason=reason;}
 public DispatchFailureReason getReason(){return reason;}
}
