package com.saas.crm.service.onboarding;

import com.saas.crm.domain.entity.PagoSuscripcionSaaS;
import com.saas.crm.domain.entity.PlanSaaS;
import com.saas.crm.domain.entity.Rol;
import com.saas.crm.domain.entity.Tenant;
import com.saas.crm.domain.entity.Usuario;
import com.saas.crm.dto.onboarding.OnboardingRequest;
import com.saas.crm.dto.onboarding.OnboardingResponse;
import com.saas.crm.dto.payment.PaymentRequest;
import com.saas.crm.dto.payment.PaymentResult;
import com.saas.crm.exception.BadRequestException;
import com.saas.crm.exception.ConflictException;
import com.saas.crm.exception.ResourceNotFoundException;
import com.saas.crm.repository.PagoSuscripcionSaaSRepository;
import com.saas.crm.repository.PlanSaaSRepository;
import com.saas.crm.repository.RolRepository;
import com.saas.crm.repository.TenantRepository;
import com.saas.crm.repository.UsuarioRepository;
import com.saas.crm.service.payment.PaymentGatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio de negocio que orquesta el registro self-service de una nueva
 * empresa inquilina (tenant) en la plataforma SaaS.
 * La transaccion es atomica: cualquier fallo revierte todo el proceso.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private static final String ROL_ADMIN_EMPRESA = "ROLE_ADMIN_EMPRESA";

    private final TenantRepository              tenantRepository;
    private final UsuarioRepository             usuarioRepository;
    private final PlanSaaSRepository            planSaaSRepository;
    private final RolRepository                 rolRepository;
    private final PagoSuscripcionSaaSRepository pagoRepository;
    private final PaymentGatewayService         paymentGatewayService;
    private final PasswordEncoder               passwordEncoder;

    /**
     * Registra una nueva empresa, su administrador inicial y persiste el cobro
     * de la suscripcion en una unica transaccion atomica.
     */
    @Transactional
    public OnboardingResponse registrarEmpresaYAdmin(OnboardingRequest request) {
        log.info("[Onboarding] Inicio registro | subdominio={} email={}", request.subdominio(), request.adminEmail());

        // 1. Unicidad del subdominio
        String subdominioNormalizado = request.subdominio().toLowerCase().trim();
        if (tenantRepository.existsBySubdominio(subdominioNormalizado)) {
            throw new ConflictException("El subdominio ya se encuentra en uso: " + subdominioNormalizado);
        }

        // 2. Unicidad global del email
        String emailNormalizado = request.adminEmail().toLowerCase().trim();
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new ConflictException("El correo electronico ya esta registrado: " + emailNormalizado);
        }

        // 3. Cargar plan SaaS activo
        PlanSaaS plan = planSaaSRepository.findById(request.planSaaSId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Plan SaaS no encontrado con ID: " + request.planSaaSId()));
        if (!Boolean.TRUE.equals(plan.getActivo())) {
            throw new BadRequestException("El plan seleccionado no esta disponible actualmente.");
        }

        // 4. Procesar pago (Strategy Pattern)
        PaymentRequest paymentRequest = new PaymentRequest(
                request.metodoPago(),
                plan.getPrecioMensual(),
                request.numeroTarjeta(),
                request.titular(),
                request.expiracion(),
                request.cvv(),
                null
        );
        PaymentResult paymentResult = paymentGatewayService.procesarPagoSuscripcion(paymentRequest);
        if (!paymentResult.exitoso()) {
            throw new BadRequestException("El pago no pudo ser procesado: " + paymentResult.mensaje());
        }

        // 5. Crear y persistir Tenant
        LocalDateTime ahora = LocalDateTime.now();
        Tenant tenant = new Tenant();
        tenant.setNombreComercial(request.nombreComercial());
        tenant.setSubdominio(subdominioNormalizado);
        tenant.setPlanSaaS(plan);
        tenant.setEstadoSuscripcion("ACTIVO");
        tenant.setActivo(true);
        tenant.setFechaVencimientoPlan(ahora.plusDays(30));
        tenant.setCreatedAt(ahora);
        tenant = tenantRepository.save(tenant);
        log.info("[Onboarding] Tenant creado | id={}", tenant.getId());

        // 6. Resolver rol ROLE_ADMIN_EMPRESA
        Rol rolAdmin = rolRepository.findByNombre(ROL_ADMIN_EMPRESA)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + ROL_ADMIN_EMPRESA));

        // 7. Crear y persistir Usuario administrador
        Usuario admin = new Usuario();
        admin.setTenant(tenant);
        admin.setRol(rolAdmin);
        admin.setNombreCompleto(request.adminNombreCompleto());
        admin.setEmail(emailNormalizado);
        admin.setPasswordHash(passwordEncoder.encode(request.adminPassword()));
        admin.setTipoUsuario("OPERADOR_INTERNO");
        admin.setActivo(true);
        admin.setCreatedAt(ahora);
        usuarioRepository.save(admin);
        log.info("[Onboarding] Usuario admin creado | email={}", emailNormalizado);

        // 8. Registrar auditoria de pago
        PagoSuscripcionSaaS pago = new PagoSuscripcionSaaS();
        pago.setTenant(tenant);
        pago.setPlanSaaS(plan);
        pago.setMonto(plan.getPrecioMensual());
        pago.setMetodoPago(request.metodoPago().toUpperCase());
        pago.setTransaccionId(paymentResult.transaccionId());
        pago.setEstado("APROBADO");
        pagoRepository.save(pago);
        log.info("[Onboarding] Pago registrado | transaccionId={}", paymentResult.transaccionId());

        // 9. Respuesta de exito
        return new OnboardingResponse(
                tenant.getId(),
                tenant.getNombreComercial(),
                tenant.getSubdominio(),
                emailNormalizado,
                plan.getNombre(),
                tenant.getEstadoSuscripcion(),
                paymentResult.transaccionId(),
                tenant.getFechaVencimientoPlan(),
                "Empresa registrada exitosamente. Bienvenido a la plataforma."
        );
    }
}