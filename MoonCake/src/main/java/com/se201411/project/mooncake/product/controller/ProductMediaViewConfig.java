package com.se201411.project.mooncake.product.controller;
import com.se201411.project.mooncake.product.service.ProductMediaService;
import com.se201411.project.mooncake.product.dto.CatalogProductView;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.*;
import jakarta.servlet.http.*;
@Configuration
public class ProductMediaViewConfig implements WebMvcConfigurer {
 private final ProductMediaService media;
 public ProductMediaViewConfig(ProductMediaService media){this.media=media;}
 public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new HandlerInterceptor(){
  public void postHandle(HttpServletRequest request,HttpServletResponse response,Object handler,ModelAndView view){
   if(view!=null && "product-detail".equals(view.getViewName()) && view.getModel().get("product") instanceof CatalogProductView p){
    view.addObject("galleryImages",media.images(p.getId()));view.addObject("galleryVideos",media.videos(p.getId()));
   }
  }
 }).addPathPatterns("/products/*");}
}
