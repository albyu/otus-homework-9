package ru.boldyrev.otus.model.transfer;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import ru.boldyrev.otus.model.entity.OrderItem;

@Data
@Accessors(chain = true)
@NoArgsConstructor
public class TransportableOrderItem {

    private Long id;

    private int quantity;

    private TransportableProduct product;


    public TransportableOrderItem(OrderItem orderItem){
        this.id = orderItem.getId();
        this.quantity = orderItem.getQuantity();
        this.product = new TransportableProduct(orderItem.getProduct());
    }
}
