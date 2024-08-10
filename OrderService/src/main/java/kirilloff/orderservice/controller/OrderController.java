package kirilloff.orderservice.controller;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import kirilloff.orderservice.model.OrderDto;
import kirilloff.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;

  @PostMapping
  public OrderDto create(@RequestBody OrderDto dto) {
    return orderService.create(dto);
  }

  @GetMapping("{id}")
  public ResponseEntity<?> getById(@PathVariable long id) {
    try {
      return ResponseEntity.ok(orderService.getOrder(id));
    } catch (CallNotPermittedException e) {
      return ResponseEntity.status(500).body("Сервис сейчас недоступен,"
          + " попробуйте повторить запрос через несколько минут");
    }
    catch (NotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }
}

