package com.halcyon.recurix.service.context;

import com.halcyon.recurix.model.Subscription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionContext {

    private Subscription subscription;
    private Integer messageToEditId;

    public SubscriptionContext(Subscription subscription) {
        this.subscription = subscription;
    }
}
