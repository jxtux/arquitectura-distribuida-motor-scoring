package com.finanscore.motorscoring.domain.entity;
import com.finanscore.motorscoring.domain.enums.EstadoModelo;
import com.finanscore.motorscoring.domain.exception.ModeloActivoNoEncontradoException;
import java.time.LocalDate;
import java.util.*;

public final class ModeloScoring{
	
    private final Long id;
    
    private final String codigo,nombre;
    
    private final EstadoModelo estado;
    
    private final List<VersionModelo> versiones;
    
    public ModeloScoring(Long id,String codigo,String nombre,EstadoModelo estado,List<VersionModelo> versiones){
        this.id=Objects.requireNonNull(id);
        this.codigo=Objects.requireNonNull(codigo);
        this.nombre=Objects.requireNonNull(nombre);
        this.estado=Objects.requireNonNull(estado);
        this.versiones=List.copyOf(versiones);
    }
    public VersionModelo versionActiva(LocalDate fecha){
        if(estado!=EstadoModelo.ACTIVO)
        	throw new ModeloActivoNoEncontradoException("Modelo inactivo.");
        
        return versiones.stream().filter(v->v.estaVigente(fecha)).findFirst().orElseThrow(()->new ModeloActivoNoEncontradoException("No existe versión activa para "+fecha));
    }
    
    public boolean tieneVersionActiva(LocalDate fecha){
        return versiones.stream().anyMatch(v->v.estaVigente(fecha));
    }
    
    public Long id(){
        return id;
    }
    public String codigo(){
        return codigo;
    }
    public String nombre(){
        return nombre;
    }
    public EstadoModelo estado(){
        return estado;
    }
    public List<VersionModelo> versiones(){
        return versiones;
    }
}
