ALTER TABLE tickets
    ADD COLUMN percepcion DECIMAL(12,2) DEFAULT 0.00 AFTER bonificacion;

ALTER TABLE factura_cabecera
    ADD COLUMN percepcion DECIMAL(12,2) DEFAULT 0.00 AFTER bonificacion;
