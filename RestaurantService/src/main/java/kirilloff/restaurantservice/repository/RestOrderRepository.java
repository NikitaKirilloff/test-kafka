package kirilloff.restaurantservice.repository;

import kirilloff.restaurantservice.model.RestOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestOrderRepository extends JpaRepository<RestOrder, Long> {

}