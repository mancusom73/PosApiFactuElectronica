ALTER TABLE factura_detalle
    MODIFY COLUMN precio_unitario_sin_iva DECIMAL(12,4) NOT NULL,
    MODIFY COLUMN neto_linea              DECIMAL(12,4),
    MODIFY COLUMN iva_linea               DECIMAL(12,4),
    MODIFY COLUMN total_linea             DECIMAL(12,4);

ALTER TABLE ticket_items
    MODIFY COLUMN precio_unitario_sin_iva DECIMAL(12,4) NOT NULL;
