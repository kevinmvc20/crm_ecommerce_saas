package com.saas.crm.dto.crm;

import com.saas.crm.domain.enums.EtapaOportunidad;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Respuesta inmutable con la representación pública de una Oportunidad Comercial.
 *
 * @param id                   Identificador único de la oportunidad.
 * @param clienteId            UUID del cliente asociado.
 * @param razonSocialCliente   Nombre o razón social del cliente.
 * @param vendedorId           UUID del vendedor responsable.
 * @param nombreVendedor       Nombre completo del vendedor.
 * @param nombre               Nombre descriptivo del trato (alias de {@code titulo}).
 * @param montoEstimado        Monto estimado de la oportunidad.
 * @param probabilidad         Probabilidad de cierre (0–100).
 * @param etapa                Etapa actual en el pipeline.
 * @param motivoPerdida        Motivo de pérdida (solo para etapa PERDIDA).
 * @param fechaCierreEsperada  Fecha objetivo de cierre (puede ser null).
 * @param createdAt            Fecha y hora de creación.
 */
public record OportunidadResponse(
        UUID id,
        UUID clienteId,
        String razonSocialCliente,
        UUID vendedorId,
        String nombreVendedor,
        String nombre,
        BigDecimal montoEstimado,
        Integer probabilidad,
        EtapaOportunidad etapa,
        String motivoPerdida,
        LocalDate fechaCierreEsperada,
        LocalDateTime createdAt
) {}
