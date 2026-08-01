package com.pos.factura;

import com.pos.factura.mapper.EventoMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EventoMapperDescuentoTicketTest {

    @Autowired
    private EventoMapper eventoMapper;

    @Test
    void obtenerDescuentoTicket_cajaZ100190_idEvento62() {
        Long cajaZ = 100190L;
        Long idEvento = 62L;

        Double descuentoTicket = eventoMapper.obtenerDescuentoTicket(cajaZ, idEvento);
        System.out.println("descuentoTicket=" + descuentoTicket);
    }
}