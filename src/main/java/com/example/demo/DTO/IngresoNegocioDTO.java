package com.example.demo.DTO;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Resultado de la consulta JPQL "ingresos aprobados por negocio".
 * sum() sobre un BigDecimal devuelve BigDecimal; count() devuelve Long.
 */
@Getter
@AllArgsConstructor
public class IngresoNegocioDTO {
    private String negocio;
    private BigDecimal total;
    private Long pagos;
}
