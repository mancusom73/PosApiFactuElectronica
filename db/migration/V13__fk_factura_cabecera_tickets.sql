ALTER TABLE tickets
    ADD COLUMN caja_z    BIGINT AFTER numero_ticket,
    ADD COLUMN id_evento BIGINT AFTER caja_z,
    ADD UNIQUE KEY uq_tickets_evento (caja_z, id_evento, fecha);

ALTER TABLE factura_cabecera
    ADD COLUMN caja_z    BIGINT AFTER numero_ticket_pos,
    ADD COLUMN id_evento BIGINT AFTER caja_z,
    ADD CONSTRAINT fk_fc_ticket
        FOREIGN KEY (caja_z, id_evento, fecha) REFERENCES tickets (caja_z, id_evento, fecha);
