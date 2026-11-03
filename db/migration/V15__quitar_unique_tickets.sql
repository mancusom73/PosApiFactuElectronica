ALTER TABLE tickets
    DROP INDEX uq_tickets_numero,
    DROP INDEX uq_tickets_evento,
    ADD INDEX idx_tickets_evento (caja_z, id_evento, fecha);
