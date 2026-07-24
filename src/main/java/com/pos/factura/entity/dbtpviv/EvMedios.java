package com.pos.factura.entity.dbtpviv;

import javax.persistence.*;

/**
 * Entidad JPA que representa la tabla ev_medios de DBTPVIV.
 *
 * Contiene los medios de pago de cada evento/ticket.
 * Motor MyISAM — no soporta FK ni transacciones, solo lectura/escritura simple.
 *
 * Mapeo de tipos MySQL → Java:
 *   INT(11)     → Integer
 *   SMALLINT(6) → Short
 *   TINYINT(4)  → Byte
 *   DOUBLE      → Double
 *   VARCHAR     → String
 *
 * PK compuesta: (CAJA_Z, ID_EVENTO, POSICION)
 */
@Entity
@Table(name = "ev_medios")
public class EvMedios {

    @EmbeddedId
    private EvMediosId id;

    @Column(name = "MODO_PAGO")
    private Short modoPago;

    @Column(name = "IMPORTE")
    private Double importe;

    @Column(name = "NUMERO_ID")
    private Integer numeroId;

    @Column(name = "TARJETA")
    private Byte tarjeta;

    @Column(name = "AUTORIZACION")
    private Integer autorizacion;

    @Column(name = "BANCO")
    private Short banco;

    @Column(name = "FECHA_VTO")
    private Short fechaVto;

    @Column(name = "NRO_TARJETA", length = 20)
    private String nroTarjeta;

    @Column(name = "AUTO_TARJETA", length = 10)
    private String autoTarjeta;

    @Column(name = "NRO_CUPON1")
    private Integer nroCupon1;

    @Column(name = "CUOTAS")
    private Byte cuotas;

    @Column(name = "NRO_COMERCIO", length = 15)
    private String nroComercio;

    @Column(name = "VUELTO_EFECTIVO")
    private Double vueltoEfectivo;

    @Column(name = "VARIACION")
    private Double variacion;

    @Column(name = "NRO_CUPON_INT1")
    private Integer nroCuponInt1;

    @Column(name = "VUELTO_PROPIO")
    private Double vueltoPropio;

    @Column(name = "MONEDA")
    private Byte moneda;

    @Column(name = "ID_REGISTRO")
    private Byte idRegistro;

    @Column(name = "CAJA")
    private Short caja;

    @Column(name = "NRO_Z")
    private Integer nroZ;

    @Column(name = "COD_SUB_MEDIO")
    private Byte codSubMedio;

    @Column(name = "VUELTO_MEDIO")
    private Double vueltoMedio;

    @Column(name = "COD_MEDIO_VUELTO")
    private Byte codMedioVuelto;

    @Column(name = "COD_SUBMEDIO_VUELTO")
    private Byte codSubmedioVuelto;

    @Column(name = "COD_SUCURSAL")
    private Short codSucursal;

    @Column(name = "FECHA_TICKET")
    private Short fechaTicket;

    @Column(name = "DATAFONO")
    private Integer datafono;

    @Column(name = "CUIT", length = 20)
    private String cuit;

    @Column(name = "RECARGO_TARJETA")
    private Double recargoTarjeta;

    // ── Getters y Setters ────────────────────────────────────────────────────

    public EvMediosId getId()               { return id; }
    public void setId(EvMediosId id)        { this.id = id; }

    public Short getModoPago()              { return modoPago; }
    public void setModoPago(Short v)        { this.modoPago = v; }

    public Double getImporte()              { return importe; }
    public void setImporte(Double v)        { this.importe = v; }

    public Integer getNumeroId()            { return numeroId; }
    public void setNumeroId(Integer v)      { this.numeroId = v; }

    public Byte getTarjeta()                { return tarjeta; }
    public void setTarjeta(Byte v)          { this.tarjeta = v; }

    public Integer getAutorizacion()        { return autorizacion; }
    public void setAutorizacion(Integer v)  { this.autorizacion = v; }

    public Short getBanco()                 { return banco; }
    public void setBanco(Short v)           { this.banco = v; }

    public Short getFechaVto()              { return fechaVto; }
    public void setFechaVto(Short v)        { this.fechaVto = v; }

    public String getNroTarjeta()           { return nroTarjeta; }
    public void setNroTarjeta(String v)     { this.nroTarjeta = v; }

    public String getAutoTarjeta()          { return autoTarjeta; }
    public void setAutoTarjeta(String v)    { this.autoTarjeta = v; }

    public Integer getNroCupon1()           { return nroCupon1; }
    public void setNroCupon1(Integer v)     { this.nroCupon1 = v; }

    public Byte getCuotas()                 { return cuotas; }
    public void setCuotas(Byte v)           { this.cuotas = v; }

    public String getNroComercio()          { return nroComercio; }
    public void setNroComercio(String v)    { this.nroComercio = v; }

    public Double getVueltoEfectivo()       { return vueltoEfectivo; }
    public void setVueltoEfectivo(Double v) { this.vueltoEfectivo = v; }

    public Double getVariacion()            { return variacion; }
    public void setVariacion(Double v)      { this.variacion = v; }

    public Integer getNroCuponInt1()        { return nroCuponInt1; }
    public void setNroCuponInt1(Integer v)  { this.nroCuponInt1 = v; }

    public Double getVueltoPropio()         { return vueltoPropio; }
    public void setVueltoPropio(Double v)   { this.vueltoPropio = v; }

    public Byte getMoneda()                 { return moneda; }
    public void setMoneda(Byte v)           { this.moneda = v; }

    public Byte getIdRegistro()             { return idRegistro; }
    public void setIdRegistro(Byte v)       { this.idRegistro = v; }

    public Short getCaja()                  { return caja; }
    public void setCaja(Short v)            { this.caja = v; }

    public Integer getNroZ()                { return nroZ; }
    public void setNroZ(Integer v)          { this.nroZ = v; }

    public Byte getCodSubMedio()            { return codSubMedio; }
    public void setCodSubMedio(Byte v)      { this.codSubMedio = v; }

    public Double getVueltoMedio()          { return vueltoMedio; }
    public void setVueltoMedio(Double v)    { this.vueltoMedio = v; }

    public Byte getCodMedioVuelto()         { return codMedioVuelto; }
    public void setCodMedioVuelto(Byte v)   { this.codMedioVuelto = v; }

    public Byte getCodSubmedioVuelto()          { return codSubmedioVuelto; }
    public void setCodSubmedioVuelto(Byte v)    { this.codSubmedioVuelto = v; }

    public Short getCodSucursal()           { return codSucursal; }
    public void setCodSucursal(Short v)     { this.codSucursal = v; }

    public Short getFechaTicket()           { return fechaTicket; }
    public void setFechaTicket(Short v)     { this.fechaTicket = v; }

    public Integer getDatafono()            { return datafono; }
    public void setDatafono(Integer v)      { this.datafono = v; }

    public String getCuit()                 { return cuit; }
    public void setCuit(String v)           { this.cuit = v; }

    public Double getRecargoTarjeta()       { return recargoTarjeta; }
    public void setRecargoTarjeta(Double v) { this.recargoTarjeta = v; }
}
