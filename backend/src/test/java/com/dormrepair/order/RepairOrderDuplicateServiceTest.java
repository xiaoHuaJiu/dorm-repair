package com.dormrepair.order;
import com.dormrepair.domain.mapper.RepairOrderMapper;
import com.dormrepair.order.dto.RepairOrderDuplicateCheckRequest;
import com.dormrepair.order.service.*;
import com.dormrepair.order.vo.SuspectedRepairOrder;
import org.junit.jupiter.api.Test;
import java.time.*; import java.util.List;
import static org.assertj.core.api.Assertions.assertThat; import static org.mockito.Mockito.*;
class RepairOrderDuplicateServiceTest {
 @Test void usesInclusiveTwentyFourHourBoundaryAndOpenStatuses(){
  var mapper=mock(RepairOrderMapper.class); var validator=mock(RepairOrderSubmissionValidator.class);
  var now=LocalDateTime.of(2026,9,24,10,0); var clock=Clock.fixed(now.atZone(ZoneId.systemDefault()).toInstant(),ZoneId.systemDefault());
  var req=new RepairOrderDuplicateCheckRequest();req.setCampusId(1L);req.setAreaId(2L);req.setBuildingId(3L);req.setRoomId(4L);req.setFaultTypeId(5L);
  when(mapper.selectSuspectedDuplicateOrders(any(),any(),any(),any(),any(),any(),any())).thenReturn(List.of(new SuspectedRepairOrder()));
  var result=new RepairOrderDuplicateService(mapper,validator,clock).check(req);
  assertThat(result.duplicate()).isTrue(); verify(validator).validate(req); verify(mapper).selectSuspectedDuplicateOrders(1L,2L,3L,4L,5L,now.minusHours(24),List.of(0,1,2,3,4,5));
 }
}
