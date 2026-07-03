package com.darkom.payment.dto;

import java.util.UUID;

/** redirectUrl points at the mock CMI gateway - see CmiClient's class-level doc. */
public record PaymentInitiationResponse(
    UUID paymentId, String cmiTransactionId, String redirectUrl) {}
