package com.se201411.project.mooncake.order.repository;
import java.util.List;
public interface OperationsRepository {
 void feedback(long customer,String content);
 List<Object[]> feedbacks();
 List<Object[]> businesses();
 Object[] statistics();
}
