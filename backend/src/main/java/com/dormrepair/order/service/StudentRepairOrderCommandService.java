package com.dormrepair.order.service;
import com.dormrepair.order.dto.*;
import org.springframework.stereotype.Service;
@Service public class StudentRepairOrderCommandService {
  private final StudentRepairOrderTransactionService transactions;
  public StudentRepairOrderCommandService(StudentRepairOrderTransactionService transactions){this.transactions=transactions;}
  public void confirm(Long id){transactions.confirm(id);}
  public void evaluate(Long id,CreateRepairEvaluationRequest request){transactions.evaluate(id,request);}
  public void rework(Long id,CreateReworkRequest request){transactions.rework(id,request);}
}
