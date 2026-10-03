package com.example.demo.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Resultado de la consulta JPQL "ranking de espacios mejor calificados".
 * JPQL lo construye directamente con "select new ...EspacioRankingDTO(...)".
 * El orden y el tipo de los campos debe coincidir con el select:
 * avg() devuelve Double y count() devuelve Long.
 */
@Getter
@AllArgsConstructor
public class EspacioRankingDTO {
    private Long espacioId;
    private String nombre;
    private String tipoDeporte;
    private Double promedio;
    private Long totalCalificaciones;
}
