package com.halcyon.recurix.service;

import com.halcyon.recurix.model.Subscription;
import com.halcyon.recurix.repository.SubscriptionRepository;
import com.halcyon.recurix.service.pagination.Page;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public Mono<Subscription> save(Subscription subscription) {
        return subscriptionRepository.save(subscription);
    }

    public Mono<Subscription> findById(Long subscriptionId) {
        return subscriptionRepository.findById(subscriptionId);
    }

    public Flux<Subscription> getAllByUserId(Long userId) {
        return subscriptionRepository.findAllByUserId(userId);
    }

    public Mono<Integer> countByUserId(Long userId) {
        return subscriptionRepository.countByUserId(userId);
    }

    /**
     * Централизованно получает страницу подписок для пользователя.
     * 
     * @param userId   ID пользователя.
     * @param pageable Объект с информацией о странице, размере и сортировке.
     * @return Mono с объектом Page.
     */
    public Mono<Page<Subscription>> getSubscriptionsAsPage(Long userId, Pageable pageable) {
        Mono<Integer> totalElementsMono = this.countByUserId(userId);
        Mono<List<Subscription>> contentMono = getAllByUserId(userId, pageable).collectList();

        return Mono.zip(contentMono, totalElementsMono, (content, total) -> {
            int pageSize = pageable.getPageSize();
            int totalPages = (total == 0)
                    ? 1
                    : (int) Math.ceil((double) total / pageSize);
            return new Page<>(content, pageable.getPageNumber(), totalPages, total);
        });
    }

    private Flux<Subscription> getAllByUserId(Long userId, Pageable pageable) {
        return subscriptionRepository.findAllByUserId(userId, pageable);
    }

    public Mono<Void> deleteById(long subscriptionId) {
        return subscriptionRepository.deleteById(subscriptionId);
    }
}
