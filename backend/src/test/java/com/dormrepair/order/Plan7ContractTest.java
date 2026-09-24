package com.dormrepair.order;
import com.dormrepair.order.dto.*;
import jakarta.validation.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
class Plan7ContractTest {
  private Validator validator;
  @BeforeEach void setup(){validator=Validation.buildDefaultValidatorFactory().getValidator();}
  @Test void evaluationValidation(){assertThat(validator.validate(new CreateRepairEvaluationRequest(0,null))).isNotEmpty();assertThat(validator.validate(new CreateRepairEvaluationRequest(6,null))).isNotEmpty();assertThat(validator.validate(new CreateRepairEvaluationRequest(5,"x".repeat(1001)))).isNotEmpty();assertThat(validator.validate(new CreateRepairEvaluationRequest(5,"好"))).isEmpty();}
  @Test void reworkValidation(){assertThat(validator.validate(new CreateReworkRequest(" ",null))).isNotEmpty();assertThat(validator.validate(new CreateReworkRequest("返工",Collections.nCopies(10,1L)))).isNotEmpty();assertThat(validator.validate(new CreateReworkRequest("返工",Arrays.asList((Long)null)))).isNotEmpty();assertThat(validator.validate(new CreateReworkRequest("返工",List.of(1L)))).isEmpty();}
}
