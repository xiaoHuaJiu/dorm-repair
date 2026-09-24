package com.dormrepair.domain.mapper;

import com.dormrepair.domain.entity.RepairArea;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface RepairAreaMapper {
    RepairArea selectById(@Param("id") Long id);
    int insert(RepairArea value);
    int update(RepairArea value);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    int countSiblingName(@Param("parentId") Long parentId, @Param("areaType") Integer areaType,
                         @Param("areaName") String areaName, @Param("excludeId") Long excludeId);
    List<RepairArea> selectAll();
    List<RepairArea> selectEnabled();
    List<RepairArea> selectChildren(@Param("parentId") Long parentId, @Param("enabledOnly") boolean enabledOnly);
}
