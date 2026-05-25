-- =====================================================================
-- V1__crear_esquema_pos.sql
-- Esquema inicial del POS para factura electrónica Argentina
-- Flyway ejecuta este script automáticamente al iniciar la aplicación
-- =====================================================================

-- -------------------------------------------------
-- Tabla: clientes
-- -------------------------------------------------
CREATE TABLE IF NOT EXISTS clientes (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    documento_tipo      VARCHAR(20)     NOT NULL COMMENT 'DNI, CUIT, CUIL, PASAPORTE',
    documento_nro       VARCHAR(20)     NOT NULL,
    razon_social        VARCHAR(200)    NOT NULL,
    email               VARCHAR(150),
    domicilio           VARCHAR(250),
    provincia           VARCHAR(5)               COMMENT 'Código AFIP: 1=CABA, 13=Córdoba...',
    condicion_iva       VARCHAR(5)      NOT NULL  COMMENT 'CF, RI, MO',
    envia_por_mail      CHAR(1)                  DEFAULT 'N',
    activo              TINYINT(1)      NOT NULL  DEFAULT 1,
    fecha_alta          DATETIME        NOT NULL  DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion  DATETIME,

    PRIMARY KEY (id),
    UNIQUE KEY uq_clientes_documento (documento_nro),
    INDEX idx_clientes_condicion_iva (condicion_iva)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- -------------------------------------------------
-- Tabla: productos
-- -------------------------------------------------
CREATE TABLE IF NOT EXISTS productos (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    codigo                  VARCHAR(50)     NOT NULL,
    descripcion             VARCHAR(250)    NOT NULL,
    precio_unitario_sin_iva DECIMAL(12,2)   NOT NULL COMMENT 'Precio neto sin IVA',
    alicuota_iva            DECIMAL(5,1)    NOT NULL COMMENT '0, 10.5 o 21',
    unidad_medida           VARCHAR(10)              DEFAULT '7' COMMENT 'Código AFIP: 7=unidades',
    lista_precios           VARCHAR(50)              DEFAULT 'LISTA GENERAL',
    stock_actual            DECIMAL(10,2)            DEFAULT 0.00,
    activo                  TINYINT(1)      NOT NULL DEFAULT 1,
    fecha_alta              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion      DATETIME,

    PRIMARY KEY (id),
    UNIQUE KEY uq_productos_codigo (codigo),
    INDEX idx_productos_activo (activo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- -------------------------------------------------
-- Tabla: tickets (cabecera)
-- -------------------------------------------------
CREATE TABLE IF NOT EXISTS tickets (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    numero_ticket       VARCHAR(20)     NOT NULL COMMENT 'Número correlativo generado por el POS',
    fecha               DATE            NOT NULL,
    fecha_vencimiento   DATE                     COMMENT 'Fecha de vencimiento del pago',
    cliente_id          BIGINT          NOT NULL,
    tipo_comprobante    VARCHAR(30)     NOT NULL  COMMENT 'FACTURA_A, FACTURA_B, FACTURA_C, etc.',
    estado              VARCHAR(15)     NOT NULL  DEFAULT 'PENDIENTE'
                            COMMENT 'PENDIENTE, FACTURADO, ERROR, ANULADO',
    moneda              VARCHAR(5)               DEFAULT 'PES',
    cotizacion          DECIMAL(10,4)            DEFAULT 1.0000,

    -- Totales (calculados por el middleware antes de enviar a AFIP)
    neto_gravado        DECIMAL(12,2)            DEFAULT 0.00,
    iva_total           DECIMAL(12,2)            DEFAULT 0.00,
    exentos             DECIMAL(12,2)            DEFAULT 0.00,
    bonificacion        DECIMAL(12,2)            DEFAULT 0.00,
    total               DECIMAL(12,2)   NOT NULL DEFAULT 0.00,

    -- Resultado de la facturación (se completa tras respuesta de AFIP)
    cae                 VARCHAR(20)              COMMENT 'Código de Autorización Electrónico AFIP',
    cae_vencimiento     DATE,
    comprobante_nro     VARCHAR(30),
    afip_qr             VARCHAR(500),
    pdf_url             VARCHAR(500),
    fecha_facturacion   DATETIME,
    error_descripcion   VARCHAR(1000),

    fecha_alta          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion  DATETIME,

    PRIMARY KEY (id),
    UNIQUE KEY uq_tickets_numero (numero_ticket),
    INDEX idx_tickets_fecha (fecha),
    INDEX idx_tickets_cliente (cliente_id),
    INDEX idx_tickets_estado (estado),
    CONSTRAINT fk_tickets_cliente
        FOREIGN KEY (cliente_id) REFERENCES clientes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- -------------------------------------------------
-- Tabla: ticket_items (líneas del ticket)
-- -------------------------------------------------
CREATE TABLE IF NOT EXISTS ticket_items (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    ticket_id               BIGINT          NOT NULL,
    producto_id             BIGINT          NOT NULL,
    cantidad                DECIMAL(10,2)   NOT NULL,
    precio_unitario_sin_iva DECIMAL(12,2)   NOT NULL COMMENT 'Precio histórico al momento de la venta',
    alicuota_iva            DECIMAL(5,1)    NOT NULL,
    bonificacion_porcentaje DECIMAL(5,2)             DEFAULT 0.00,
    leyenda                 VARCHAR(200),
    afecta_stock            CHAR(1)                  DEFAULT 'S',
    orden                   INT                      COMMENT 'Orden de impresión en el comprobante',

    PRIMARY KEY (id),
    INDEX idx_ticket_items_ticket (ticket_id),
    INDEX idx_ticket_items_producto (producto_id),
    CONSTRAINT fk_ticket_items_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE CASCADE,
    CONSTRAINT fk_ticket_items_producto
        FOREIGN KEY (producto_id) REFERENCES productos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- -------------------------------------------------
-- Tabla: ticket_pagos (formas de pago del ticket)
-- -------------------------------------------------
CREATE TABLE IF NOT EXISTS ticket_pagos (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    ticket_id   BIGINT          NOT NULL,
    descripcion VARCHAR(100)    NOT NULL COMMENT 'Efectivo, VISA DB, MercadoPago, etc.',
    importe     DECIMAL(12,2)   NOT NULL,

    PRIMARY KEY (id),
    INDEX idx_ticket_pagos_ticket (ticket_id),
    CONSTRAINT fk_ticket_pagos_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- -------------------------------------------------
-- Datos de ejemplo para desarrollo/testing
-- -------------------------------------------------
INSERT INTO clientes (documento_tipo, documento_nro, razon_social, email, domicilio, provincia, condicion_iva, envia_por_mail)
VALUES
    ('DNI',  '30111222',     'Juan Pérez',          'juan@email.com',        'Av. Colón 123, Córdoba',   '13', 'CF', 'S'),
    ('CUIT', '30712345678',  'Empresa Cliente S.A.', 'compras@empresa.com',   'Av. Libertador 500, CABA', '1',  'RI', 'S'),
    ('DNI',  '27654321',     'María González',       'maria@email.com',       'Bv. San Juan 456, Córdoba','13', 'CF', 'N');

INSERT INTO productos (codigo, descripcion, precio_unitario_sin_iva, alicuota_iva, unidad_medida, lista_precios, stock_actual)
VALUES
    ('AUR-001', 'Auriculares Bluetooth',  10000.00, 21.0, '7', 'LISTA GENERAL', 50),
    ('CAB-002', 'Cable USB-C 2m',            500.00, 21.0, '7', 'LISTA GENERAL', 200),
    ('MON-024', 'Monitor 24 pulgadas FHD', 80000.00, 21.0, '7', 'LISTA MAYORISTA', 10),
    ('LIB-001', 'Libro Educativo',           1500.00,  0.0, '7', 'LISTA GENERAL', 30);
