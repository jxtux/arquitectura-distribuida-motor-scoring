package com.finanscore.motorscoring.domain.entity;
import com.finanscore.motorscoring.domain.enums.*;
import com.finanscore.motorscoring.domain.exception.SolicitudNoEvaluableException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public final class VersionModelo{
    private final Long id;
    private final String numeroVersion;
    private final LocalDate fechaInicioVigencia,fechaFinVigencia;
    private final EstadoVersionModelo estado;
    private final List<FactorScoring> factores;
    
    public VersionModelo(Long id,String numero,LocalDate inicio,LocalDate fin,EstadoVersionModelo estado,List<FactorScoring> factores){
        this.id=Objects.requireNonNull(id);
        this.numeroVersion=Objects.requireNonNull(numero);
        this.fechaInicioVigencia=Objects.requireNonNull(inicio);
        this.fechaFinVigencia=fin;
        this.estado=Objects.requireNonNull(estado);
        this.factores=List.copyOf(factores);
    }
    
    public boolean estaVigente(LocalDate f){
        return estado==EstadoVersionModelo.ACTIVA&&!f.isBefore(fechaInicioVigencia)&&(fechaFinVigencia==null||!f.isAfter(fechaFinVigencia));
    }
    
    public void validarPesos(){
        BigDecimal total=factores.stream().filter(x->x.estado()==EstadoFactor.ACTIVO).map(x->x.peso().valor()).reduce(BigDecimal.ZERO,BigDecimal::add);
       
        if(total.compareTo(new BigDecimal("100.0000"))!=0)
        	throw new SolicitudNoEvaluableException("Los pesos deben sumar 100%. Total: "+total);
    }
    public Long id(){
        return id;
    }
    public String numeroVersion(){
        return numeroVersion;
    }
    public LocalDate fechaInicioVigencia(){
        return fechaInicioVigencia;
    }
    public LocalDate fechaFinVigencia(){
        return fechaFinVigencia;
    }
    public EstadoVersionModelo estado(){
        return estado;
    }
    public List<FactorScoring> factores(){
        return factores;
    }
}
