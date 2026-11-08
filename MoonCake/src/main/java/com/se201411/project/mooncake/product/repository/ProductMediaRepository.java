package com.se201411.project.mooncake.product.repository;
import java.util.List;
public interface ProductMediaRepository {
 List<Object[]> images(long product);
 List<Object[]> videos(long product);
 List<Object[]> allVideos();
 void add(long product,String url,String caption);
 void remove(long id);
}
