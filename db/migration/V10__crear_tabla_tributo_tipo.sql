CREATE TABLE tributo_tipo (
    codigo INT NOT NULL,
    descripcion VARCHAR(100) NOT NULL,
    PRIMARY KEY (codigo)
);

INSERT INTO tributo_tipo (codigo, descripcion) VALUES
(1, 'Impuestos nacionales'),
(2, 'Impuestos provinciales'),
(3, 'Tributos municipales'),
(4, 'Impuestos internos'),
(5, 'IIBB'),
(6, 'Percepcion de IVA'),
(7, 'Percepcion de IIBB'),
(8, 'Percepciones por Tributos Municipales'),
(9, 'Otras Percepciones'),
(10, 'Impuesto interno a nivel item'),
(13, 'Percepcion de IVA a no Categorizado'),
(14, 'Retencion IIGG - RG 830'),
(15, 'Retencion IVA - RG 3873'),
(16, 'Pago a cuenta IVA - RG 3873'),
(17, 'Percepcion IVA RG 3873'),
(18, 'Retencion IVA - RG 2616/2009'),
(19, 'Retencion Ganancias - RG 2616/2009'),
(99, 'Otros');
