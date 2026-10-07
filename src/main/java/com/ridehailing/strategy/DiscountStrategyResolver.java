package com.ridehailing.strategy;

import com.ridehailing.model.DiscountType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class DiscountStrategyResolver {

    private final Map<DiscountType, DiscountStrategy> strategyMap = new EnumMap<>(DiscountType.class);

    public DiscountStrategyResolver(List<DiscountStrategy> strategies) {
        for (DiscountStrategy strategy : strategies) {
            strategyMap.put(strategy.getSupportedDiscountType(), strategy);
        }
    }

    public Optional<DiscountStrategy> resolve(DiscountType discountType) {
        return Optional.ofNullable(strategyMap.get(discountType));
    }
}
