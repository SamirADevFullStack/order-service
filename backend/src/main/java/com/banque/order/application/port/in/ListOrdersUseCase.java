package com.banque.order.application.port.in;

import com.banque.order.application.pagination.PageQuery;
import com.banque.order.application.pagination.PageResult;
import com.banque.order.domain.model.Order;

/** PORT ENTRANT : lister les commandes, de la plus récente à la plus ancienne. */
public interface ListOrdersUseCase {

    PageResult<Order> list(PageQuery query);
}