package com.se201411.project.mooncake.product.controller;

import com.se201411.project.mooncake.product.dto.CatalogProductView;
import com.se201411.project.mooncake.product.service.ProductMediaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ProductMediaViewConfig implements WebMvcConfigurer {

    private final ProductMediaService productMediaService;

    public ProductMediaViewConfig(ProductMediaService productMediaService) {
        this.productMediaService = productMediaService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new GalleryInterceptor()).addPathPatterns("/products/*");
    }

    private class GalleryInterceptor implements HandlerInterceptor {

        @Override
        public void postHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            ModelAndView view
        ) {
            if (view == null || !"product-detail".equals(view.getViewName())) {
                return;
            }
            if (!(view.getModel().get("product") instanceof CatalogProductView product)) {
                return;
            }
            view.addObject("galleryImages", productMediaService.images(product.getId()));
            view.addObject("galleryVideos", productMediaService.videos(product.getId()));
        }
    }
}
