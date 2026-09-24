package com.dormrepair.transfer.dto;
import com.dormrepair.transfer.enums.TransferReasonType;import jakarta.validation.constraints.*;
public record CreateTransferRequest(@NotBlank @Size(max=64) String bizNo,@NotNull TransferReasonType reasonType,@Size(max=500) String reason){@AssertTrue(message="其他原因必须填写详细说明")public boolean isReasonValid(){return reasonType!=TransferReasonType.OTHER||(reason!=null&&!reason.isBlank());}}
