package com.dormrepair.order;
import com.dormrepair.order.dto.CreateRepairOrderRequest;
import com.dormrepair.order.service.RepairOrderRequestHasher;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
class RepairOrderRequestHasherTest {
    @Test void confirmationDoesNotChangeHashButBusinessContentDoes(){
        var r=request(); r.setConfirmDuplicate(false); var hasher=new RepairOrderRequestHasher();
        String first=hasher.hash(9L,r); r.setConfirmDuplicate(true);
        assertThat(hasher.hash(9L,r)).isEqualTo(first);
        r.setRoomId(99L); assertThat(hasher.hash(9L,r)).isNotEqualTo(first);
    }
    private CreateRepairOrderRequest request(){var r=new CreateRepairOrderRequest();r.setBizNo("b");r.setCampusId(1L);r.setAreaId(2L);r.setBuildingId(3L);r.setRoomId(4L);r.setFaultTypeId(5L);r.setProblemDescription("灯坏了");r.setContactName("张三");r.setContactPhone("13800000000");r.setImageUrls(List.of("a","b"));return r;}
}
