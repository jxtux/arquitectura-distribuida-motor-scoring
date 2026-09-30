package com.finanscore.motorscoring.domain.entity;
import com.finanscore.motorscoring.domain.enums.EstadoSolicitud;
import com.finanscore.motorscoring.domain.exception.DomainException;
import com.finanscore.motorscoring.domain.valueobject.*;
import java.time.LocalDateTime;
import java.util.Objects;

public final class SolicitudCredito{
	
    private final Long id,idSolicitante,idProducto;
    
    private final Dinero montoSolicitado;
    
    private final int plazoSolicitado;
    
    private final String finalidadCredito,canalOrigen;
    
    private final LocalDateTime fechaRegistro;
    
    private final IdentificadorExterno identificadorExterno;
    
    private EstadoSolicitud estado;
    
    private SolicitudCredito(Long id,Long sol,Long prod,Dinero monto,int plazo,String fin,String canal,LocalDateTime fecha,IdentificadorExterno ext,EstadoSolicitud estado){
        this.id=id;
        this.idSolicitante=Objects.requireNonNull(sol);
        this.idProducto=Objects.requireNonNull(prod);
        this.montoSolicitado=Objects.requireNonNull(monto);
        
        if(monto.monto().signum()<=0)
        	throw new DomainException("El monto debe ser mayor que cero.");
        
        if(plazo<=0)
        	throw new DomainException("El plazo debe ser mayor que cero.");
        
        if(fin==null||fin.isBlank())
        	throw new DomainException("La finalidad es obligatoria.");
        
        if(canal==null||canal.isBlank())
        	throw new DomainException("El canal es obligatorio.");
        
        this.plazoSolicitado=plazo;
        this.finalidadCredito=fin.trim();
        this.canalOrigen=canal.trim();
        this.fechaRegistro=Objects.requireNonNull(fecha);
        this.identificadorExterno=Objects.requireNonNull(ext);
        this.estado=Objects.requireNonNull(estado);
    }
    public static SolicitudCredito registrar(Long s,Long p,Dinero m,int pl,String f,String c,IdentificadorExterno e,LocalDateTime fecha){
        return new SolicitudCredito(null,s,p,m,pl,f,c,fecha,e,EstadoSolicitud.REGISTRADA);
    }
    public static SolicitudCredito reconstituir(Long id,Long s,Long p,Dinero m,int pl,String f,String c,LocalDateTime fecha,IdentificadorExterno e,EstadoSolicitud estado){
        return new SolicitudCredito(id,s,p,m,pl,f,c,fecha,e,estado);
    }
    public void marcarEvaluada(){
        if(estado!=EstadoSolicitud.REGISTRADA)throw new DomainException("Solo una solicitud registrada puede evaluarse.");
        estado=EstadoSolicitud.EVALUADA;
    }
    public boolean estaRegistrada(){
        return estado==EstadoSolicitud.REGISTRADA;
    }
    public boolean estaEvaluable(){
        return estaRegistrada();
    }
    public Long id(){
        return id;
    }
    public Long idSolicitante(){
        return idSolicitante;
    }
    public Long idProducto(){
        return idProducto;
    }
    public Dinero montoSolicitado(){
        return montoSolicitado;
    }
    public int plazoSolicitado(){
        return plazoSolicitado;
    }
    public String finalidadCredito(){
        return finalidadCredito;
    }
    public String canalOrigen(){
        return canalOrigen;
    }
    public LocalDateTime fechaRegistro(){
        return fechaRegistro;
    }
    public IdentificadorExterno identificadorExterno(){
        return identificadorExterno;
    }
    public EstadoSolicitud estado(){
        return estado;
    }
}
