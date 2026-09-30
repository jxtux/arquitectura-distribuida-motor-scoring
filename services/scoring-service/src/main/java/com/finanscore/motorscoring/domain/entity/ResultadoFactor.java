package com.finanscore.motorscoring.domain.entity;
import com.finanscore.motorscoring.domain.enums.ResultadoScoring;
import java.math.BigDecimal;
import java.util.Objects;

public record ResultadoFactor(Long idFactor,String codigoFactor,BigDecimal valorEvaluado,BigDecimal pesoAplicado,int puntajeBase,int puntajeObtenido,String reglaAplicada,String observacion,boolean reglaExcluyente,ResultadoScoring resultadoExcluyente){
    public ResultadoFactor{
        Objects.requireNonNull(idFactor);
        Objects.requireNonNull(codigoFactor);
        Objects.requireNonNull(valorEvaluado);
        Objects.requireNonNull(pesoAplicado);
        Objects.requireNonNull(reglaAplicada);
        observacion=observacion==null?"":observacion;
    }
}
