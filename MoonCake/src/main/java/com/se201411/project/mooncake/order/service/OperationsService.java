package com.se201411.project.mooncake.order.service;
import com.se201411.project.mooncake.order.repository.*;
import com.se201411.project.mooncake.security.service.*;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class OperationsService {
 private final OperationsRepository repo;
 private final CurrentCustomerService customers; private final AdminAccessService admins;
 public OperationsService(OperationsRepository repo,CurrentCustomerService customers,AdminAccessService admins){this.repo=repo;this.customers=customers;this.admins=admins;}
 @Transactional public void feedback(String content){if(content==null || content.isBlank() || content.length()>2000)throw new BusinessRuleViolationException("Góp ý cần từ 1 đến 2.000 ký tự.");repo.feedback(customers.requireCurrentCustomer().customerId(),content.trim());}
 @Transactional(readOnly=true) public java.util.List<Object[]> feedbacks(){admins.requireAdmin();return repo.feedbacks();}
 @Transactional(readOnly=true) public java.util.List<Object[]> businesses(){admins.requireAdmin();return repo.businesses();}
 @Transactional(readOnly=true) public Object[] statistics(){admins.requireAdmin();return repo.statistics();}
}
