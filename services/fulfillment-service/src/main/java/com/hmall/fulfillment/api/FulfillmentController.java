package com.hmall.fulfillment.api;

import com.hmall.fulfillment.api.dto.CancelFulfillmentRequestDto;
import com.hmall.fulfillment.api.dto.CancelFulfillmentResponseDto;
import com.hmall.fulfillment.api.dto.CreateFulfillmentRequestDto;
import com.hmall.fulfillment.api.dto.CreateFulfillmentResponseDto;
import com.hmall.fulfillment.api.dto.FulfillmentOrderDto;
import com.hmall.fulfillment.api.dto.ShipRequestDto;
import com.hmall.fulfillment.application.FulfillmentAllocateApplicationService;
import com.hmall.fulfillment.application.FulfillmentCancelApplicationService;
import com.hmall.fulfillment.application.FulfillmentCancelApplicationService.CancelResult;
import com.hmall.fulfillment.application.FulfillmentCreateApplicationService;
import com.hmall.fulfillment.application.FulfillmentCreateApplicationService.CreateFulfillmentItem;
import com.hmall.fulfillment.application.FulfillmentCreateApplicationService.CreateFulfillmentResult;
import com.hmall.fulfillment.application.FulfillmentCreateApplicationService.CreateShippingAddress;
import com.hmall.fulfillment.application.FulfillmentDeliverApplicationService;
import com.hmall.fulfillment.application.FulfillmentQueryApplicationService;
import com.hmall.fulfillment.application.FulfillmentShipApplicationService;
import com.hmall.fulfillment.domain.FulfillmentOrder;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/fulfillment")
public class FulfillmentController {

    private final FulfillmentCreateApplicationService createService;
    private final FulfillmentAllocateApplicationService allocateService;
    private final FulfillmentShipApplicationService shipService;
    private final FulfillmentDeliverApplicationService deliverService;
    private final FulfillmentCancelApplicationService cancelService;
    private final FulfillmentQueryApplicationService queryService;

    public FulfillmentController(FulfillmentCreateApplicationService createService,
                                FulfillmentAllocateApplicationService allocateService,
                                FulfillmentShipApplicationService shipService,
                                FulfillmentDeliverApplicationService deliverService,
                                FulfillmentCancelApplicationService cancelService,
                                FulfillmentQueryApplicationService queryService) {
        this.createService = createService;
        this.allocateService = allocateService;
        this.shipService = shipService;
        this.deliverService = deliverService;
        this.cancelService = cancelService;
        this.queryService = queryService;
    }

    @GetMapping("/health")
    public ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/create")
    public ResponseEntity<CreateFulfillmentResponseDto> create(
            @Valid @RequestBody CreateFulfillmentRequestDto dto) {
        var items = dto.items().stream()
            .map(i -> new CreateFulfillmentItem(i.skuId(), i.quantity(), i.itemType()))
            .toList();
        var address = new CreateShippingAddress(
            dto.shippingAddress().recipientName(), dto.shippingAddress().phone(),
            dto.shippingAddress().province(), dto.shippingAddress().city(),
            dto.shippingAddress().district(), dto.shippingAddress().detail()
        );
        CreateFulfillmentResult result = createService.create(dto.orderId(), items, address);
        return ResponseEntity.ok(new CreateFulfillmentResponseDto(result.orderId(), result.fulfillmentOrderIds()));
    }

    @PostMapping("/{fulfillmentOrderId}/allocate")
    public ResponseEntity<Void> allocate(@PathVariable Long fulfillmentOrderId) {
        allocateService.allocate(fulfillmentOrderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{fulfillmentOrderId}/ship")
    public ResponseEntity<Void> ship(@PathVariable Long fulfillmentOrderId,
                                     @Valid @RequestBody ShipRequestDto dto) {
        shipService.ship(fulfillmentOrderId, dto.carrier(), dto.trackingNumber());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{fulfillmentOrderId}/deliver")
    public ResponseEntity<Void> deliver(@PathVariable Long fulfillmentOrderId) {
        deliverService.deliver(fulfillmentOrderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cancel")
    public ResponseEntity<CancelFulfillmentResponseDto> cancel(
            @Valid @RequestBody CancelFulfillmentRequestDto dto) {
        CancelResult result = cancelService.cancel(dto.orderId());
        return ResponseEntity.ok(new CancelFulfillmentResponseDto(result.orderId(), result.cancelledCount()));
    }

    @GetMapping("/{fulfillmentOrderId}")
    public ResponseEntity<FulfillmentOrderDto> getById(@PathVariable Long fulfillmentOrderId) {
        FulfillmentOrder order = queryService.getById(fulfillmentOrderId);
        return ResponseEntity.ok(FulfillmentOrderDto.from(order));
    }

    @GetMapping
    public ResponseEntity<List<FulfillmentOrderDto>> list(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String status) {
        List<FulfillmentOrderDto> result = queryService.list(orderId, status).stream()
            .map(FulfillmentOrderDto::from)
            .toList();
        return ResponseEntity.ok(result);
    }
}
