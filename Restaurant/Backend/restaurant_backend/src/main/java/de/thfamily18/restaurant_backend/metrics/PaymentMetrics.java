package de.thfamily18.restaurant_backend.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentMetrics {

    private final MeterRegistry meterRegistry;

    private Counter paymentCreated;
    private Counter paymentSuccess;
    private Counter paymentFailed;

    private Counter refundRequested;
    private Counter refundSuccess;
    private Counter refundFailed;

    private DistributionSummary refundAmount;

    private Timer paymentTimer;

    @PostConstruct
    void init() {
        paymentCreated = meterRegistry.counter("payment.created");
        paymentSuccess = meterRegistry.counter("payment.success");
        paymentFailed = meterRegistry.counter("payment.failed");

        refundRequested = meterRegistry.counter("refund.requested");
        refundSuccess = meterRegistry.counter("refund.success");
        refundFailed = meterRegistry.counter("refund.failed");

        refundAmount = DistributionSummary.builder("refund.amount")
                .baseUnit("eur")
                .register(meterRegistry);

        paymentTimer = meterRegistry.timer("payment.duration");
    }

    public Counter paymentCreated() { return paymentCreated; }
    public Counter paymentSuccess() { return paymentSuccess; }
    public Counter paymentFailed() { return paymentFailed; }

    public Counter refundRequested() { return refundRequested; }
    public Counter refundSuccess() { return refundSuccess; }
    public Counter refundFailed() { return refundFailed; }

    public DistributionSummary refundAmount() { return refundAmount; }
    public Timer paymentTimer() { return paymentTimer; }
}
