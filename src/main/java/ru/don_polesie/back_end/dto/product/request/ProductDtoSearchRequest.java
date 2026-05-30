package ru.don_polesie.back_end.dto.product.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProductDtoSearchRequest {
    private String brand;
    private String name;
}
