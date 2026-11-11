package com.se201411.project.mooncake.order.service.impl;

import java.util.ArrayList;
import java.math.BigDecimal;
import com.se201411.project.mooncake.enums.PaymentMethod;
import com.se201411.project.mooncake.order.service.PaymentProcessor;
import com.se201411.project.mooncake.order.service.PaymentProcessorRegistry;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentProcessorRegistryImpl implements PaymentProcessorRegistry {
    private final Map<PaymentMethod, PaymentProcessor> processors =
        new EnumMap<>(PaymentMethod.class);

    public PaymentProcessorRegistryImpl(List<PaymentProcessor> processorBeans) {
        for (PaymentProcessor processor : processorBeans) {
            PaymentProcessor previous = processors.put(processor.method(), processor);
            if (previous != null) {
                throw new IllegalStateException(
                    "Two payment processors handle " + processor.method()
                );
            }
        }
    }

    @Override
    public PaymentProcessor forMethod(PaymentMethod method) {
        PaymentProcessor processor = processors.get(method);
        if (processor == null) {
            throw new IllegalStateException("No payment processor for " + method);
        }
        return processor;
    }

    @Override
    public List<PaymentMethod> methodsAccepting(BigDecimal total) {
        List<PaymentMethod> methods = new ArrayList<>();
        for (PaymentProcessor processor : processors.values()) {
            if (processor.isAvailable() && processor.acceptsTotal(total)) {
                methods.add(processor.method());
            }
        }
        return methods;
    }

    @Override
    public PaymentMethod defaultMethodFor(BigDecimal total) {
        List<PaymentMethod> methods = methodsAccepting(total);
        if (methods.isEmpty()) {
            return processors.keySet().iterator().next();
        }
        return methods.get(0);
    }
}
