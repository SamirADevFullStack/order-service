package com.banque.order.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banque.order.application.port.in.CreateOrderCommand;
import com.banque.order.application.port.in.CreateOrderUseCase;
import com.banque.order.domain.exception.InvalidOrderException;
import com.banque.order.domain.model.Money;
import com.banque.order.domain.model.Order;
import com.banque.order.domain.model.OrderLine;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/** Vérifie le principe de l'Outbox : commande + événement validés ensemble, ou annulés ensemble. */
@ExtendWith(MockitoExtension.class)
class TransactionalCreateOrderUseCaseTest {

    private static final CreateOrderCommand COMMAND = new CreateOrderCommand("C-001", "EUR",
            List.of(new CreateOrderCommand.LineItem("BOOK", 1, BigDecimal.TEN)));

    @Mock
    private CreateOrderUseCase delegate;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionStatus transactionStatus;

    private TransactionalCreateOrderUseCase useCase;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        useCase = new TransactionalCreateOrderUseCase(delegate, new TransactionTemplate(transactionManager));
    }

    @Test
    void valide_la_transaction_quand_le_cas_d_usage_reussit() {
        Order order = Order.create("C-001",
                List.of(new OrderLine("BOOK", 1, Money.of(BigDecimal.TEN, "EUR"))), Instant.now());
        when(delegate.create(COMMAND)).thenReturn(order);

        assertThat(useCase.create(COMMAND)).isEqualTo(order);

        verify(transactionManager).commit(transactionStatus);
        verify(transactionManager, never()).rollback(transactionStatus);
    }

    @Test
    void annule_tout_si_le_cas_d_usage_echoue() {
        when(delegate.create(COMMAND)).thenThrow(new InvalidOrderException("Règle violée"));

        assertThatThrownBy(() -> useCase.create(COMMAND)).isInstanceOf(InvalidOrderException.class);

        verify(transactionManager).rollback(transactionStatus);
        verify(transactionManager, never()).commit(transactionStatus);
    }
}
