CREATE TABLE condicion_iva_cliente (
    codigo INT NOT NULL,
    descripcion VARCHAR(100) NOT NULL,
    codigoPOS INT NULL,
    PRIMARY KEY (codigo)
);

INSERT INTO condicion_iva_cliente (codigo, descripcion,codigoPOS) VALUES
(1, 'IVA Responsable Inscripto',1),
(4, 'IVA Sujeto Exento',4),
(5, 'Consumidor Final',5),
(6, 'Responsable Monotributo',6),
(7, 'Sujeto no Categorizado',7),
(8, 'Proveedor del Exterior', null),
(9, 'Cliente del Exterior',9),
(10, 'IVA Liberado - Ley N 19.640',null),
(13, 'Monotributista Social',null),
(15, 'IVA No Alcanzado',null),
(16, 'Monotributo Trabajador Independiente Promovido', null);
