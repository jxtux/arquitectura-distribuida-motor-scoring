package com.finanscore.motorscoring.domain.entity;
import com.finanscore.motorscoring.domain.enums.EstadoProducto;
import com.finanscore.motorscoring.domain.exception.SolicitudNoEvaluableException;
import com.finanscore.motorscoring.domain.valueobject.Dinero;
import java.util.Objects;
public final class ProductoCrediticio{
	
    private final Long id,idModeloScoring;
    
    private final String codigo,nombre;
    
    private final Dinero montoMinimo,montoMaximo;
    
    private final int plazoMinimo,plazoMaximo;
    
    private final EstadoProducto estado;
    
    public ProductoCrediticio(Long id,String codigo,String nombre,Dinero min,Dinero max,int pmin,int pmax,EstadoProducto estado,Long modelo){
    	
        this.id=Objects.requireNonNull(id);        
        this.codigo=Objects.requireNonNull(codigo);        
        this.nombre=Objects.requireNonNull(nombre);        
        this.montoMinimo=Objects.requireNonNull(min);        
        this.montoMaximo=Objects.requireNonNull(max);        
        if(min.moneda()!=max.moneda()||min.monto().compareTo(max.monto())>0)
        	throw new IllegalArgumentException("Rango monetario inválido.");
        
        if(pmin<=0||pmax<pmin)
        	throw new IllegalArgumentException("Rango de plazo inválido.");
        
        this.plazoMinimo=pmin;        
        this.plazoMaximo=pmax;        
        this.estado=Objects.requireNonNull(estado);        
        this.idModeloScoring=Objects.requireNonNull(modelo);
    }
    public void validarSolicitud(Dinero monto,int plazo){
    	
        if(estado!=EstadoProducto.ACTIVO)
        	throw new SolicitudNoEvaluableException("El producto está inactivo.");
        
        if(monto.moneda()!=montoMinimo.moneda())
        	throw new SolicitudNoEvaluableException("La moneda no corresponde al producto.");
        
        if(monto.monto().compareTo(montoMinimo.monto())<0||monto.monto().compareTo(montoMaximo.monto())>0)
        	throw new SolicitudNoEvaluableException("El monto está fuera de los límites del producto.");
        
        if(plazo<plazoMinimo||plazo>plazoMaximo)
        	throw new SolicitudNoEvaluableException("El plazo está fuera de los límites del producto.");
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
    public Dinero montoMinimo(){
        return montoMinimo;
    }
    public Dinero montoMaximo(){
        return montoMaximo;
    }
    public int plazoMinimo(){
        return plazoMinimo;
    }
    public int plazoMaximo(){
        return plazoMaximo;
    }
    public EstadoProducto estado(){
        return estado;
    }
    public Long idModeloScoring(){
        return idModeloScoring;
    }
}
