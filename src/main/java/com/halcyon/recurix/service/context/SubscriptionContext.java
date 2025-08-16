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
    private Integer pageNumber;

    public SubscriptionContext(Subscription subscription) {
        this.subscription = subscription;
    }

    public SubscriptionContext(Subscription subscription, Integer messageToEditId) {
        this.subscription = subscription;
        this.messageToEditId = messageToEditId;
    }
}
