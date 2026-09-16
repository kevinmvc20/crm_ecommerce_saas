package com.saas.crm.repository;

import com.saas.crm.domain.entity.PlanSaaS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad {@link PlanSaaS}.
 */
@Repository
public interface PlanSaaSRepository extends JpaRepository<PlanSaaS, Integer> {

    /**
     * Retorna únicamente los planes marcados como activos.
     *
     * @return lista de planes activos
     */
    List<PlanSaaS> findByActivoTrue();
}
