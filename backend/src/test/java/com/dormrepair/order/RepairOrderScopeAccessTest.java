package com.dormrepair.order;

import com.dormrepair.common.enums.UserRoleEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.domain.entity.RepairOrder;
import com.dormrepair.domain.entity.RepairWorker;
import com.dormrepair.domain.mapper.RepairWorkerMapper;
import com.dormrepair.order.dto.RepairOrderQueryRequest;
import com.dormrepair.order.service.CurrentWorkerResolver;
import com.dormrepair.order.service.RepairOrderAccessService;
import com.dormrepair.order.service.RepairOrderQueryScopeFactory;
import com.dormrepair.security.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RepairOrderScopeAccessTest {
    @AfterEach void clear(){SecurityContextHolder.clearContext();}

    @Test void studentScopeAlwaysUsesCurrentUser(){
        authenticate(11L, UserRoleEnum.STUDENT);
        var condition=new RepairOrderQueryScopeFactory(new CurrentWorkerResolver(mock(RepairWorkerMapper.class)))
            .create(new RepairOrderQueryRequest());
        assertThat(condition.getStudentUid()).isEqualTo(11L);
        assertThat(condition.getCurrentAssigneeId()).isNull();
    }

    @Test void workerScopeMapsUserIdToWorkerId(){
        RepairWorkerMapper mapper=mock(RepairWorkerMapper.class); RepairWorker worker=new RepairWorker();
        worker.setId(88L); worker.setUserId(22L); worker.setWorkStatus(1); worker.setDeleted(0);
        when(mapper.selectByUserId(22L)).thenReturn(worker); authenticate(22L,UserRoleEnum.WORKER);
        var condition=new RepairOrderQueryScopeFactory(new CurrentWorkerResolver(mapper)).create(new RepairOrderQueryRequest());
        assertThat(condition.getCurrentAssigneeId()).isEqualTo(88L);
    }

    @Test void detailsRejectOtherOwnersAndAllowAdmin(){
        RepairWorkerMapper mapper=mock(RepairWorkerMapper.class); RepairOrder order=new RepairOrder();
        order.setStudentUid(11L); order.setCurrentAssigneeId(88L);
        RepairOrderAccessService access=new RepairOrderAccessService(new CurrentWorkerResolver(mapper));
        authenticate(12L,UserRoleEnum.STUDENT);
        assertThatThrownBy(()->access.checkViewPermission(order)).isInstanceOf(BusinessException.class);
        authenticate(99L,UserRoleEnum.ADMIN);
        assertThatCode(()->access.checkViewPermission(order)).doesNotThrowAnyException();
    }

    private void authenticate(Long id,UserRoleEnum role){
        LoginUser user=new LoginUser(id,"u","测试",role);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user,null,List.of()));
    }
}
