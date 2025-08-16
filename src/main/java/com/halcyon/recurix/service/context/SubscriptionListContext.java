package com.halcyon.recurix.service.context;

import org.springframework.data.domain.Sort;

/**
 * Хранит состояние отображения списка подписок для пользователя.
 * 
 * @param sortField     Поле для сортировки ("paymentDate" или "price").
 * @param sortDirection Направление сортировки (ASC или DESC).
 */
public record SubscriptionListContext(String sortField, Sort.Direction sortDirection) {}
