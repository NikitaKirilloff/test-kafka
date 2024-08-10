package kirilloff.orderservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for {@link Order}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDto {
  private Long id;
  private String items;
  private String userId;
  private Long cost;
  private Status status;
}