package com.dormrepair.transfer.dto;
import com.dormrepair.transfer.enums.TransferReviewAction;import jakarta.validation.constraints.*;
public record ReviewTransferRequest(@NotBlank @Size(max=64) String bizNo,@NotNull TransferReviewAction action,@Size(max=500) String remark){}
