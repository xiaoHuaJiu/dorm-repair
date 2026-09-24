package com.dormrepair.domain.mapper;
import com.dormrepair.domain.entity.RepairOrderAlert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper public interface RepairOrderAlertMapper {
    RepairOrderAlert selectById(@Param("id") Long id);
    int insertIgnore(RepairOrderAlert alert);
}
