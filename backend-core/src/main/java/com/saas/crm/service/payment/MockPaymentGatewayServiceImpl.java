package com.saas.crm.service.payment;

import com.saas.crm.dto.payment.PaymentRequest;
import com.saas.crm.dto.payment.PaymentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementacion simulada (Mock) de PaymentGatewayService.
 * Aprueba TARJETA si tiene 16 digitos, y QR si la referencia no es nula.
 */
@Slf4j
@Service
public class MockPaymentGatewayServiceImpl implements PaymentGatewayService {

    private static final String TARJETA_PRUEBA = "4242424242424242";

    @Override
    public PaymentResult procesarPagoSuscripcion(PaymentRequest request) {
        log.info("[MockPayment] Procesando pago | metodo={} monto={}", request.metodoPago(), request.monto());
        return switch (request.metodoPago().toUpperCase()) {
            case "TARJETA" -> procesarTarjeta(request);
            case "QR"      -> procesarQR(request);
            default        -> rechazar("Metodo de pago no soportado: " + request.metodoPago());
        };
    }

    private PaymentResult procesarTarjeta(PaymentRequest request) {
        String numero = request.numeroTarjeta() == null ? "" : request.numeroTarjeta().replaceAll("\\s+", "");
        if (numero.length() != 16 && !TARJETA_PRUEBA.equals(numero)) {
            return rechazar("Numero de tarjeta invalido. Debe contener 16 digitos.");
        }
        return aprobar("Pago con tarjeta aprobado por el procesador simulado.");
    }

    private PaymentResult procesarQR(PaymentRequest request) {
        if (request.referenciaQR() == null || request.referenciaQR().isBlank()) {
            return rechazar("La referencia QR no puede estar vacia.");
        }
        return aprobar("Pago QR verificado y aprobado por el procesador simulado.");
    }

    private PaymentResult aprobar(String mensaje) {
        String txId = "MOCK-" + UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 16);
        log.info("[MockPayment] APROBADO | transaccionId={}", txId);
        return new PaymentResult(true, txId, mensaje, LocalDateTime.now());
    }

    private PaymentResult rechazar(String motivo) {
        log.warn("[MockPayment] RECHAZADO | motivo={}", motivo);
        return new PaymentResult(false, null, motivo, LocalDateTime.now());
    }
}