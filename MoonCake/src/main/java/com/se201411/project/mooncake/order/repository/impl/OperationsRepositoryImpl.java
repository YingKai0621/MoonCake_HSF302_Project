package com.se201411.project.mooncake.order.repository.impl;
import com.se201411.project.mooncake.order.repository.OperationsRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public class OperationsRepositoryImpl implements OperationsRepository {
 private final EntityManager em;
 public OperationsRepositoryImpl(EntityManager em){this.em=em;}
 public void feedback(long customer,String content){em.createNativeQuery("INSERT dbo.FEEDBACK(customer_id,content) VALUES(:customer,:content)").setParameter("customer",customer).setParameter("content",content).executeUpdate();}
 @SuppressWarnings("unchecked") public List<Object[]> feedbacks(){return em.createNativeQuery("SELECT TOP(100) f.feedback_id,c.full_name,f.content,f.created_at FROM dbo.FEEDBACK f JOIN dbo.CUSTOMER c ON c.customer_id=f.customer_id ORDER BY f.feedback_id DESC").getResultList();}
 @SuppressWarnings("unchecked") public List<Object[]> businesses(){return em.createNativeQuery("SELECT b.business_id,b.business_name,b.contact_name,b.contact_email,b.contact_phone,COUNT(o.order_id),COALESCE(SUM(o.total_amount),0) FROM dbo.BUSINESS b LEFT JOIN dbo.SALES_ORDER o ON o.business_id=b.business_id GROUP BY b.business_id,b.business_name,b.contact_name,b.contact_email,b.contact_phone ORDER BY b.business_id DESC").getResultList();}
 public Object[] statistics(){return (Object[])em.createNativeQuery("SELECT COUNT(*),COALESCE(SUM(CASE WHEN order_status='PLACED' THEN 1 ELSE 0 END),0),COALESCE(SUM(CASE WHEN order_status='COMPLETED' THEN total_amount ELSE 0 END),0),COALESCE(SUM(CASE WHEN order_status='CANCELLED' THEN 1 ELSE 0 END),0) FROM dbo.SALES_ORDER").getSingleResult();}
}
