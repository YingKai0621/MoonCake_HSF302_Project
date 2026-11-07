package com.se201411.project.mooncake.product.service;
import com.se201411.project.mooncake.product.repository.ProductMediaRepository;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
@Transactional(readOnly=true)
public class ProductMediaService {
 private final ProductMediaRepository repo;private final AdminAccessService access;
 public ProductMediaService(ProductMediaRepository repo,AdminAccessService access){this.repo=repo;this.access=access;}
 public List<Object[]> images(long id){return repo.images(id);}
 public List<Object[]> videos(long id){return repo.videos(id);}
 public List<Object[]> all(){access.requireAdmin();return repo.allVideos();}
 @Transactional public void add(long id,String url,String caption){access.requireAdmin();
  if(url==null || url.length()>1000 || !url.matches("(?:https://[^\\s]+|/images/[^\\s]+)\\.(?:mp4|webm)(?:\\?[^\\s]*)?") || caption==null || caption.isBlank() || caption.length()>250)
   throw new BusinessRuleViolationException("Nhập link HTTPS hoặc /images/ tới video MP4/WebM và mô tả tối đa 250 ký tự.");
  repo.add(id,url,caption.trim());
 }
 @Transactional public void remove(long id){access.requireAdmin();repo.remove(id);}
}
