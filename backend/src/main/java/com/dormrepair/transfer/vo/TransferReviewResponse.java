package com.dormrepair.transfer.vo;public record TransferReviewResponse(Long requestId,boolean reviewSuccess,boolean dispatchSuccess,Long newAssigneeId,String failureReason,boolean replayed){}
