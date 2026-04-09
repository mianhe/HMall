package com.hmall.promotion.application;

import com.hmall.promotion.api.dto.CalculatePriceRequest;
import com.hmall.promotion.api.dto.CalculatePriceResponse;
import com.hmall.promotion.api.dto.CalculatePriceResponse.ActivityExplanation;
import com.hmall.promotion.api.dto.CalculatePriceResponse.DiscountDetailDto;
import com.hmall.promotion.api.dto.CalculatePriceResponse.LineItemDiscount;
import com.hmall.promotion.api.dto.PreviewSkuPriceResponse;
import com.hmall.promotion.api.dto.PreviewSkuPricesRequest;
import com.hmall.promotion.domain.Coupon;
import com.hmall.promotion.domain.CouponRepository;
import com.hmall.promotion.domain.CouponStatus;
import com.hmall.promotion.domain.CouponTemplate;
import com.hmall.promotion.domain.CouponTemplateRepository;
import com.hmall.promotion.domain.CouponType;
import com.hmall.promotion.domain.PromotionActivity;
import com.hmall.promotion.domain.PromotionActivityRepository;
import com.hmall.promotion.domain.PromotionActivityStatus;
import com.hmall.promotion.domain.PromotionActivityType;
import com.hmall.promotion.domain.TemplateStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class CouponApplicationService {

    private final CouponRepository couponRepository;
    private final CouponTemplateRepository templateRepository;
    private final PromotionActivityRepository promotionActivityRepository;
    private final UserSegmentResolver userSegmentResolver;

    public CouponApplicationService(
            CouponRepository couponRepository,
            CouponTemplateRepository templateRepository,
            PromotionActivityRepository promotionActivityRepository,
            UserSegmentResolver userSegmentResolver) {
        this.couponRepository = couponRepository;
        this.templateRepository = templateRepository;
        this.promotionActivityRepository = promotionActivityRepository;
        this.userSegmentResolver = userSegmentResolver;
    }

    @Transactional
    public List<Coupon> issueCoupons(long templateId, long userId, int quantity) {
        CouponTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("券模板不存在: " + templateId));
        validateForIssue(template, userId, quantity);

        List<Coupon> coupons = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            coupons.add(Coupon.issueFromTemplate(template, userId));
        }
        template.incrementIssuedQuantity(quantity);
        templateRepository.save(template);
        return couponRepository.saveAll(coupons);
    }

    @Transactional
    public Coupon claimCoupon(long templateId, long userId) {
        return issueCoupons(templateId, userId, 1).getFirst();
    }

    @Transactional(readOnly = true)
    public List<Coupon> findByUserId(long userId) {
        return couponRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Coupon> findByUserIdAndStatus(long userId, CouponStatus status) {
        return couponRepository.findByUserIdAndStatus(userId, status);
    }

    @Transactional(readOnly = true)
    public List<CouponTemplate> findClaimableTemplates(long userId) {
        List<CouponTemplate> activeTemplates = templateRepository.findByStatus(TemplateStatus.ACTIVE);
        if (activeTemplates.isEmpty()) {
            return List.of();
        }
        UserSegments userSegments = userSegmentResolver.resolve(userId);
        List<Long> templateIds = activeTemplates.stream().map(CouponTemplate::getId).toList();
        Map<Long, Long> userCounts = couponRepository.countByUserIdAndTemplateIds(userId, templateIds);
        return activeTemplates.stream()
                .filter(t -> matchesTemplateTargeting(t.getTargetingRule(), userSegments))
                .filter(t -> t.hasStock(1))
                .filter(t -> userCounts.getOrDefault(t.getId(), 0L) < t.getPerUserLimit())
                .toList();
    }

    @Transactional
    public int expireCoupons() {
        List<Coupon> expired = couponRepository.findByStatusAndExpiresAtBefore(
                CouponStatus.AVAILABLE, Instant.now());
        for (Coupon coupon : expired) {
            coupon.expire();
        }
        couponRepository.saveAll(expired);
        return expired.size();
    }

    @Transactional(readOnly = true)
    public CalculatePriceResponse calculatePrice(CalculatePriceRequest request) {
        Instant now = Instant.now();
        long originalAmountCents = request.items().stream()
                .mapToLong(item -> item.unitPriceCents() * item.quantity())
                .sum();
        List<WorkingLine> lines = createWorkingLines(request.items());
        UserSegments userSegments = userSegmentResolver.resolve(request.userId());
        ActivityOutcome activityOutcome = applyActivities(lines, originalAmountCents, now, userSegments);
        long activityDiscountAmountCents = activityOutcome.totalDiscount();
        long amountAfterActivity = Math.max(0, originalAmountCents - activityDiscountAmountCents);

        long couponDiscountAmountCents = 0L;
        Coupon coupon = null;
        if (request.couponId() != null) {
            coupon = couponRepository.findById(request.couponId())
                    .orElseThrow(() -> new PromotionBadRequestException("优惠券不存在"));
            validateCoupon(coupon, request.userId(), amountAfterActivity, now);
            couponDiscountAmountCents = computeDiscount(coupon, amountAfterActivity);
            applyCoupon(lines, coupon, couponDiscountAmountCents);
        }

        long discountAmountCents = activityDiscountAmountCents + couponDiscountAmountCents;
        long payableAmountCents = Math.max(0, originalAmountCents - discountAmountCents);
        List<LineItemDiscount> lineItems = lines.stream()
                .map(WorkingLine::toLineItemDiscount)
                .toList();
        return new CalculatePriceResponse(
                originalAmountCents,
                activityDiscountAmountCents,
                couponDiscountAmountCents,
                discountAmountCents,
                payableAmountCents,
                lineItems,
                activityOutcome.explanations());
    }

    @Transactional(readOnly = true)
    public PreviewSkuPriceResponse previewSkuPrices(PreviewSkuPricesRequest request) {
        List<PromotionActivity> activeActivities = findActiveActivities(Instant.now());
        UserSegments userSegments = request.userId() == null
                ? new UserSegments("L1", Set.of())
                : userSegmentResolver.resolve(request.userId());
        List<PreviewSkuPriceResponse.SkuPricePreview> items = request.items().stream()
                .map(item -> {
                    long bestDiscount = 0L;
                    String bestLabel = null;
                    boolean hasTargetingMiss = false;
                    for (PromotionActivity activity : activeActivities) {
                        if (activity.getType() != PromotionActivityType.SKU_AMOUNT_OFF) {
                            continue;
                        }
                        if (!activity.targetsSku(item.skuId())) {
                            continue;
                        }
                        if (!matchesTargeting(activity, userSegments)) {
                            hasTargetingMiss = true;
                            continue;
                        }
                        long discount = Math.min(item.unitPriceCents(), activity.getDiscountCents());
                        if (discount > bestDiscount) {
                            bestDiscount = discount;
                            bestLabel = activity.getName();
                        }
                    }
                    String reason = bestDiscount > 0
                            ? null
                            : (hasTargetingMiss ? "当前账号暂不满足活动条件" : null);
                    return new PreviewSkuPriceResponse.SkuPricePreview(
                            item.skuId(),
                            item.unitPriceCents(),
                            bestDiscount,
                            Math.max(0, item.unitPriceCents() - bestDiscount),
                            bestLabel,
                            reason
                    );
                })
                .toList();
        return new PreviewSkuPriceResponse(items);
    }

    @Transactional(readOnly = true)
    public List<Coupon> findAvailableCoupons(long userId, long orderAmountCents) {
        List<Coupon> available = couponRepository.findByUserIdAndStatus(userId, CouponStatus.AVAILABLE);
        Instant now = Instant.now();
        return available.stream()
                .filter(c -> c.getExpiresAt().isAfter(now))
                .filter(c -> c.getThresholdCents() <= orderAmountCents)
                .toList();
    }

    @Transactional
    public Coupon lockCoupon(long couponId, long orderId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new PromotionBadRequestException("优惠券不存在"));
        coupon.lock(orderId);
        return couponRepository.save(coupon);
    }

    @Transactional
    public Coupon redeemCoupon(long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new PromotionBadRequestException("优惠券不存在"));
        coupon.redeem();
        return couponRepository.save(coupon);
    }

    @Transactional
    public Coupon releaseCoupon(long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new PromotionBadRequestException("优惠券不存在"));
        coupon.release();
        return couponRepository.save(coupon);
    }

    private long computeDiscount(Coupon coupon, long originalAmountCents) {
        if (coupon.getType() == CouponType.AMOUNT_OFF) {
            return coupon.getDiscountCents() != null ? coupon.getDiscountCents() : 0;
        }
        BigDecimal rate = coupon.getDiscountRate();
        if (rate == null) return 0;
        long discount = BigDecimal.valueOf(originalAmountCents)
                .multiply(BigDecimal.ONE.subtract(rate))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
        if (coupon.getMaxDiscountCents() != null && discount > coupon.getMaxDiscountCents()) {
            discount = coupon.getMaxDiscountCents();
        }
        return discount;
    }

    private List<WorkingLine> createWorkingLines(List<CalculatePriceRequest.LineItem> items) {
        List<WorkingLine> result = new ArrayList<>();
        for (CalculatePriceRequest.LineItem item : items) {
            long lineTotalCents = item.unitPriceCents() * item.quantity();
            result.add(new WorkingLine(
                    item.skuId(),
                    item.unitPriceCents(),
                    item.quantity(),
                    lineTotalCents,
                    new ArrayList<>()));
        }
        return result;
    }

    private ActivityOutcome applyActivities(List<WorkingLine> lines, long originalAmountCents, Instant now, UserSegments userSegments) {
        List<PromotionActivity> activeActivities = findActiveActivities(now);
        if (activeActivities.isEmpty()) {
            return new ActivityOutcome(0L, List.of());
        }
        Map<Long, ActivityEvaluation> evaluations = new LinkedHashMap<>();
        Map<Long, ActivityApplication> candidates = new HashMap<>();
        for (PromotionActivity activity : activeActivities) {
            if (!matchesTargeting(activity, userSegments)) {
                evaluations.put(activity.getId(), new ActivityEvaluation(activity, null, "当前用户不满足活动定向条件"));
                continue;
            }
            ActivityEvaluation evaluation = evaluateActivity(activity, lines, originalAmountCents);
            evaluations.put(activity.getId(), evaluation);
            ActivityApplication app = evaluation.application();
            if (app == null || app.totalDiscount() <= 0) {
                continue;
            }
            candidates.put(activity.getId(), app);
        }
        Map<String, ActivityApplication> bestByGroup = new HashMap<>();
        for (ActivityApplication app : candidates.values()) {
            String groupKey = mutexGroupKey(app.activity());
            ActivityApplication current = bestByGroup.get(groupKey);
            if (current == null || compareApplication(app, current) > 0) {
                bestByGroup.put(groupKey, app);
            }
        }

        Set<Long> winnerIds = bestByGroup.values().stream()
                .map(a -> a.activity().getId())
                .collect(java.util.stream.Collectors.toSet());
        long totalDiscount = 0L;
        for (ActivityApplication app : bestByGroup.values()) {
            for (Map.Entry<Integer, Long> e : app.lineDiscounts().entrySet()) {
                WorkingLine line = lines.get(e.getKey());
                long amount = e.getValue();
                if (amount <= 0) {
                    continue;
                }
                line.discounts().add(new DiscountDetailDto(
                        "ACTIVITY", app.activity().getId(), amount, app.activity().getName()));
                totalDiscount += amount;
            }
        }

        List<ActivityExplanation> explanations = activeActivities.stream()
                .map(activity -> {
                    Long activityId = activity.getId();
                    if (winnerIds.contains(activityId)) {
                        return new ActivityExplanation(activityId, activity.getName(), true, "已命中并生效");
                    }
                    if (candidates.containsKey(activityId)) {
                        return new ActivityExplanation(activityId, activity.getName(), false, "命中但被互斥组更优活动替代");
                    }
                    String message = evaluations.containsKey(activityId)
                            ? evaluations.get(activityId).reason()
                            : "未命中活动条件";
                    return new ActivityExplanation(activityId, activity.getName(), false, message);
                })
                .toList();
        return new ActivityOutcome(totalDiscount, explanations);
    }

    private ActivityEvaluation evaluateActivity(
            PromotionActivity activity, List<WorkingLine> lines, long originalAmountCents) {
        if (activity.getPieceRule() != null) {
            return evaluatePieceRuleActivity(activity, lines);
        }
        if (activity.getType() == PromotionActivityType.SKU_AMOUNT_OFF) {
            Map<Integer, Long> lineDiscounts = new HashMap<>();
            long total = 0L;
            boolean hasTargetSku = false;
            for (int i = 0; i < lines.size(); i++) {
                WorkingLine line = lines.get(i);
                if (!activity.targetsSku(line.skuId())) {
                    continue;
                }
                hasTargetSku = true;
                long discount = Math.min(line.totalPriceCents(), activity.getDiscountCents() * line.quantity());
                if (discount <= 0) {
                    continue;
                }
                lineDiscounts.put(i, discount);
                total += discount;
            }
            if (!hasTargetSku) {
                return new ActivityEvaluation(activity, null, "订单中无活动指定商品");
            }
            if (total <= 0) {
                return new ActivityEvaluation(activity, null, "活动优惠金额为0");
            }
            return new ActivityEvaluation(activity, new ActivityApplication(activity, lineDiscounts, total), null);
        }
        if (activity.getType() == PromotionActivityType.ORDER_AMOUNT_OFF) {
            if (activity.getThresholdCents() == null || originalAmountCents < activity.getThresholdCents()) {
                return new ActivityEvaluation(activity, null, "未达到订单满减门槛");
            }
            long discount = Math.min(activity.getDiscountCents(), originalAmountCents);
            Map<Integer, Long> prorated = prorate(lines, discount, WorkingLine::totalPriceCents);
            if (discount <= 0) {
                return new ActivityEvaluation(activity, null, "活动优惠金额为0");
            }
            return new ActivityEvaluation(activity, new ActivityApplication(activity, prorated, discount), null);
        }
        return new ActivityEvaluation(activity, null, "不支持的活动类型");
    }

    private ActivityEvaluation evaluatePieceRuleActivity(PromotionActivity activity, List<WorkingLine> lines) {
        PromotionActivity.PieceRule rule = activity.getPieceRule();
        List<Integer> eligibleIndices = new ArrayList<>();
        long eligibleAmount = 0L;
        int eligibleQuantity = 0;
        for (int i = 0; i < lines.size(); i++) {
            WorkingLine line = lines.get(i);
            if (!isPieceRuleLineEligible(rule, line)) {
                continue;
            }
            eligibleIndices.add(i);
            eligibleAmount += line.totalPriceCents();
            eligibleQuantity += line.quantity();
        }
        if (eligibleIndices.isEmpty()) {
            return new ActivityEvaluation(activity, null, "订单中无满足满件范围的商品");
        }
        if (eligibleQuantity < rule.minQuantity()) {
            return new ActivityEvaluation(activity, null, "未达到满件门槛");
        }
        long discount = computePieceRuleDiscount(rule, eligibleAmount);
        if (discount <= 0) {
            return new ActivityEvaluation(activity, null, "满件规则优惠金额为0");
        }
        discount = Math.min(discount, eligibleAmount);
        List<WorkingLine> eligibleLines = eligibleIndices.stream().map(lines::get).toList();
        Map<Integer, Long> scopedProrate = prorate(eligibleLines, discount, WorkingLine::totalPriceCents);
        Map<Integer, Long> lineDiscounts = new HashMap<>();
        for (int idx = 0; idx < eligibleIndices.size(); idx++) {
            lineDiscounts.put(eligibleIndices.get(idx), scopedProrate.getOrDefault(idx, 0L));
        }
        long total = lineDiscounts.values().stream().mapToLong(Long::longValue).sum();
        if (total <= 0) {
            return new ActivityEvaluation(activity, null, "满件规则优惠金额为0");
        }
        return new ActivityEvaluation(activity, new ActivityApplication(activity, lineDiscounts, total), null);
    }

    private long computePieceRuleDiscount(PromotionActivity.PieceRule rule, long eligibleAmount) {
        if ("PERCENTAGE_OFF".equalsIgnoreCase(rule.discountType())) {
            long discount = BigDecimal.valueOf(eligibleAmount)
                    .multiply(BigDecimal.valueOf(rule.discountValue()))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                    .longValue();
            if (rule.maxDiscountCents() != null && discount > rule.maxDiscountCents()) {
                discount = rule.maxDiscountCents();
            }
            return discount;
        }
        return rule.discountValue() == null ? 0L : rule.discountValue();
    }

    private boolean isPieceRuleLineEligible(PromotionActivity.PieceRule rule, WorkingLine line) {
        if ("ORDER".equalsIgnoreCase(rule.scopeType()) || rule.scopeType() == null) {
            return true;
        }
        if (rule.scopeIds() == null || rule.scopeIds().isEmpty()) {
            return false;
        }
        if ("SKU".equalsIgnoreCase(rule.scopeType())) {
            return rule.scopeIds().contains(line.skuId());
        }
        return false;
    }

    private boolean matchesTargeting(PromotionActivity activity, UserSegments userSegments) {
        PromotionActivity.TargetingRule rule = activity.getTargetingRule();
        if (rule == null) {
            return true;
        }
        String level = userSegments.level();
        java.util.Set<String> tags = userSegments.tags();
        if (!rule.levelsIn().isEmpty() && !rule.levelsIn().contains(level)) {
            return false;
        }
        if (!rule.excludeTags().isEmpty() && rule.excludeTags().stream().anyMatch(tags::contains)) {
            return false;
        }
        if (!rule.tagsAll().isEmpty() && !tags.containsAll(rule.tagsAll())) {
            return false;
        }
        if (!rule.tagsAny().isEmpty() && rule.tagsAny().stream().noneMatch(tags::contains)) {
            return false;
        }
        return true;
    }

    private static int compareApplication(ActivityApplication a, ActivityApplication b) {
        return Comparator
                .comparingLong(ActivityApplication::totalDiscount)
                .thenComparingInt(o -> o.activity().getPriority())
                .thenComparingLong(o -> Objects.requireNonNullElse(o.activity().getId(), Long.MAX_VALUE))
                .compare(a, b);
    }

    private static String mutexGroupKey(PromotionActivity activity) {
        return activity.getMutexGroupCode() == null || activity.getMutexGroupCode().isBlank()
                ? "__single_" + activity.getId()
                : activity.getMutexGroupCode();
    }

    private List<PromotionActivity> findActiveActivities(Instant now) {
        return promotionActivityRepository.findByStatus(PromotionActivityStatus.ACTIVE).stream()
                .filter(a -> a.isActiveAt(now))
                .toList();
    }

    private void validateCoupon(Coupon coupon, long userId, long amountAfterActivity, Instant now) {
        if (coupon.getStatus() != CouponStatus.AVAILABLE) {
            throw new PromotionBadRequestException("优惠券不可用，当前状态: " + coupon.getStatus());
        }
        if (coupon.getUserId() != userId) {
            throw new PromotionBadRequestException("优惠券不属于该用户");
        }
        if (coupon.getExpiresAt().isBefore(now)) {
            throw new PromotionBadRequestException("优惠券已过期");
        }
        if (amountAfterActivity < coupon.getThresholdCents()) {
            throw new PromotionBadRequestException("订单金额未达到优惠券使用门槛");
        }
    }

    private void applyCoupon(List<WorkingLine> lines, Coupon coupon, long totalDiscount) {
        Map<Integer, Long> prorated = prorate(lines, totalDiscount,
                line -> Math.max(0, line.totalPriceCents() - line.activityDiscountAmount()));
        for (Map.Entry<Integer, Long> e : prorated.entrySet()) {
            if (e.getValue() <= 0) {
                continue;
            }
            lines.get(e.getKey()).discounts().add(new DiscountDetailDto(
                    "COUPON", coupon.getId(), e.getValue(), coupon.getName()));
        }
    }

    private Map<Integer, Long> prorate(
            List<WorkingLine> lines,
            long totalDiscount,
            java.util.function.ToLongFunction<WorkingLine> amountExtractor) {
        long baseTotal = lines.stream().mapToLong(amountExtractor).sum();
        Map<Integer, Long> result = new HashMap<>();
        long allocated = 0L;
        for (int i = 0; i < lines.size(); i++) {
            long lineBase = amountExtractor.applyAsLong(lines.get(i));
            long lineDiscount;
            if (i == lines.size() - 1) {
                lineDiscount = Math.max(0, totalDiscount - allocated);
            } else {
                lineDiscount = baseTotal > 0 ? totalDiscount * lineBase / baseTotal : 0;
                allocated += lineDiscount;
            }
            result.put(i, lineDiscount);
        }
        return result;
    }

    private List<LineItemDiscount> proRateDiscount(
            List<CalculatePriceRequest.LineItem> items, Coupon coupon, long totalDiscount) {
        long originalTotal = items.stream()
                .mapToLong(i -> i.unitPriceCents() * i.quantity())
                .sum();
        List<LineItemDiscount> result = new ArrayList<>();
        long allocated = 0;
        for (int i = 0; i < items.size(); i++) {
            CalculatePriceRequest.LineItem item = items.get(i);
            long lineTotalCents = item.unitPriceCents() * item.quantity();
            long lineDiscount;
            if (i == items.size() - 1) {
                lineDiscount = totalDiscount - allocated;
            } else {
                lineDiscount = originalTotal > 0
                        ? totalDiscount * lineTotalCents / originalTotal
                        : 0;
                allocated += lineDiscount;
            }
            List<DiscountDetailDto> discounts = lineDiscount > 0
                    ? List.of(new DiscountDetailDto("COUPON", coupon.getId(), lineDiscount, coupon.getName()))
                    : List.of();
            result.add(new LineItemDiscount(item.skuId(), item.unitPriceCents(), item.quantity(), lineTotalCents, discounts));
        }
        return result;
    }

    private void validateForIssue(CouponTemplate template, long userId, int quantity) {
        if (template.getStatus() != TemplateStatus.ACTIVE) {
            throw new PromotionBadRequestException("券模板已停用");
        }
        UserSegments userSegments = userSegmentResolver.resolve(userId);
        if (!matchesTemplateTargeting(template.getTargetingRule(), userSegments)) {
            throw new PromotionBadRequestException("当前用户不满足券模板定向条件");
        }
        if (!template.hasStock(quantity)) {
            throw new PromotionBadRequestException("库存不足");
        }
        long existingCount = couponRepository.countByTemplateIdAndUserId(template.getId(), userId);
        if (existingCount + quantity > template.getPerUserLimit()) {
            throw new PromotionBadRequestException("超过每人限领数量");
        }
    }

    private boolean matchesTemplateTargeting(CouponTemplate.TargetingRule rule, UserSegments userSegments) {
        if (rule == null) {
            return true;
        }
        String level = userSegments.level();
        Set<String> tags = userSegments.tags();
        if (!rule.levelsIn().isEmpty() && !rule.levelsIn().contains(level)) {
            return false;
        }
        if (!rule.excludeTags().isEmpty() && rule.excludeTags().stream().anyMatch(tags::contains)) {
            return false;
        }
        if (!rule.tagsAll().isEmpty() && !tags.containsAll(rule.tagsAll())) {
            return false;
        }
        if (!rule.tagsAny().isEmpty() && rule.tagsAny().stream().noneMatch(tags::contains)) {
            return false;
        }
        return true;
    }

    private record ActivityApplication(
            PromotionActivity activity,
            Map<Integer, Long> lineDiscounts,
            long totalDiscount) {
    }

    private record ActivityEvaluation(
            PromotionActivity activity,
            ActivityApplication application,
            String reason
    ) {
    }

    private record ActivityOutcome(
            long totalDiscount,
            List<ActivityExplanation> explanations
    ) {
    }

    private record WorkingLine(
            Long skuId,
            long unitPriceCents,
            int quantity,
            long totalPriceCents,
            List<DiscountDetailDto> discounts
    ) {
        long activityDiscountAmount() {
            return discounts.stream()
                    .filter(d -> "ACTIVITY".equals(d.type()))
                    .mapToLong(DiscountDetailDto::amountCents)
                    .sum();
        }

        LineItemDiscount toLineItemDiscount() {
            return new LineItemDiscount(skuId, unitPriceCents, quantity, totalPriceCents, List.copyOf(discounts));
        }
    }
}
