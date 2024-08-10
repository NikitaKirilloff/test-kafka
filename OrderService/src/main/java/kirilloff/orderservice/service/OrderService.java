package kirilloff.orderservice.service;

import kirilloff.orderservice.model.OrderDto;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;

public interface OrderService {


  OrderDto create(OrderDto dto);

  OrderDto getOrder(Long orderId) throws NotFoundException;
}
