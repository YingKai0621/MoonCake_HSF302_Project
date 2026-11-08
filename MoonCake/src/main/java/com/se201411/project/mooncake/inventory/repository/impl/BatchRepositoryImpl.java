package com.se201411.project.mooncake.inventory.repository.impl;
import com.se201411.project.mooncake.inventory.repository.BatchRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
@Repository
@SuppressWarnings("unchecked")
public class BatchRepositoryImpl implements BatchRepository {
 private final EntityManager em;
 public BatchRepositoryImpl(EntityManager em){this.em=em;}
 public int physical(long v){return ((Number)em.createNativeQuery("SELECT available_quantity FROM dbo.INVENTORY_BALANCE WITH(UPDLOCK,HOLDLOCK) WHERE variant_id=:v").setParameter("v",v).getSingleResult()).intValue();}
 public int tracked(long v){return ((Number)em.createNativeQuery("SELECT COALESCE(SUM(remaining_quantity),0) FROM dbo.STOCK_BATCH WHERE variant_id=:v").setParameter("v",v).getSingleResult()).intValue();}
 public long create(long v,String code,int q){return ((Number)em.createNativeQuery("INSERT dbo.STOCK_BATCH(variant_id,batch_code,remaining_quantity) OUTPUT INSERTED.batch_id VALUES(:v,:code,:q)").setParameter("v",v).setParameter("code",code).setParameter("q",q).getSingleResult()).longValue();}
 public List<Object[]> available(long v,boolean expiredAllowed){return em.createNativeQuery("SELECT batch_id,remaining_quantity FROM dbo.STOCK_BATCH WITH(UPDLOCK,HOLDLOCK) WHERE variant_id=:v AND remaining_quantity>0 "+(expiredAllowed?"":"AND (expires_on IS NULL OR expires_on>=CAST(SYSUTCDATETIME() AS date)) ")+"ORDER BY CASE WHEN expires_on IS NULL THEN 1 ELSE 0 END,expires_on,batch_id").setParameter("v",v).getResultList();}
 public List<Object[]> sold(long v,long order){return em.createNativeQuery("SELECT a.batch_id,-SUM(a.quantity_delta) FROM dbo.BATCH_ALLOCATION a JOIN dbo.STOCK_BATCH b ON b.batch_id=a.batch_id WHERE a.order_id=:o AND b.variant_id=:v AND a.quantity_delta<0 GROUP BY a.batch_id").setParameter("v",v).setParameter("o",order).getResultList();}
 public void change(long id,int delta){int changed=em.createNativeQuery("UPDATE dbo.STOCK_BATCH SET remaining_quantity=remaining_quantity+:d WHERE batch_id=:id AND CAST(remaining_quantity AS bigint)+:d BETWEEN 0 AND 2147483647").setParameter("id",id).setParameter("d",delta).executeUpdate();if(changed!=1)throw new IllegalStateException("Batch quantity changed concurrently");}
 public void record(long id,Long order,String key,int d){em.createNativeQuery("INSERT dbo.BATCH_ALLOCATION(batch_id,order_id,operation_key,quantity_delta) VALUES(:id,:o,:key,:d)").setParameter("id",id).setParameter("o",order).setParameter("key",key).setParameter("d",d).executeUpdate();}
 public List<Object[]> list(){return em.createNativeQuery("SELECT b.batch_id,v.sku,b.batch_code,b.produced_on,b.expires_on,b.remaining_quantity,CASE WHEN b.expires_on<CAST(SYSUTCDATETIME() AS date) THEN N'Hết hạn' WHEN b.expires_on IS NULL THEN N'Chưa có hạn dùng' ELSE N'Còn hạn' END FROM dbo.STOCK_BATCH b JOIN dbo.PRODUCT_VARIANT v ON v.variant_id=b.variant_id ORDER BY b.batch_id DESC").getResultList();}
 public void finishPack(String group,long variant){
  // Packaging has no food expiry. Inherit the earliest known expiry of consumed cakes.
  em.createNativeQuery("""
   UPDATE b SET expires_on=(
    SELECT MIN(source.expires_on) FROM dbo.BATCH_ALLOCATION a
    JOIN dbo.STOCK_BATCH source ON source.batch_id=a.batch_id
    JOIN dbo.PRODUCT_VARIANT v ON v.variant_id=source.variant_id
    JOIN dbo.PRODUCT p ON p.product_id=v.product_id
    WHERE LEFT(a.operation_key,LEN(:prefix))=:prefix AND a.quantity_delta<0 AND p.product_type='MOONCAKE')
   FROM dbo.STOCK_BATCH b JOIN dbo.BATCH_ALLOCATION output ON output.batch_id=b.batch_id
   WHERE output.operation_key=:key AND output.quantity_delta>0
   """).setParameter("prefix",group+":").setParameter("key",group+":"+variant+":IN").executeUpdate();
 }
 public void dates(long id,String code,LocalDate production,LocalDate expiry){int count=em.createNativeQuery("UPDATE dbo.STOCK_BATCH SET batch_code=:code,produced_on=:production,expires_on=:expiry WHERE batch_id=:id AND NOT EXISTS(SELECT 1 FROM dbo.BATCH_ALLOCATION a WHERE a.batch_id=:id AND a.quantity_delta<0)").setParameter("id",id).setParameter("code",code).setParameter("production",production).setParameter("expiry",expiry).executeUpdate();if(count!=1)throw new com.se201411.project.mooncake.common.exception.BusinessRuleViolationException("Lô đã xuất hàng không thể đổi thông tin truy xuất.");}
}
