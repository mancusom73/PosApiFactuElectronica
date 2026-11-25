package com.pos.factura.repository.dbtpviv;

import com.pos.factura.entity.dbtpviv.CajaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CajaRepository extends JpaRepository<CajaEntity, Short> {

    @Query("SELECT c.xapikey FROM CajaEntity c, Evento e WHERE c.caja = e.caja AND e.cajaZ = :cajaZ AND e.idEvento = :idEvento")
    String findXapikeyByEvento(@Param("cajaZ") Long cajaZ, @Param("idEvento") Long idEvento);

    default String obtenerXapikey(Long cajaZ, Long idEvento) {
        String apiKey = findXapikeyByEvento(cajaZ, idEvento);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(String.format(
                    "No se encontró XAPIKEY en cajas para el evento cajaZ=%s, idEvento=%s", cajaZ, idEvento));
        }
        return apiKey;
    }
}
