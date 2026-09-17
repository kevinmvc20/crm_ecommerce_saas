package com.saas.crm.domain.enums;

/**
 * Tipos de interacción registrables en el CRM.
 *
 * <ul>
 *   <li>{@link #LLAMADA}  – Llamada telefónica.</li>
 *   <li>{@link #REUNION}  – Reunión presencial o virtual.</li>
 *   <li>{@link #CORREO}   – Intercambio de correos electrónicos.</li>
 *   <li>{@link #NOTA}     – Nota interna del ejecutivo.</li>
 * </ul>
 */
public enum TipoInteraccionCRM {
    LLAMADA,
    REUNION,
    CORREO,
    NOTA
}
