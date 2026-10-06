package com.banque.order.infrastructure.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banque.order.application.port.in.CreateOrderUseCase;
import com.banque.order.domain.exception.InvalidOrderException;
import com.banque.order.domain.model.Money;
import com.banque.order.domain.model.Order;
import com.banque.order.domain.model.OrderLine;
import com.banque.order.infrastructure.in.rest.mapper.OrderRestMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Test de l'adaptateur REST : on vérifie que le contrôleur respecte le contrat (statuts, JSON, validation).
 * La route, @RequestBody et @Valid viennent de l'interface OrdersApi générée : ce test le vérifie aussi.
 * Le cas d'usage est mocké : on ne teste QUE l'adaptateur, sans démarrer Spring Boot.
 */
class OrderControllerTest {

    private static final String VALID_BODY = """
            {"customerId":"C-001","currency":"EUR","lines":[{"productCode":"BOOK","quantity":2,"unitPrice":12.50}]}
            """;

    private final CreateOrderUseCase createOrderUseCase = mock(CreateOrderUseCase.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new OrderController(createOrderUseCase, new OrderRestMapper()))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void cree_une_commande_et_renvoie_201() throws Exception {
        Order order = Order.create("C-001",
                List.of(new OrderLine("BOOK", 2, Money.of(new BigDecimal("12.50"), "EUR"))),
                Instant.parse("2026-10-05T09:00:00Z"));
        when(createOrderUseCase.create(any())).thenReturn(order);

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/orders/" + order.id().value()))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmount").value(25.0));
    }

    @Test
    void renvoie_400_si_le_json_est_invalide() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"","currency":"EUR","lines":[]}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createOrderUseCase);
    }

    @Test
    void traduit_une_regle_metier_violee_en_400() throws Exception {
        when(createOrderUseCase.create(any())).thenThrow(new InvalidOrderException("Règle violée"));

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Commande invalide"));
    }
}
