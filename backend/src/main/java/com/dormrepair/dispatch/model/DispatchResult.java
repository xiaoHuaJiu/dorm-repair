package com.dormrepair.dispatch.model;
public record DispatchResult(boolean success,Long workerId,DispatchFailureReason failureReason) {
 public static DispatchResult success(Long workerId){return new DispatchResult(true,workerId,null);}
 public static DispatchResult failure(DispatchFailureReason reason){return new DispatchResult(false,null,reason);}
}
