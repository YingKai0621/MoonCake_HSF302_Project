package com.se201411.project.mooncake.product.repository.impl;
import com.se201411.project.mooncake.product.repository.ProductMediaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
@SuppressWarnings("unchecked")
public class ProductMediaRepositoryImpl implements ProductMediaRepository {
 private final EntityManager em;
 public ProductMediaRepositoryImpl(EntityManager em){this.em=em;}
 public List<Object[]> images(long id){return em.createNativeQuery("SELECT i.image_url,i.alt_text FROM dbo.PRODUCT_IMAGE i JOIN dbo.PRODUCT_VARIANT v ON v.variant_id=i.variant_id WHERE v.product_id=:id AND v.is_active=1 ORDER BY i.is_primary DESC,i.sort_order,i.image_id").setParameter("id",id).getResultList();}
 public List<Object[]> videos(long id){return em.createNativeQuery("SELECT video_id,video_url,caption FROM dbo.PRODUCT_VIDEO WHERE product_id=:id ORDER BY sort_order,video_id").setParameter("id",id).getResultList();}
 public List<Object[]> allVideos(){return em.createNativeQuery("SELECT v.video_id,p.product_name,v.video_url,v.caption FROM dbo.PRODUCT_VIDEO v JOIN dbo.PRODUCT p ON p.product_id=v.product_id ORDER BY v.video_id DESC").getResultList();}
 public void add(long product,String url,String caption){em.createNativeQuery("INSERT dbo.PRODUCT_VIDEO(product_id,video_url,caption) VALUES(:id,:url,:caption)").setParameter("id",product).setParameter("url",url).setParameter("caption",caption).executeUpdate();}
 public void remove(long id){em.createNativeQuery("DELETE dbo.PRODUCT_VIDEO WHERE video_id=:id").setParameter("id",id).executeUpdate();}
}
