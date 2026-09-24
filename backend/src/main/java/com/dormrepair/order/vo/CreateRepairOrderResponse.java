package com.dormrepair.order.vo;
import java.util.List;
public record CreateRepairOrderResponse(boolean created, boolean duplicate, Long orderId, String orderNo, List<SuspectedRepairOrder> suspectedOrders) {}
