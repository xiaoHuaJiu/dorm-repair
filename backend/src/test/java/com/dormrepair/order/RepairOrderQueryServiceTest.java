package com.dormrepair.order;

import com.dormrepair.domain.mapper.RepairOrderMapper;
import com.dormrepair.order.dto.RepairOrderQueryRequest;
import com.dormrepair.order.model.RepairOrderQueryCondition;
import com.dormrepair.order.service.RepairOrderQueryScopeFactory;
import com.dormrepair.order.service.RepairOrderQueryService;
import com.dormrepair.order.vo.RepairOrderListItem;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RepairOrderQueryServiceTest {
    @Test void computesTimeoutFlagsOnServer(){
        RepairOrderMapper mapper=mock(RepairOrderMapper.class); RepairOrderQueryScopeFactory scopes=mock(RepairOrderQueryScopeFactory.class);
        RepairOrderQueryRequest request=new RepairOrderQueryRequest(); RepairOrderQueryCondition condition=new RepairOrderQueryCondition(request);
        when(scopes.create(request)).thenReturn(condition);
        RepairOrderListItem item=new RepairOrderListItem(); item.setStatus(1); item.setAcceptDeadline(LocalDateTime.now().minusMinutes(1));
        when(mapper.selectPage(condition)).thenReturn(List.of(item));
        var result=new RepairOrderQueryService(mapper,scopes).page(request);
        assertThat(result.getRecords()).singleElement().extracting(RepairOrderListItem::getAcceptTimeout).isEqualTo(true);
    }
}
