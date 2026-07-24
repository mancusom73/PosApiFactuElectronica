package com.pos.factura.entity.dbtpviv;

/**
 * Enum que representa los tipos de comprobante del punto de venta,
 * mapeados a sus códigos internos (campo TIPO_EVENTO de la tabla eventos)
 * y a su descripción AFIP correspondiente.
 *
 * Equivalencia con TipoComprobante de TicketEntity:
 *   FACTURA_A      (código 3) → TicketEntity.TipoComprobante.FACTURA_A
 *   FACTURA_B      (código 4) → TicketEntity.TipoComprobante.FACTURA_B
 *   NOTA_CREDITO_A (código 5) → TicketEntity.TipoComprobante.NOTA_CREDITO_A
 *   NOTA_CREDITO_B (código 6) → TicketEntity.TipoComprobante.NOTA_CREDITO_B
 *
 * Uso desde EventoMapper:
 *   TipoEventoPOS tipo = TipoEventoPOS.fromCodigo(evento.getTipoEvento());
 *   String descripcionAfip = tipo.getDescripcionAfip();
 *
 * Uso para obtener TicketEntity.TipoComprobante equivalente:
 *   TicketEntity.TipoComprobante tipoTicket = TipoEventoPOS.fromCodigo(evento.getTipoEvento())
 *                                                           .getTipoComprobante();
 */
public enum TipoEventoPOS {

    FACTURA_A     (3, "FACTURA A",        "Factura A — Responsable Inscripto"),
    FACTURA_B     (4, "FACTURA B",        "Factura B — Consumidor Final"),
    NOTA_CREDITO_A(5, "NOTA DE CREDITO A","Nota de Crédito A"),
    NOTA_CREDITO_B(6, "NOTA DE CREDITO B","Nota de Crédito B");

    /** Código interno del POS (campo TIPO_EVENTO en la tabla eventos) */
    private final int codigo;

    /** Descripción que espera la API de AFIP en el campo "tipo" del comprobante */
    private final String descripcionAfip;

    /** Descripción legible para logs y UI */
    private final String descripcionLegible;

    TipoEventoPOS(int codigo, String descripcionAfip, String descripcionLegible) {
        this.codigo             = codigo;
        this.descripcionAfip    = descripcionAfip;
        this.descripcionLegible = descripcionLegible;
    }

    public int getCodigo()                  { return codigo; }
    public String getDescripcionAfip()      { return descripcionAfip; }
    public String getDescripcionLegible()   { return descripcionLegible; }

    /**
     * Convierte el código numérico del POS al enum correspondiente.
     *
     * @param codigo Valor del campo TIPO_EVENTO (Byte de la tabla eventos)
     * @return El TipoEventoPOS correspondiente
     * @throws IllegalArgumentException si el código no existe
     */
    public static TipoEventoPOS fromCodigo(Byte codigo) {
        if (codigo == null) {
            throw new IllegalArgumentException("El código de tipo de evento no puede ser nulo");
        }
        for (TipoEventoPOS tipo : values()) {
            if (tipo.codigo == codigo) {
                return tipo;
            }
        }
        throw new IllegalArgumentException(
                "Código de tipo de evento desconocido: " + codigo
                + ". Valores válidos: 3=Factura A, 4=Factura B, "
                + "5=Nota Crédito A, 6=Nota Crédito B");
    }

    /**
     * Igual que fromCodigo() pero devuelve un default en lugar de lanzar excepción.
     * Útil cuando preferís no interrumpir el flujo ante un código inesperado.
     *
     * @param codigo       Valor del campo TIPO_EVENTO
     * @param defaultTipo  Tipo a devolver si el código no se reconoce
     */
    public static TipoEventoPOS fromCodigoODefault(Byte codigo, TipoEventoPOS defaultTipo) {
        if (codigo == null) return defaultTipo;
        for (TipoEventoPOS tipo : values()) {
            if (tipo.codigo == codigo) {
                return tipo;
            }
        }
        return defaultTipo;
    }

    /**
     * Verifica si un código es válido sin lanzar excepción.
     */
    public static boolean esCodigoValido(Byte codigo) {
        if (codigo == null) return false;
        for (TipoEventoPOS tipo : values()) {
            if (tipo.codigo == codigo) return true;
        }
        return false;
    }
}
