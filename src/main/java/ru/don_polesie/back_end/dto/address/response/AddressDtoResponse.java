package ru.don_polesie.back_end.dto.address.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class AddressDtoResponse {
    private Long id;
    @NotBlank
    private String city;
    @NotBlank
    private String street;
    @NotNull
    private String houseNumber;

    private Integer apartmentNumber;

    private Integer floor;

    private String comment;
}
