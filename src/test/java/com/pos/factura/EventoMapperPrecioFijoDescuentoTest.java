package com.pos.factura;

import com.pos.factura.mapper.EventoMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EventoMapperPrecioFijoDescuentoTest {

    @Autowired
    private EventoMapper eventoMapper;

    @Test
    void obtenerPrecioFijoDescuento_cajaZ100190_idEvento62() {
        Long cajaZ = 100190L;
        Long idEvento = 62L;
        int[] codArts = {36287, 36281, 35932};

        for (int codArt : codArts) {
            Double importe = eventoMapper.obtenerPrecioFijoDescuento(cajaZ, idEvento, codArt);
            System.out.println("codArt=" + codArt + " -> precioFijoDescuento=" + importe);
        }
    }
}