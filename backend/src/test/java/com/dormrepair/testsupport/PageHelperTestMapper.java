package com.dormrepair.testsupport;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface PageHelperTestMapper {
    record ProbeRow(Long id, String category, String displayName) {}

    @Select("<script>SELECT id, category, display_name FROM page_helper_probe"
        + "<if test='category != null'> WHERE category = #{category}</if> ORDER BY id</script>")
    List<ProbeRow> selectByCategory(@Param("category") String category);
}
