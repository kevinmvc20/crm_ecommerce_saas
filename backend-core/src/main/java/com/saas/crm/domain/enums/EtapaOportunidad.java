package com.saas.crm.domain.enums;

/**
 * Etapas del pipeline de ventas para una {@code Oportunidad}.
 *
 * <ul>
 *   <li>{@link #CALIFICACION}  – Etapa inicial de validación del interés.</li>
 *   <li>{@link #PROPUESTA}     – Se ha enviado una propuesta formal al cliente.</li>
 *   <li>{@link #NEGOCIACION}   – Proceso de negociación activo.</li>
 *   <li>{@link #GANADA}        – Oportunidad cerrada con éxito.</li>
 *   <li>{@link #PERDIDA}       – Oportunidad cerrada sin éxito.</li>
 * </ul>
 */
public enum EtapaOportunidad {
    CALIFICACION,
    PROPUESTA,
    NEGOCIACION,
    GANADA,
    PERDIDA
}
