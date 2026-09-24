package com.dormrepair.order.service;

import com.dormrepair.common.enums.RepairOrderStatusEnum;
import com.dormrepair.common.result.PageResult;
import com.dormrepair.domain.mapper.RepairOrderMapper;
import com.dormrepair.order.dto.RepairOrderQueryRequest;
import com.dormrepair.order.model.RepairOrderQueryCondition;
import com.dormrepair.order.vo.RepairOrderListItem;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RepairOrderQueryService {
    private final RepairOrderMapper mapper; private final RepairOrderQueryScopeFactory scopes;
    public RepairOrderQueryService(RepairOrderMapper mapper,RepairOrderQueryScopeFactory scopes){this.mapper=mapper;this.scopes=scopes;}
    public PageResult<RepairOrderListItem> page(RepairOrderQueryRequest request){
        RepairOrderQueryCondition condition=scopes.create(request);
        PageHelper.startPage(request.getPageNum(),request.getPageSize());
        List<RepairOrderListItem> values=mapper.selectPage(condition);
        PageInfo<RepairOrderListItem> info=new PageInfo<>(values);
        LocalDateTime now=condition.getNow();
        values.forEach(v->{
            v.setStatusName(RepairOrderStatusEnum.fromCode(v.getStatus()).map(RepairOrderStatusEnum::getDescription).orElse("未知状态"));
            v.setAcceptTimeout(v.getStatus()!=null&&v.getStatus()==1&&v.getAcceptDeadline()!=null&&v.getAcceptDeadline().isBefore(now));
            v.setCompleteTimeout(v.getStatus()!=null&&(v.getStatus()==2||v.getStatus()==4)&&v.getCompleteDeadline()!=null&&v.getCompleteDeadline().isBefore(now));
        });
        return PageResult.of(info.getTotal(),info.getPageNum(),info.getPageSize(),values);
    }
}
