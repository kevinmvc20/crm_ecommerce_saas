package com.saas.crm.dto.crm;

import com.saas.crm.domain.enums.TipoInteraccionCRM;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Respuesta inmutable con la representación pública de una {@code InteraccionCRM}.
 *
 * @param id              Identificador único de la interacción.
 * @param tenantId        Tenant al que pertenece.
 * @param leadId          UUID del Lead asociado (puede ser null si es sobre cliente).
 * @param clienteId       UUID del Cliente asociado (puede ser null si es sobre lead).
 * @param ejecutivoId     UUID del usuario ejecutivo que registró la interacción.
 * @param nombreEjecutivo Nombre completo del ejecutivo.
 * @param tipo            Tipo de actividad realizada.
 * @param descripcion     Detalle narrativo de la actividad.
 * @param fechaHora       Marca de tiempo de la interacción.
 */
public record InteraccionResponse(
        UUID id,
        UUID tenantId,
        UUID leadId,
        UUID clienteId,
        UUID ejecutivoId,
        String nombreEjecutivo,
        TipoInteraccionCRM tipo,
        String descripcion,
        LocalDateTime fechaHora
) {}
