package ru.don_polesie.back_end.dto.address.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class AddressDtoRequest {
    @NotBlank
    private String city;
    @NotBlank
    private String street;
    @NotNull
    private Integer houseNumber;

    private Integer apartmentNumber;

    private Integer floor;

    private String comment;
}
