ALTER TABLE ticket_items
    ADD COLUMN impuesto_interno DECIMAL(12,4) NOT NULL DEFAULT 0 AFTER alicuota_iva;
