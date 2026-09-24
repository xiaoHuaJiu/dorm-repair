package com.dormrepair.common.result;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PageQueryTest {
    @Test void normalizesPageBoundaries() {
        PageQuery query = new PageQuery();
        assertThat(query.getPageNum()).isEqualTo(1);
        assertThat(query.getPageSize()).isEqualTo(10);
        query.setPageNum(0);
        query.setPageSize(101);
        assertThat(query.getPageNum()).isEqualTo(1);
        assertThat(query.getPageSize()).isEqualTo(100);
        query.setPageNum(null);
        query.setPageSize(-1);
        assertThat(query.getPageNum()).isEqualTo(1);
        assertThat(query.getPageSize()).isEqualTo(10);
    }
}
