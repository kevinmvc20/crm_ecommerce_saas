package com.saas.crm.dto.payment;

import java.time.LocalDateTime;

/**
 * DTO inmutable que representa el resultado de un intento de cobro
 * devuelto por cualquier implementacion de PaymentGatewayService.
 */
public record PaymentResult(
        boolean exitoso,
        String transaccionId,
        String mensaje,
        LocalDateTime fechaTransaccion
) {}