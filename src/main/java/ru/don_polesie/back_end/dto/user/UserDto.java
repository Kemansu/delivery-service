package ru.don_polesie.back_end.dto.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ru.don_polesie.back_end.model.Role;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class UserDto {
    public int id;
    public String name;
    public String surname;
    public String email;
    public String password;
    public String phoneNumber;
    public Set<String> roles;
    public boolean isActive;
}
