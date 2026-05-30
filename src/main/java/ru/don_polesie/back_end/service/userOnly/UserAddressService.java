package ru.don_polesie.back_end.service.userOnly;

import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.don_polesie.back_end.dto.address.request.AddressDtoRequest;
import ru.don_polesie.back_end.dto.address.response.AddressDtoResponse;
import ru.don_polesie.back_end.mapper.AddressMapper;
import ru.don_polesie.back_end.model.user.Address;
import ru.don_polesie.back_end.model.user.User;
import ru.don_polesie.back_end.repository.AddressRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAddressService {
    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;

    /**
     * Получает список адресов пользователя
     *
     * @param user пользователь, для которого запрашиваются адреса
     * @return список адресов в формате DTO
     */

    public List<AddressDtoResponse> getUserAddresses(User user) {
        return addressRepository.findByUserAndActiveTrue(user)
                .stream()
                .map(addressMapper::toDto)
                .toList();
    }

    /**
     * Сохраняет новый адрес для пользователя
     *
     * @param addressDTO данные адреса для сохранения
     * @param user       пользователь, для которого сохраняется адрес
     * @return строковое представление сохраненного адреса
     */
    @Transactional
    public void save(AddressDtoRequest addressDTO, User user) {
        Address address = addressMapper.toEntityFromAddressDtoRequest(addressDTO);
        address.setUser(user);
        address.setActive(true);
        addressRepository.save(address);
    }

    /**
     * Удаляет адрес пользователя по идентификатору
     *
     * @param id   идентификатор адреса для удаления
     * @param user пользователь, для проверки прав доступа
     * @throws IllegalArgumentException если адрес с указанным id не найден
     */

    public void delete(Long id, User user) {
        if (!addressRepository.existsById(id)) {
            throw new IllegalArgumentException(String.format("Address %n not found.", id));
        }
        addressRepository.deleteById(id);
    }

    @Transactional
    public void deactivate(@Min(0) Long id, User user) {
        if (!addressRepository.existsById(id)) {
            throw new IllegalArgumentException(String.format("Address %n not found.", id));
        }
        addressRepository.deactivateById(id, user);
    }

    @Transactional
    public void updateComment(Long id, String comment, User user) {
        addressRepository.updateComment(id, comment, user);
    }
}