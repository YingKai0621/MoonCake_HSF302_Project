package com.se201411.project.mooncake.inventory.service;
import com.se201411.project.mooncake.inventory.repository.BatchRepository;
import com.se201411.project.mooncake.enums.StockMovementType;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
@Service
public class BatchStockService {
 private final BatchRepository repo;private final AdminAccessService access;
 public BatchStockService(BatchRepository repo,AdminAccessService access){this.repo=repo;this.access=access;}
 @Transactional public void apply(long variant,int delta,String key,Long order,StockMovementType type){
  int before=Math.subtractExact(repo.physical(variant),delta);
  int tracked=repo.tracked(variant);
  if(before>tracked)repo.create(variant,"LEGACY-"+UUID.randomUUID().toString().substring(0,12),before-tracked);
  if(before<tracked)throw new BusinessRuleViolationException("Tồn lô không khớp tồn tổng; cần kiểm kê SKU #"+variant);
  if(delta>0){
   var original=type==StockMovementType.CANCEL_RETURN && order!=null?repo.sold(variant,order):List.<Object[]>of();
   if(!original.isEmpty()){
    int restored=0;
    for(var row:original){long id=((Number)row[0]).longValue();int quantity=((Number)row[1]).intValue();restored=Math.addExact(restored,quantity);repo.change(id,quantity);repo.record(id,order,key,quantity);}
    if(restored!=delta)throw new BusinessRuleViolationException("Lượng hoàn lô không khớp đơn.");
   }else{long id=repo.create(variant,"LOT-"+UUID.randomUUID().toString().substring(0,12),delta);repo.record(id,order,key,delta);}
  }else{
   int need=Math.negateExact(delta);
   var rows=repo.available(variant,type==StockMovementType.ADJUST);
   long valid=rows.stream().mapToLong(v->((Number)v[1]).longValue()).sum();
   if(valid<need)throw new BusinessRuleViolationException("SKU #"+variant+" chỉ còn "+valid+" sản phẩm thuộc lô có thể bán. Vui lòng đặt lại.");
   for(var row:rows){if(need==0)break;long id=((Number)row[0]).longValue();int take=Math.min(need,((Number)row[1]).intValue());repo.change(id,-take);repo.record(id,order,key,-take);need-=take;}
  }
 }
 @Transactional public void finishPack(String group,long variant){repo.finishPack(group,variant);}
 @Transactional(readOnly=true) public List<Object[]> list(){access.requireAdmin();return repo.list();}
 @Transactional public void dates(long id,String code,LocalDate production,LocalDate expiry){access.requireAdmin();
  if(code==null || code.isBlank() || code.length()>80 || (production!=null && expiry!=null && expiry.isBefore(production)))throw new BusinessRuleViolationException("Mã lô tối đa 80 ký tự; hạn dùng phải sau ngày sản xuất.");
  repo.dates(id,code.trim(),production,expiry);
 }
}
