-- =====================================================================
-- V2__crear_tablas_factura_electronica.sql
--
-- Crea las 3 tablas donde se persiste exactamente el JSON enviado
-- a la API de factura electronica de AFIP/ARCA, dividido en:
--
--   factura_cabecera     -> datos del comprobante + datos del cliente
--   factura_detalle      -> lineas de productos/servicios
--   factura_medios_pago  -> formas de pago
--
-- Compatible con MySQL 5.1+
-- Nota: MySQL 5.1 no soporta utf8mb4 ni COMMENT en columnas dentro de
-- definiciones complejas; se usan VARCHAR y comentarios SQL estandar.
-- =====================================================================


-- -----------------------------------------------------
-- Tabla: factura_cabecera
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS factura_cabecera (

    id                      BIGINT          NOT NULL AUTO_INCREMENT,

    -- Datos del comprobante (del JSON enviado a AFIP)
    tipo_comprobante        VARCHAR(30)     NOT NULL,
    punto_venta             VARCHAR(10)     NOT NULL,
    numero_comprobante      VARCHAR(20),
    fecha                   DATE            NOT NULL,
    fecha_vencimiento       DATE,
    moneda                  VARCHAR(5)               DEFAULT 'PES',
    cotizacion              DECIMAL(10,4)            DEFAULT 1.0000,
    operacion               VARCHAR(2)               DEFAULT 'V',
    rubro                   VARCHAR(100),

    -- Totales calculados antes del envio
    neto_gravado            DECIMAL(12,2)            DEFAULT 0.00,
    iva_total               DECIMAL(12,2)            DEFAULT 0.00,
    exentos                 DECIMAL(12,2)            DEFAULT 0.00,
    bonificacion            DECIMAL(12,2)            DEFAULT 0.00,
    total                   DECIMAL(12,2)   NOT NULL DEFAULT 0.00,

    -- Datos del cliente (snapshot al momento de la facturacion)
    cliente_documento_tipo  VARCHAR(20),
    cliente_documento_nro   VARCHAR(20),
    cliente_razon_social    VARCHAR(200),
    cliente_email           VARCHAR(150),
    cliente_domicilio       VARCHAR(250),
    cliente_provincia       VARCHAR(5),
    cliente_condicion_iva   VARCHAR(5),
    cliente_envia_por_mail  CHAR(1)                  DEFAULT 'N',

    -- Estado del proceso con AFIP
    -- Valores: PENDIENTE, ENVIADO, APROBADO, RECHAZADO, ERROR
    estado                  VARCHAR(15)     NOT NULL DEFAULT 'PENDIENTE',

    -- Respuesta de AFIP (se completa despues de llamar a la API)
    cae                     VARCHAR(20),
    cae_vencimiento         DATE,
    pdf_url                 VARCHAR(500),
    pdf_base64              MEDIUMTEXT,
    afip_qr                 VARCHAR(500),
    afip_codigo_barras      VARCHAR(100),
    error_descripcion       VARCHAR(1000),

    -- Trazabilidad
    numero_ticket_pos       VARCHAR(20),
    fecha_envio_api         DATETIME,
    fecha_respuesta_api     DATETIME,
    fecha_alta              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion      DATETIME,

    PRIMARY KEY (id),
    INDEX idx_fc_fecha              (fecha),
    INDEX idx_fc_tipo               (tipo_comprobante),
    INDEX idx_fc_doc_cliente        (cliente_documento_nro),
    INDEX idx_fc_estado             (estado),
    INDEX idx_fc_cae                (cae),
    INDEX idx_fc_ticket_pos         (numero_ticket_pos)

) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- -----------------------------------------------------
-- Tabla: factura_detalle
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS factura_detalle (

    id                          BIGINT          NOT NULL AUTO_INCREMENT,
    cabecera_id                 BIGINT          NOT NULL,

    -- Orden del item dentro del comprobante
    orden                       INT             NOT NULL DEFAULT 1,

    -- Campos del objeto "detalle" del JSON
    cantidad                    DECIMAL(10,2)   NOT NULL,
    afecta_stock                CHAR(1)                  DEFAULT 'S',
    bonificacion_porcentaje     DECIMAL(5,2)             DEFAULT 0.00,
    leyenda                     VARCHAR(200),

    -- Campos del objeto "producto" dentro de cada item
    producto_codigo             VARCHAR(50),
    producto_descripcion        VARCHAR(250)    NOT NULL,
    precio_unitario_sin_iva     DECIMAL(12,2)   NOT NULL,
    alicuota_iva                DECIMAL(5,1)    NOT NULL,
    unidad_medida               VARCHAR(10)              DEFAULT '7',
    lista_precios               VARCHAR(50),
    impuestos_internos_alicuota DECIMAL(5,2)             DEFAULT 0.00,

    -- Totales calculados de la linea (para auditoria y consultas)
    neto_linea                  DECIMAL(12,2),
    iva_linea                   DECIMAL(12,2),
    total_linea                 DECIMAL(12,2),

    PRIMARY KEY (id),
    INDEX idx_fd_cabecera       (cabecera_id),
    INDEX idx_fd_codigo         (producto_codigo),

    CONSTRAINT fk_fd_cabecera
        FOREIGN KEY (cabecera_id) REFERENCES factura_cabecera (id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- -----------------------------------------------------
-- Tabla: factura_medios_pago
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS factura_medios_pago (

    id              BIGINT          NOT NULL AUTO_INCREMENT,
    cabecera_id     BIGINT          NOT NULL,

    -- Campos del array "formas_pago" del objeto "pagos" del JSON
    -- Ej: "Efectivo", "VISA DB", "MercadoPago", "Transferencia"
    descripcion     VARCHAR(100)    NOT NULL,
    importe         DECIMAL(12,2)   NOT NULL,

    PRIMARY KEY (id),
    INDEX idx_fmp_cabecera (cabecera_id),

    CONSTRAINT fk_fmp_cabecera
        FOREIGN KEY (cabecera_id) REFERENCES factura_cabecera (id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- =====================================================================
-- Vistas utiles para consultas frecuentes
-- =====================================================================

-- Vista: resumen de facturas con totales de medios de pago
CREATE OR REPLACE VIEW v_facturas_resumen AS
SELECT
    fc.id,
    fc.fecha,
    fc.tipo_comprobante,
    fc.punto_venta,
    fc.numero_comprobante,
    fc.estado,
    fc.cliente_documento_nro,
    fc.cliente_razon_social,
    fc.cliente_condicion_iva,
    fc.neto_gravado,
    fc.iva_total,
    fc.exentos,
    fc.bonificacion,
    fc.total,
    fc.cae,
    fc.cae_vencimiento,
    fc.numero_ticket_pos,
    fc.fecha_envio_api,
    fc.fecha_respuesta_api,
    COUNT(fd.id)  AS cantidad_items,
    COUNT(fmp.id) AS cantidad_medios_pago
FROM factura_cabecera fc
LEFT JOIN factura_detalle    fd  ON fd.cabecera_id  = fc.id
LEFT JOIN factura_medios_pago fmp ON fmp.cabecera_id = fc.id
GROUP BY
    fc.id, fc.fecha, fc.tipo_comprobante, fc.punto_venta,
    fc.numero_comprobante, fc.estado, fc.cliente_documento_nro,
    fc.cliente_razon_social, fc.cliente_condicion_iva,
    fc.neto_gravado, fc.iva_total, fc.exentos, fc.bonificacion,
    fc.total, fc.cae, fc.cae_vencimiento, fc.numero_ticket_pos,
    fc.fecha_envio_api, fc.fecha_respuesta_api;


-- Vista: detalle completo por factura (una fila por item)
CREATE OR REPLACE VIEW v_facturas_detalle AS
SELECT
    fc.id                       AS factura_id,
    fc.fecha,
    fc.tipo_comprobante,
    fc.numero_comprobante,
    fc.cliente_razon_social,
    fc.cliente_documento_nro,
    fc.estado,
    fc.cae,
    fd.orden,
    fd.producto_codigo,
    fd.producto_descripcion,
    fd.cantidad,
    fd.precio_unitario_sin_iva,
    fd.alicuota_iva,
    fd.bonificacion_porcentaje,
    fd.neto_linea,
    fd.iva_linea,
    fd.total_linea
FROM factura_cabecera  fc
JOIN factura_detalle   fd ON fd.cabecera_id = fc.id
ORDER BY fc.fecha DESC, fc.id DESC, fd.orden ASC;
