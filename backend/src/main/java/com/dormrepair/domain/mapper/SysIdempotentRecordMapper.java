package com.dormrepair.domain.mapper;
import com.dormrepair.domain.entity.SysIdempotentRecord;
import org.apache.ibatis.annotations.Mapper; import org.apache.ibatis.annotations.Param;
@Mapper public interface SysIdempotentRecordMapper {
    SysIdempotentRecord selectById(@Param("id") Long id);
    SysIdempotentRecord selectByBusinessKey(@Param("bizType") String bizType,@Param("userId") Long userId,@Param("bizNo") String bizNo);
    int insert(SysIdempotentRecord record);
    int markSuccess(@Param("id") Long id,@Param("resultSnapshot") String resultSnapshot);
}
