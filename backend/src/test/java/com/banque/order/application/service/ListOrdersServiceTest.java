package com.banque.order.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.banque.order.application.pagination.PageQuery;
import com.banque.order.application.pagination.PageResult;
import com.banque.order.application.port.out.OrderRepository;
import com.banque.order.domain.model.Order;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListOrdersServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private ListOrdersService service;

    /*
    @BeforeEach
    void setUp() {
        service = new ListOrdersService(orderRepository);
    }
    */

    @Test
    void renvoie_la_page_fournie_par_le_port_sortant() {
        // given : le port sortant renverra cette page pour cette demande
        PageQuery query = new PageQuery(1, 20);
        PageResult<Order> page = new PageResult<>(List.of(), 1, 20, 25);
        // programmer le mock avec when(...).thenReturn(...)
        when(orderRepository.findAll(query)).thenReturn(page);
        // when
        PageResult<Order> result = service.list(query);

        // then
        // vérifier que le résultat est EXACTEMENT la page du port (indice AssertJ : isSameAs)
        assertThat(result).isSameAs(page);
    }
}