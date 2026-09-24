package com.dormrepair.transfer;

import com.dormrepair.transfer.dto.CreateTransferRequest;
import com.dormrepair.transfer.dto.ReviewTransferRequest;
import com.dormrepair.transfer.enums.TransferReasonType;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TransferContractTest {
    private final jakarta.validation.Validator validator=Validation.buildDefaultValidatorFactory().getValidator();

    @Test void otherReasonRequiresDescription(){
        assertThat(validator.validate(new CreateTransferRequest("biz-1",TransferReasonType.OTHER,null))).isNotEmpty();
        assertThat(validator.validate(new CreateTransferRequest("biz-1",TransferReasonType.NO_SKILL,null))).isEmpty();
    }

    @Test void reviewRequiresBizNoAndAction(){
        assertThat(validator.validate(new ReviewTransferRequest("",null,null))).isNotEmpty();
    }
}
