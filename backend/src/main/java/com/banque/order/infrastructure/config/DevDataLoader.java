package com.banque.order.infrastructure.config;

import com.banque.order.application.port.in.CreateOrderCommand;
import com.banque.order.application.port.in.CreateOrderCommand.LineItem;
import com.banque.order.application.port.in.CreateOrderUseCase;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Données de démonstration, UNIQUEMENT avec le profil « dev » :
 * mvn spring-boot:run -Dspring-boot.run.profiles=dev
 * Les commandes passent par le cas d'usage : les règles métier s'appliquent aussi aux données de test.
 */
@Component
@Profile("dev")
public class DevDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataLoader.class);
    private static final int ORDER_COUNT = 25;

    private final CreateOrderUseCase createOrderUseCase;

    public DevDataLoader(CreateOrderUseCase createOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (int i = 1; i <= ORDER_COUNT; i++) {
            createOrderUseCase.create(new CreateOrderCommand(
                    "C-%03d".formatted(i % 5 + 1),
                    "EUR",
                    List.of(new LineItem("BOOK", i % 3 + 1, new BigDecimal("12.50")),
                            new LineItem("PEN", i, new BigDecimal("1.20")))));
        }
        log.info("Profil dev : {} commandes de démonstration créées", ORDER_COUNT);
    }
}
