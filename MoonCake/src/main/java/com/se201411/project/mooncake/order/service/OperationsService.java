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
 @Transactional(readOnly=true) public java.util.List<Object[]> businesses(){admins.requireAdmin();return repo.businesses();}
 @Transactional(readOnly=true) public Object[] statistics(){admins.requireAdmin();return repo.statistics();}
}
