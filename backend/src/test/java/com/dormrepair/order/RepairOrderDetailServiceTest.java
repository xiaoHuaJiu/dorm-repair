package com.dormrepair.order;

import com.dormrepair.common.enums.ResultCodeEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.domain.entity.RepairOrder;
import com.dormrepair.domain.mapper.*;
import com.dormrepair.order.service.RepairOrderAccessService;
import com.dormrepair.order.service.RepairOrderDetailService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RepairOrderDetailServiceTest {
    @Test void checksAccessBeforeReadingChildren(){
        RepairOrderMapper orders=mock(RepairOrderMapper.class); RepairOrder order=new RepairOrder(); order.setId(1L); when(orders.selectById(1L)).thenReturn(order);
        RepairOrderAccessService access=mock(RepairOrderAccessService.class); doThrow(new BusinessException(ResultCodeEnum.ORDER_ACCESS_DENIED)).when(access).checkViewPermission(order);
        RepairProcessRecordMapper processes=mock(RepairProcessRecordMapper.class); RepairMaterialUsageMapper materials=mock(RepairMaterialUsageMapper.class);
        RepairReworkRecordMapper reworks=mock(RepairReworkRecordMapper.class); RepairEvaluationMapper evaluations=mock(RepairEvaluationMapper.class); RepairOrderFlowMapper flows=mock(RepairOrderFlowMapper.class);
        RepairOrderDetailService service=new RepairOrderDetailService(orders,access,processes,materials,reworks,evaluations,flows);
        assertThatThrownBy(()->service.getDetail(1L)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(processes,materials,reworks,evaluations,flows);
    }

    @Test void missingAssociationsBecomeEmptyCollectionsAndNullEvaluation(){
        RepairOrderMapper orders=mock(RepairOrderMapper.class); RepairOrder order=new RepairOrder(); order.setId(1L); when(orders.selectById(1L)).thenReturn(order);
        RepairOrderAccessService access=mock(RepairOrderAccessService.class); RepairProcessRecordMapper processes=mock(RepairProcessRecordMapper.class);
        RepairMaterialUsageMapper materials=mock(RepairMaterialUsageMapper.class); RepairReworkRecordMapper reworks=mock(RepairReworkRecordMapper.class);
        RepairEvaluationMapper evaluations=mock(RepairEvaluationMapper.class); RepairOrderFlowMapper flows=mock(RepairOrderFlowMapper.class);
        when(processes.selectByOrderId(1L)).thenReturn(List.of()); when(materials.selectByOrderId(1L)).thenReturn(List.of());
        when(reworks.selectByOrderId(1L)).thenReturn(List.of()); when(flows.selectByOrderId(1L)).thenReturn(List.of());
        var detail=new RepairOrderDetailService(orders,access,processes,materials,reworks,evaluations,flows).getDetail(1L);
        assertThat(detail.getProcessRecords()).isEmpty(); assertThat(detail.getMaterialRecords()).isEmpty();
        assertThat(detail.getReworkRecords()).isEmpty(); assertThat(detail.getFlows()).isEmpty(); assertThat(detail.getEvaluation()).isNull();
    }

    @Test void studentDetailDoesNotExposeMaterialRecords(){
        RepairOrderMapper orders=mock(RepairOrderMapper.class); RepairOrder order=new RepairOrder(); order.setId(1L); when(orders.selectById(1L)).thenReturn(order);
        RepairOrderAccessService access=mock(RepairOrderAccessService.class); when(access.currentViewerIsStudent()).thenReturn(true);
        RepairProcessRecordMapper processes=mock(RepairProcessRecordMapper.class); RepairMaterialUsageMapper materials=mock(RepairMaterialUsageMapper.class);
        RepairReworkRecordMapper reworks=mock(RepairReworkRecordMapper.class); RepairEvaluationMapper evaluations=mock(RepairEvaluationMapper.class); RepairOrderFlowMapper flows=mock(RepairOrderFlowMapper.class);
        when(processes.selectByOrderId(1L)).thenReturn(List.of()); when(reworks.selectByOrderId(1L)).thenReturn(List.of()); when(flows.selectByOrderId(1L)).thenReturn(List.of());
        var detail=new RepairOrderDetailService(orders,access,processes,materials,reworks,evaluations,flows).getDetail(1L);
        assertThat(detail.getMaterialRecords()).isEmpty(); verifyNoInteractions(materials);
    }
}
