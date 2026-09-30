package com.finanscore.motorscoring.domain.entity;
import com.finanscore.motorscoring.domain.enums.*;
import java.math.BigDecimal;
import java.util.Objects;

public final class ReglaEvaluacion{
    private final Long id;
    private final String codigo,descripcion;
    private final BigDecimal valorMinimo,valorMaximo;
    private final int puntaje;
    private final boolean excluyente;
    private final ResultadoScoring resultadoExcluyente;
    private final EstadoRegla estado;
    
    public ReglaEvaluacion(Long id,String codigo,String descripcion,BigDecimal min,BigDecimal max,int puntaje,boolean excluyente,ResultadoScoring resultado,EstadoRegla estado){
    	
        this.id=Objects.requireNonNull(id);
        this.codigo=Objects.requireNonNull(codigo);
        this.descripcion=Objects.requireNonNull(descripcion);
        this.valorMinimo=Objects.requireNonNull(min);
        this.valorMaximo=Objects.requireNonNull(max);
        
        if(max.compareTo(min)<0)
        	throw new IllegalArgumentException("Rango inválido.");
        
        if(puntaje<0||puntaje>100)
        	throw new IllegalArgumentException("Puntaje de regla inválido.");
        
        if(excluyente&&resultado==null)
        	throw new IllegalArgumentException("La regla excluyente requiere resultado.");
        
        this.puntaje=puntaje;
        this.excluyente=excluyente;
        this.resultadoExcluyente=resultado;
        this.estado=Objects.requireNonNull(estado);
    }
    
    public boolean aplica(BigDecimal v){
        return estado==EstadoRegla.ACTIVA&&v.compareTo(valorMinimo)>=0&&v.compareTo(valorMaximo)<=0;
    }
    
    public Long id(){
        return id;
    }
    public String codigo(){
        return codigo;
    }
    public String descripcion(){
        return descripcion;
    }
    public BigDecimal valorMinimo(){
        return valorMinimo;
    }
    public BigDecimal valorMaximo(){
        return valorMaximo;
    }
    public int puntaje(){
        return puntaje;
    }
    public boolean excluyente(){
        return excluyente;
    }
    public ResultadoScoring resultadoExcluyente(){
        return resultadoExcluyente;
    }
    public EstadoRegla estado(){
        return estado;
    }
}
