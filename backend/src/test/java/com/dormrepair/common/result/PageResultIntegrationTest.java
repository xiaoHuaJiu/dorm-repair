package com.dormrepair.common.result;

import com.dormrepair.testsupport.PageHelperTestMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PageResultIntegrationTest {
    @Autowired PageHelperTestMapper mapper;

    @ParameterizedTest
    @CsvSource({"1,10,,25,10", "2,10,,25,10", "3,10,,25,5", "9,10,,25,0", "1,10,A,15,10", "2,10,A,15,5", "0,101,B,10,10"})
    void paginatesAndFilters(Integer pageNum, Integer pageSize, String category, long total, int listSize) {
        PageQuery query = new PageQuery();
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        PageResult<PageHelperTestMapper.ProbeRow> result = PageResult.from(new PageInfo<>(mapper.selectByCategory(category)));
        assertThat(result.getTotal()).isEqualTo(total);
        assertThat(result.getList()).hasSize(listSize);
        assertThat(result.getPageNum()).isEqualTo(query.getPageNum());
        assertThat(result.getPageSize()).isEqualTo(query.getPageSize());
    }
}
