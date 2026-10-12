package com.saas.crm.dto.payment;

import java.math.BigDecimal;

/**
 * DTO inmutable que encapsula los datos necesarios para procesar
 * un cobro de suscripcion SaaS a traves del PaymentGatewayService.
 */
public record PaymentRequest(
        String metodoPago,
        BigDecimal monto,
        String numeroTarjeta,
        String titular,
        String expiracion,
        String cvv,
        String referenciaQR
) {}