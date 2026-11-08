package com.se201411.project.mooncake.order.repository.impl;
import com.se201411.project.mooncake.order.repository.OperationsRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public class OperationsRepositoryImpl implements OperationsRepository {
 private final EntityManager em;
 public OperationsRepositoryImpl(EntityManager em){this.em=em;}
 @SuppressWarnings("unchecked") public List<Object[]> businesses(){return em.createNativeQuery("SELECT b.business_id,b.business_name,b.contact_name,b.contact_email,b.contact_phone,COUNT(o.order_id),COALESCE(SUM(o.total_amount),0) FROM dbo.BUSINESS b LEFT JOIN dbo.SALES_ORDER o ON o.business_id=b.business_id GROUP BY b.business_id,b.business_name,b.contact_name,b.contact_email,b.contact_phone ORDER BY b.business_id DESC").getResultList();}
 public Object[] statistics(){return (Object[])em.createNativeQuery("SELECT COUNT(*),COALESCE(SUM(CASE WHEN order_status='PLACED' THEN 1 ELSE 0 END),0),COALESCE(SUM(CASE WHEN order_status='COMPLETED' THEN total_amount ELSE 0 END),0),COALESCE(SUM(CASE WHEN order_status='CANCELLED' THEN 1 ELSE 0 END),0) FROM dbo.SALES_ORDER").getSingleResult();}
}
