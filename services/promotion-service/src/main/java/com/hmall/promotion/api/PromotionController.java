package com.hmall.promotion.api;

import com.hmall.promotion.api.dto.CalculatePriceRequest;
import com.hmall.promotion.api.dto.CalculatePriceResponse;
import com.hmall.promotion.api.dto.CouponDto;
import com.hmall.promotion.api.dto.CouponTemplateDto;
import com.hmall.promotion.api.dto.CreateCouponTemplateRequest;
import com.hmall.promotion.api.dto.CreatePromotionActivityRequest;
import com.hmall.promotion.api.dto.IssueCouponRequest;
import com.hmall.promotion.api.dto.LockCouponRequest;
import com.hmall.promotion.api.dto.PreviewSkuPriceResponse;
import com.hmall.promotion.api.dto.PreviewSkuPricesRequest;
import com.hmall.promotion.api.dto.PromotionActivityDto;
import com.hmall.promotion.application.CouponApplicationService;
import com.hmall.promotion.application.CouponTemplateApplicationService;
import com.hmall.promotion.application.CouponTemplateApplicationService.CreateCouponTemplateCommand;
import com.hmall.promotion.application.PromotionActivityApplicationService;
import com.hmall.promotion.application.PromotionActivityApplicationService.CreatePromotionActivityCommand;
import com.hmall.promotion.domain.Coupon;
import com.hmall.promotion.domain.CouponStatus;
import com.hmall.promotion.domain.CouponTemplate;
import com.hmall.promotion.domain.PromotionActivity;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/promotion")
public class PromotionController {

    private final CouponTemplateApplicationService templateService;
    private final CouponApplicationService couponService;
    private final PromotionActivityApplicationService activityService;

    public PromotionController(
            CouponTemplateApplicationService templateService,
            CouponApplicationService couponService,
            PromotionActivityApplicationService activityService) {
        this.templateService = templateService;
        this.couponService = couponService;
        this.activityService = activityService;
    }

    @GetMapping("/health")
    public ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }

    // ── CouponTemplate endpoints ──

    @PostMapping("/coupon-templates")
    public ResponseEntity<CouponTemplateDto> createTemplate(@Valid @RequestBody CreateCouponTemplateRequest req) {
        CreateCouponTemplateCommand cmd = new CreateCouponTemplateCommand(
                req.name(), req.type(), req.thresholdCents(),
                req.discountCents(), req.discountRate(), req.maxDiscountCents(),
                req.totalQuantity(), req.perUserLimit(), req.validDays(),
                toCouponTemplateTargetingRule(req.targetingRule())
        );
        CouponTemplate template = templateService.create(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(CouponTemplateDto.from(template));
    }

    @GetMapping("/coupon-templates")
    public ResponseEntity<Page<CouponTemplateDto>> listTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CouponTemplateDto> result = templateService.list(PageRequest.of(page, size))
                .map(CouponTemplateDto::from);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/coupon-templates/{id}")
    public ResponseEntity<CouponTemplateDto> getTemplate(@PathVariable Long id) {
        CouponTemplate template = templateService.getById(id);
        return ResponseEntity.ok(CouponTemplateDto.from(template));
    }

    @PostMapping("/coupon-templates/{id}/deactivate")
    public ResponseEntity<CouponTemplateDto> deactivateTemplate(@PathVariable Long id) {
        CouponTemplate template = templateService.deactivate(id);
        return ResponseEntity.ok(CouponTemplateDto.from(template));
    }

    @PostMapping("/coupon-templates/{id}/issue")
    public ResponseEntity<List<CouponDto>> issueCoupons(
            @PathVariable Long id,
            @Valid @RequestBody IssueCouponRequest req) {
        List<Coupon> coupons = couponService.issueCoupons(id, req.userId(), req.quantity());
        List<CouponDto> dtos = coupons.stream().map(CouponDto::from).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/coupon-templates/claimable")
    public ResponseEntity<List<CouponTemplateDto>> listClaimableTemplates(@RequestParam Long userId) {
        List<CouponTemplate> templates = couponService.findClaimableTemplates(userId);
        List<CouponTemplateDto> dtos = templates.stream().map(CouponTemplateDto::from).toList();
        return ResponseEntity.ok(dtos);
    }

    // ── Coupon endpoints ──

    @PostMapping("/coupon-templates/{id}/claim")
    public ResponseEntity<CouponDto> claimCoupon(
            @PathVariable Long id,
            @RequestParam Long userId) {
        Coupon coupon = couponService.claimCoupon(id, userId);
        return ResponseEntity.ok(CouponDto.from(coupon));
    }

    @GetMapping("/coupons/my")
    public ResponseEntity<List<CouponDto>> myCoupons(
            @RequestParam Long userId,
            @RequestParam(required = false) CouponStatus status) {
        List<Coupon> coupons = status != null
                ? couponService.findByUserIdAndStatus(userId, status)
                : couponService.findByUserId(userId);
        List<CouponDto> dtos = coupons.stream().map(CouponDto::from).toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/coupons/expire-scan")
    public ResponseEntity<Map<String, Integer>> expireScan() {
        int count = couponService.expireCoupons();
        return ResponseEntity.ok(Map.of("expiredCount", count));
    }

    // ── Price Calculation & Coupon Lifecycle endpoints ──

    @PostMapping("/calculate-price")
    public ResponseEntity<CalculatePriceResponse> calculatePrice(
            @Valid @RequestBody CalculatePriceRequest request) {
        CalculatePriceResponse response = couponService.calculatePrice(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/coupons/available")
    public ResponseEntity<List<CouponDto>> availableCoupons(
            @RequestParam Long userId,
            @RequestParam Long orderAmountCents) {
        List<Coupon> coupons = couponService.findAvailableCoupons(userId, orderAmountCents);
        return ResponseEntity.ok(coupons.stream().map(CouponDto::from).toList());
    }

    @PostMapping("/coupons/{id}/lock")
    public ResponseEntity<CouponDto> lockCoupon(
            @PathVariable Long id,
            @Valid @RequestBody LockCouponRequest request) {
        Coupon coupon = couponService.lockCoupon(id, request.orderId());
        return ResponseEntity.ok(CouponDto.from(coupon));
    }

    @PostMapping("/coupons/{id}/redeem")
    public ResponseEntity<CouponDto> redeemCoupon(@PathVariable Long id) {
        Coupon coupon = couponService.redeemCoupon(id);
        return ResponseEntity.ok(CouponDto.from(coupon));
    }

    @PostMapping("/coupons/{id}/release")
    public ResponseEntity<CouponDto> releaseCoupon(@PathVariable Long id) {
        Coupon coupon = couponService.releaseCoupon(id);
        return ResponseEntity.ok(CouponDto.from(coupon));
    }

    // ── Promotion Activity endpoints ──

    @PostMapping("/activities")
    public ResponseEntity<PromotionActivityDto> createActivity(
            @Valid @RequestBody CreatePromotionActivityRequest request) {
        CreatePromotionActivityCommand command = new CreatePromotionActivityCommand(
                request.name(),
                request.type(),
                request.targetSkuIds() == null ? null : Set.copyOf(request.targetSkuIds()),
                request.thresholdCents(),
                request.discountCents(),
                request.mutexGroupCode(),
                request.priority(),
                request.startAt(),
                request.endAt(),
                toTargetingRule(request.targetingRule()),
                toPieceRule(request.pieceRule())
        );
        PromotionActivity activity = activityService.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(PromotionActivityDto.from(activity));
    }

    @GetMapping("/activities")
    public ResponseEntity<Page<PromotionActivityDto>> listActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PromotionActivityDto> result = activityService.list(PageRequest.of(page, size))
                .map(PromotionActivityDto::from);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/activities/{id}/activate")
    public ResponseEntity<PromotionActivityDto> activateActivity(@PathVariable Long id) {
        return ResponseEntity.ok(PromotionActivityDto.from(activityService.activate(id)));
    }

    @PostMapping("/activities/{id}/deactivate")
    public ResponseEntity<PromotionActivityDto> deactivateActivity(@PathVariable Long id) {
        return ResponseEntity.ok(PromotionActivityDto.from(activityService.deactivate(id)));
    }

    @PostMapping("/preview-sku-prices")
    public ResponseEntity<PreviewSkuPriceResponse> previewSkuPrices(
            @Valid @RequestBody PreviewSkuPricesRequest request) {
        return ResponseEntity.ok(couponService.previewSkuPrices(request));
    }

    private static PromotionActivity.TargetingRule toTargetingRule(CreatePromotionActivityRequest.TargetingRule source) {
        if (source == null) return null;
        return new PromotionActivity.TargetingRule(
                source.levelsIn(),
                source.tagsAny(),
                source.tagsAll(),
                source.excludeTags()
        );
    }

    private static PromotionActivity.PieceRule toPieceRule(CreatePromotionActivityRequest.PieceRule source) {
        if (source == null) return null;
        return new PromotionActivity.PieceRule(
                source.scopeType(),
                source.scopeIds(),
                source.minQuantity(),
                source.discountType(),
                source.discountValue(),
                source.maxDiscountCents()
        );
    }

    private static CouponTemplate.TargetingRule toCouponTemplateTargetingRule(CreateCouponTemplateRequest.TargetingRule source) {
        if (source == null) return null;
        return new CouponTemplate.TargetingRule(
                source.levelsIn(),
                source.tagsAny(),
                source.tagsAll(),
                source.excludeTags()
        );
    }
}
