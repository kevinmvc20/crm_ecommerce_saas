package com.saas.crm.domain.enums;

/**
 * Estados del ciclo de vida de un Lead (prospecto).
 *
 * <ul>
 *   <li>{@link #NUEVO}          – Lead recién registrado, sin gestión aún.</li>
 *   <li>{@link #CONTACTADO}     – Se ha establecido el primer contacto.</li>
 *   <li>{@link #CALIFICADO}     – Lead evaluado con score y notas; listo para convertir.</li>
 *   <li>{@link #DESCALIFICADO}  – Lead descartado del pipeline.</li>
 *   <li>{@link #CONVERTIDO}     – Lead convertido a {@code Cliente}.</li>
 * </ul>
 */
public enum EstadoLead {
    NUEVO,
    CONTACTADO,
    CALIFICADO,
    DESCALIFICADO,
    CONVERTIDO
}
