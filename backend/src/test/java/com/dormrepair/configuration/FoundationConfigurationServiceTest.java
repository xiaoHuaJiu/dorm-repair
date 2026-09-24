package com.dormrepair.configuration;

import com.dormrepair.area.dto.AreaCreateRequest;
import com.dormrepair.area.service.AreaService;
import com.dormrepair.common.enums.ResultCodeEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.domain.entity.RepairArea;
import com.dormrepair.domain.entity.RepairFaultType;
import com.dormrepair.domain.entity.RepairWorkSchedule;
import com.dormrepair.domain.mapper.RepairAreaMapper;
import com.dormrepair.domain.mapper.RepairFaultTypeMapper;
import com.dormrepair.domain.mapper.RepairWorkScheduleMapper;
import com.dormrepair.fault.dto.FaultTypeCreateRequest;
import com.dormrepair.fault.service.FaultTypeService;
import com.dormrepair.schedule.dto.WorkScheduleCreateRequest;
import com.dormrepair.schedule.service.WorkScheduleService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FoundationConfigurationServiceTest {

    @Test
    void faultTypeConvertsDuplicateCodeAndDefaultsToEnabled() {
        RepairFaultTypeMapper mapper = mock(RepairFaultTypeMapper.class);
        doThrow(new DuplicateKeyException("uk_fault_type_code")).when(mapper).insert(any());
        FaultTypeService service = new FaultTypeService(mapper);

        assertThatThrownBy(() -> service.create(new FaultTypeCreateRequest("ELEC", "水电", 10, null)))
            .isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(ResultCodeEnum.FAULT_CODE_EXISTS.getCode()));
    }

    @Test
    void areaRejectsIllegalParentLevel() {
        RepairAreaMapper mapper = mock(RepairAreaMapper.class);
        RepairArea parent = new RepairArea();
        parent.setId(9L); parent.setAreaType(1); parent.setStatus(1); parent.setDeleted(0);
        when(mapper.selectById(9L)).thenReturn(parent);
        AreaService service = new AreaService(mapper);

        assertThatThrownBy(() -> service.create(new AreaCreateRequest(9L, "B01", "一号楼", 3, 0, null)))
            .isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(ResultCodeEnum.AREA_PARENT_INVALID.getCode()));
    }

    @Test
    void publicChildrenAreEmptyWhenParentIsDisabled() {
        RepairAreaMapper mapper = mock(RepairAreaMapper.class);
        RepairArea parent = new RepairArea();parent.setId(9L);parent.setAreaType(1);parent.setStatus(0);parent.setDeleted(0);
        when(mapper.selectById(9L)).thenReturn(parent);
        AreaService service = new AreaService(mapper);
        assertThat(service.children(9L,true)).isEmpty();
        verify(mapper,never()).selectChildren(anyLong(),anyBoolean());
    }

    @Test
    void roomCanOnlyBeCreatedBelowBuilding() {
        RepairAreaMapper mapper = mock(RepairAreaMapper.class);
        RepairArea building = new RepairArea();
        building.setId(30L); building.setAreaType(3); building.setStatus(1); building.setDeleted(0);
        when(mapper.selectById(30L)).thenReturn(building);
        when(mapper.insert(any())).thenAnswer(invocation -> {
            RepairArea value = invocation.getArgument(0);
            value.setId(40L);
            return 1;
        });
        AreaService service = new AreaService(mapper);

        Long id = service.create(new AreaCreateRequest(30L, "ROOM-502", "502室", 4, 0, null));

        assertThat(id).isEqualTo(40L);
        verify(mapper).insert(argThat(value -> value.getAreaType() == 4 && value.getParentId() == 30L));
    }

    @Test
    void scheduleRejectsInclusiveDateOverlap() {
        RepairWorkScheduleMapper mapper = mock(RepairWorkScheduleMapper.class);
        when(mapper.countEnabledOverlap(any(), any(), isNull())).thenReturn(1);
        WorkScheduleService service = new WorkScheduleService(mapper);

        WorkScheduleCreateRequest request = new WorkScheduleCreateRequest(
            "冬季", LocalDate.of(2026, 9, 30), LocalDate.of(2026, 12, 31),
            LocalTime.of(8, 0), LocalTime.of(18, 0), 1, null);
        assertThatThrownBy(() -> service.create(request, 1L))
            .isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(ResultCodeEnum.SCHEDULE_CONFLICT.getCode()));
    }
}
