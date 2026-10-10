package com.banque.order.infrastructure.in.rest;

import com.banque.order.application.port.in.CreateOrderUseCase;
import com.banque.order.application.port.in.GetOrderUseCase;
import com.banque.order.application.port.in.ListOrdersUseCase;
import com.banque.order.infrastructure.in.rest.mapper.OrderRestMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validation des paramètres de requête (@Min/@Max générés depuis le contrat).
 * L'interface générée porte @Validated : la validation passe par un proxy Spring,
 * absent en standaloneSetup. D'où @WebMvcTest, qui charge la vraie configuration Spring MVC.
 */
@WebMvcTest(OrderController.class)
@Import({OrderRestMapper.class, RestExceptionHandler.class})
class OrderControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateOrderUseCase createOrderUseCase;

    @MockitoBean
    private ListOrdersUseCase listOrdersUseCase;

    @MockitoBean
    private GetOrderUseCase getOrderUseCase;

    @Test
    void refuse_une_taille_de_page_superieure_a_100_avec_une_400() throws Exception {
        mockMvc.perform(get("/api/orders").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Paramètres invalides"));

        verifyNoInteractions(listOrdersUseCase);
    }

    @Test
    void refuse_un_numero_de_page_negatif_avec_une_400() throws Exception {
        mockMvc.perform(get("/api/orders").param("page", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(listOrdersUseCase);
    }
}