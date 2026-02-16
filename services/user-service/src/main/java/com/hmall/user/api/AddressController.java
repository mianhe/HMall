package com.hmall.user.api;

import com.hmall.user.api.dto.AddressCreateDto;
import com.hmall.user.api.dto.AddressDto;
import com.hmall.user.api.dto.AddressUpdateDto;
import com.hmall.user.application.AddressApplicationService;
import com.hmall.user.domain.Address;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/addresses")
public class AddressController {

    private final AddressApplicationService applicationService;

    public AddressController(AddressApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<AddressDto> create(@PathVariable Long userId, @Valid @RequestBody AddressCreateDto dto) {
        Address created = applicationService.create(
            userId,
            dto.recipientName(),
            dto.phone(),
            dto.province(),
            dto.city(),
            dto.district(),
            dto.detail()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(AddressDto.from(created));
    }

    @GetMapping
    public List<AddressDto> list(@PathVariable Long userId) {
        return applicationService.listByUserId(userId).stream()
            .map(AddressDto::from)
            .toList();
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<AddressDto> getById(@PathVariable Long userId, @PathVariable Long addressId) {
        Address address = applicationService.getById(userId, addressId);
        return ResponseEntity.ok(AddressDto.from(address));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressDto> update(@PathVariable Long userId, @PathVariable Long addressId,
                                             @Valid @RequestBody AddressUpdateDto dto) {
        Address updated = applicationService.update(
            userId,
            addressId,
            dto.recipientName(),
            dto.phone(),
            dto.province(),
            dto.city(),
            dto.district(),
            dto.detail()
        );
        return ResponseEntity.ok(AddressDto.from(updated));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> delete(@PathVariable Long userId, @PathVariable Long addressId) {
        applicationService.delete(userId, addressId);
        return ResponseEntity.noContent().build();
    }
}
