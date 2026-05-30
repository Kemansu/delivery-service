package ru.don_polesie.back_end.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.don_polesie.back_end.dto.order.response.OrderDtoResponse;
import ru.don_polesie.back_end.model.order.Order;
import ru.don_polesie.back_end.model.order.OrderProduct;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "orderProducts", target = "items", qualifiedByName = "mapItems")
    OrderDtoResponse toOrderDtoResponse(Order order);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderProducts", ignore = true)
    Order toOrder(OrderDtoResponse orderDtoResponse);

    @Named("mapItems")
    default List<OrderDtoResponse.OrderItemDto> mapItems(Set<OrderProduct> ops) {
        return ops.stream()
                .map(op -> new OrderDtoResponse.OrderItemDto(
                        op.getProduct().getId(),
                        op.getProduct().getName(),
                        op.getQuantity(),
                        op.getProduct().getPrice(),
                        op.getProduct().getImageUrl(),
                        op.getProduct().getIsWeighted())
                )
                .collect(Collectors.toList());
    }
}
