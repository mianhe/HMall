package com.hmall.user.application;

import com.hmall.user.domain.Address;
import com.hmall.user.domain.AddressRepository;
import com.hmall.user.domain.User;
import com.hmall.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressApplicationService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressApplicationService(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Address create(Long userId, String recipientName, String phone,
                          String province, String city, String district, String detail) {
        userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        validateAddressFields(recipientName, phone, province, city, district, detail);
        Address address = new Address(userId, recipientName, phone, province, city, district, detail);
        return addressRepository.save(address);
    }

    @Transactional(readOnly = true)
    public List<Address> listByUserId(Long userId) {
        return addressRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Address getById(Long userId, Long addressId) {
        Address address = addressRepository.findById(addressId)
            .orElseThrow(() -> new IllegalArgumentException("地址不存在"));
        if (!address.getUserId().equals(userId)) {
            throw new IllegalArgumentException("地址不存在");
        }
        return address;
    }

    @Transactional
    public Address update(Long userId, Long addressId, String recipientName, String phone,
                          String province, String city, String district, String detail) {
        Address existing = getById(userId, addressId);
        validateAddressFields(recipientName, phone, province, city, district, detail);
        Address updated = new Address(addressId, userId, recipientName, phone, province, city, district, detail);
        return addressRepository.save(updated);
    }

    @Transactional
    public void delete(Long userId, Long addressId) {
        getById(userId, addressId);
        addressRepository.deleteById(addressId);
    }

    private void validateAddressFields(String recipientName, String phone, String province,
                                       String city, String district, String detail) {
        if (recipientName == null || recipientName.isBlank()) {
            throw new UserBadRequestException("收件人不能为空");
        }
        if (phone == null || phone.isBlank()) {
            throw new UserBadRequestException("手机号不能为空");
        }
        if (province == null || province.isBlank()) {
            throw new UserBadRequestException("省/直辖市不能为空");
        }
        if (city == null || city.isBlank()) {
            throw new UserBadRequestException("城市不能为空");
        }
        if (district == null || district.isBlank()) {
            throw new UserBadRequestException("区/县不能为空");
        }
        if (detail == null || detail.isBlank()) {
            throw new UserBadRequestException("详细地址不能为空");
        }
    }
}
