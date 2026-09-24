package com.dormrepair.order.dto;

import java.time.LocalDateTime;

public record AcceptRepairOrderRequest(LocalDateTime expectedCompleteTime) {}
