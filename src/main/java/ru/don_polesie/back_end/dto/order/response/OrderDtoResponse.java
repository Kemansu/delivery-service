package ru.don_polesie.back_end.dto.order.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.don_polesie.back_end.dto.address.response.AddressDtoResponse;
import ru.don_polesie.back_end.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDtoResponse {
    private Long id;
    private String phoneNumber;
    private AddressDtoResponse address;
    private LocalDateTime  createdAt;
    private LocalDateTime updatedAt;
    private OrderStatus status;
    private List<OrderItemDto> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class OrderItemDto {
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal price;
        private String imageUrl;
        private boolean isWeighted;
    }

    @Override
    public String toString() {
        return "OrderDtoResponse{" +
                "id=" + id +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", address=" + address +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", status=" + status +
                ", items=" + items +
                '}';
    }
}
