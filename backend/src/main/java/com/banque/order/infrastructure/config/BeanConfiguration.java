package com.banque.order.infrastructure.config;

import com.banque.order.application.port.in.CreateOrderUseCase;
import com.banque.order.application.port.in.GetOrderUseCase;
import com.banque.order.application.port.in.ListOrdersUseCase;
import com.banque.order.application.port.out.EventPublisher;
import com.banque.order.application.port.out.OrderRepository;
import com.banque.order.application.service.CreateOrderService;
import java.time.Clock;

import com.banque.order.application.service.GetOrderService;
import com.banque.order.application.service.ListOrdersService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * CÂBLAGE : c'est ici, et seulement ici, que Spring rencontre le cœur.
 * Le service du cas d'usage reçoit les adaptateurs qui implémentent ses ports :
 * OrderJpaAdapter pour OrderRepository, et OutboxEventPublisher (ou KafkaEventPublisher) pour EventPublisher.
 * Il est ensuite enveloppé dans un décorateur transactionnel.
 */
@Configuration
@EnableScheduling   // active le @Scheduled du relais Outbox
public class BeanConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public CreateOrderUseCase createOrderUseCase(OrderRepository orderRepository,
                                                 EventPublisher eventPublisher,
                                                 Clock clock,
                                                 TransactionTemplate transactionTemplate) {
        CreateOrderService service = new CreateOrderService(orderRepository, eventPublisher, clock);
        return new TransactionalCreateOrderUseCase(service, transactionTemplate);
    }

    @Bean
    public ListOrdersUseCase listOrdersUseCase(OrderRepository orderRepository) {
        return new ListOrdersService(orderRepository);
    }

    @Bean
    public GetOrderUseCase getOrderUseCase(OrderRepository orderRepository){
        return new GetOrderService(orderRepository);
    }
}
