package com.pos.factura.entity.dbtpviv;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clave primaria compuesta de la tabla articulo (DBTPVIV).
 * PRIMARY KEY (COD_INTERNO, CODIGO_BARRA)
 */
public class ArticuloId implements Serializable {

    private Integer codInterno;
    private String codigoBarra;

    public ArticuloId() {}

    public ArticuloId(Integer codInterno, String codigoBarra) {
        this.codInterno  = codInterno;
        this.codigoBarra = codigoBarra;
    }

    public Integer getCodInterno()  { return codInterno; }
    public String  getCodigoBarra() { return codigoBarra; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArticuloId)) return false;
        ArticuloId that = (ArticuloId) o;
        return Objects.equals(codInterno, that.codInterno)
                && Objects.equals(codigoBarra, that.codigoBarra);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codInterno, codigoBarra);
    }
}
