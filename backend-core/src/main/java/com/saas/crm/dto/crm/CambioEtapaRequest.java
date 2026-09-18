package com.saas.crm.dto.crm;

import com.saas.crm.domain.enums.EtapaOportunidad;
import jakarta.validation.constraints.NotNull;

/**
 * Payload para cambiar la etapa activa de una Oportunidad.
 * No acepta GANADA ni PERDIDA; use los endpoints especializados para esos casos.
 *
 * @param etapa nueva etapa del pipeline (requerida).
 */
public record CambioEtapaRequest(

        @NotNull(message = "La etapa es obligatoria.")
        EtapaOportunidad etapa
) {}
