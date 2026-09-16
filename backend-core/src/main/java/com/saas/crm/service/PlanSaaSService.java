package com.saas.crm.service;

import com.saas.crm.dto.saas.PlanSaaSDTO;
import com.saas.crm.repository.PlanSaaSRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de negocio para la gestión de planes SaaS.
 */
@Service
@RequiredArgsConstructor
public class PlanSaaSService {

    private final PlanSaaSRepository planSaaSRepository;

    /**
     * Retorna todos los planes marcados como activos mapeados a {@link PlanSaaSDTO}.
     *
     * @return lista de planes activos
     */
    @Transactional(readOnly = true)
    public List<PlanSaaSDTO> listarPlanesActivos() {
        return planSaaSRepository.findByActivoTrue()
                .stream()
                .map(plan -> new PlanSaaSDTO(
                        plan.getId(),
                        plan.getNombre(),
                        plan.getPrecioMensual(),
                        plan.getLimiteUsuarios(),
                        plan.getLimiteProductos(),
                        plan.getActivo()
                ))
                .toList();
    }
}
