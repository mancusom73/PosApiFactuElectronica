package com.pos.factura.service;

import com.pos.factura.entity.dbtpviv.Evento;
import com.pos.factura.repository.dbtpviv.EventoRepository;

import org.springframework.stereotype.Service;

@Service
public class EventoService {

    private final EventoRepository repository;

    public EventoService(
            EventoRepository repository
    ) {
        this.repository = repository;
    }

    public Evento buscar(
            Integer cajaZ,
            Integer idEvento
    ) {

        return repository
                .findByIdCajaZAndIdIdEvento(
                        cajaZ,
                        idEvento
                );
    }
}