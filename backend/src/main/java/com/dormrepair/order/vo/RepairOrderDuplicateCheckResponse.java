package com.dormrepair.order.vo;
import java.util.List;
public record RepairOrderDuplicateCheckResponse(boolean duplicate, List<SuspectedRepairOrder> suspectedOrders) {}
