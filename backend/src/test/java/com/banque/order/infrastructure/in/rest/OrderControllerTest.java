package com.banque.order.infrastructure.in.rest;

import com.banque.order.application.pagination.PageQuery;
import com.banque.order.application.pagination.PageResult;
import com.banque.order.application.port.in.CreateOrderUseCase;
import com.banque.order.application.port.in.GetOrderUseCase;
import com.banque.order.application.port.in.ListOrdersUseCase;
import com.banque.order.domain.exception.InvalidOrderException;
import com.banque.order.domain.exception.OrderNotFoundException;
import com.banque.order.domain.model.Money;
import com.banque.order.domain.model.Order;
import com.banque.order.domain.model.OrderId;
import com.banque.order.domain.model.OrderLine;
import com.banque.order.infrastructure.in.rest.mapper.OrderRestMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    private final ListOrdersUseCase listOrdersUseCase = mock(ListOrdersUseCase.class);
    private final GetOrderUseCase getOrderUseCase = mock(GetOrderUseCase.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new OrderController(createOrderUseCase, new OrderRestMapper(), listOrdersUseCase, getOrderUseCase))
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

    private static Order sampleOrder() {
        return Order.create("C-001",
                List.of(new OrderLine("BOOK", 2, Money.of(new BigDecimal("12.50"), "EUR"))),
                Instant.parse("2026-10-05T09:00:00Z"));
    }

    @Test
    void liste_les_commandes_page_par_page() throws Exception {
        Order order = sampleOrder();
        when(listOrdersUseCase.list(new PageQuery(1, 20))).thenReturn(new PageResult<>(List.of(order), 1, 20, 21));

        mockMvc.perform(get("/api/orders").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(order.id().value().toString()))
                .andExpect(jsonPath("$.content[0].totalAmount").value(25.0))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(21))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void renvoie_le_detail_d_une_commande_avec_ses_lignes() throws Exception {
        Order order = sampleOrder();
        when(getOrderUseCase.get(order.id())).thenReturn(order);

        mockMvc.perform(get("/api/orders/{id}", order.id().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("C-001"))
                .andExpect(jsonPath("$.lines[0].productCode").value("BOOK"))
                .andExpect(jsonPath("$.lines[0].quantity").value(2))
                .andExpect(jsonPath("$.lines[0].lineTotal").value(25.0));
    }

    @Test
    void renvoie_404_si_la_commande_n_existe_pas() throws Exception {
        OrderId unknown = OrderId.newId();
        when(getOrderUseCase.get(unknown)).thenThrow(new OrderNotFoundException(unknown));

        mockMvc.perform(get("/api/orders/{id}", unknown.value()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Commande introuvable"));
    }
}
