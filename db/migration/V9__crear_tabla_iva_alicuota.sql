CREATE TABLE iva_alicuota (
    codigo INT NOT NULL,
    alicuota DECIMAL(5,2) NOT NULL,
    descripcion VARCHAR(150) NOT NULL,
    PRIMARY KEY (codigo)
);

INSERT INTO iva_alicuota (codigo, alicuota, descripcion) VALUES
(3, 0.00, 'Operaciones exentas, exportaciones o productos alcanzados al 0%.'),
(4, 10.50, 'Alicuota reducida (ej. pan, harina de trigo, carnes, frutas, verduras, obra destinada a vivienda).'),
(5, 21.00, 'Alicuota general (mayoria de bienes, productos y servicios).'),
(6, 27.00, 'Alicuota incrementada (servicios publicos como gas, electricidad, telefonia e internet).'),
(8, 5.00, 'Alicuotas especiales (segun actividad editorial o regimenes especificos).'),
(9, 2.50, 'Alicuotas reducidas especiales (segun actividad editorial o regimenes especificos).');
