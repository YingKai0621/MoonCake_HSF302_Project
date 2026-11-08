package com.se201411.project.mooncake.order.repository.impl;
import com.se201411.project.mooncake.order.repository.VnpayRepository;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Repository
public class VnpayRepositoryImpl implements VnpayRepository {
 private final EntityManager em;
 public VnpayRepositoryImpl(EntityManager em){this.em=em;}
 public SalesOrder lockOwned(String code,long customer){
  return em.createQuery("from SalesOrder o where o.orderCode=:code and o.customer.id=:customer",SalesOrder.class)
   .setParameter("code",code).setParameter("customer",customer).setLockMode(LockModeType.PESSIMISTIC_WRITE)
   .getResultStream().findFirst().orElseThrow(()->new ResourceNotFoundException("Không tìm thấy đơn của bạn"));
 }
 public SalesOrder lock(long id){return em.find(SalesOrder.class,id,LockModeType.PESSIMISTIC_WRITE);}
 public Object[] attempt(String ref){
  return (Object[])em.createNativeQuery("SELECT order_id,amount,status FROM dbo.VNPAY_ATTEMPT WHERE txn_ref=:ref")
   .setParameter("ref",ref).getResultStream().findFirst().orElse(null);
 }
 public boolean pending(long order){
  return ((Number)em.createNativeQuery("SELECT COUNT(*) FROM dbo.VNPAY_ATTEMPT WHERE order_id=:id AND status='PENDING'")
   .setParameter("id",order).getSingleResult()).intValue()>0;
 }
 public Pending pendingAttempt(long order){
  Object[] row=(Object[])em.createNativeQuery("SELECT TOP(1) txn_ref,amount,created_at FROM dbo.VNPAY_ATTEMPT WHERE order_id=:id AND status='PENDING' ORDER BY created_at DESC")
    .setParameter("id",order).getResultStream().findFirst().orElse(null);
  if(row==null)return null;
  LocalDateTime created=row[2] instanceof java.sql.Timestamp t?t.toLocalDateTime():(LocalDateTime)row[2];
  return new Pending((String)row[0],(BigDecimal)row[1],created);
 }
 public void create(String ref,long order,BigDecimal amount,LocalDateTime now){
  em.createNativeQuery("INSERT dbo.VNPAY_ATTEMPT(txn_ref,order_id,amount,created_at,expires_at) VALUES(:ref,:id,:amount,:now,:expires)")
   .setParameter("ref",ref).setParameter("id",order).setParameter("amount",amount).setParameter("now",now).setParameter("expires",now.plusMinutes(15)).executeUpdate();
 }
 public void finish(String ref,String status,String transaction){
  em.createNativeQuery("UPDATE dbo.VNPAY_ATTEMPT SET status=:status,transaction_no=:txn WHERE txn_ref=:ref AND status='PENDING'")
   .setParameter("status",status).setParameter("txn",transaction).setParameter("ref",ref).executeUpdate();
 }
}
