package com.saas.crm.service.payment;

import com.saas.crm.dto.payment.PaymentRequest;
import com.saas.crm.dto.payment.PaymentResult;

/**
 * Contrato de pasarela de pagos para suscripciones SaaS.
 * El patron Strategy permite intercambiar implementaciones sin modificar
 * el servicio de negocio OnboardingService.
 */
public interface PaymentGatewayService {

    /**
     * Procesa el cobro de una suscripcion SaaS.
     */
    PaymentResult procesarPagoSuscripcion(PaymentRequest request);
}