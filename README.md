# POS — Factura Electrónica Argentina
## Middleware Spring Boot entre Punto de Venta y API REST AFIP/ARCA

---

## Estructura del proyecto

```
pos-factura-electronica/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/pos/factura/
    │   │   ├── FacturaElectronicaApplication.java   ← Clase principal
    │   │   ├── config/
    │   │   │   ├── ApiConfig.java                   ← Credenciales API externa
    │   │   │   ├── EmisorConfig.java                ← Datos del emisor (CUIT, PV)
    │   │   │   └── RestTemplateConfig.java          ← HTTP client con timeouts
    │   │   ├── controller/
    │   │   │   └── FacturaController.java           ← Endpoint REST para el POS
    │   │   ├── model/
    │   │   │   ├── Cliente.java
    │   │   │   ├── Comprobante.java
    │   │   │   ├── ComprobanteAsociado.java
    │   │   │   ├── DetalleItem.java
    │   │   │   ├── Pagos.java
    │   │   │   └── Producto.java
    │   │   ├── dto/
    │   │   │   ├── SolicitudFacturaRequest.java     ← Lo que envía el POS
    │   │   │   ├── ApiFacturaRequest.java           ← Lo que va a la API externa
    │   │   │   ├── ApiFacturaResponse.java          ← Respuesta de la API externa
    │   │   │   └── FacturaResponse.java             ← Respuesta al POS
    │   │   ├── service/
    │   │   │   └── FacturaService.java              ← Orquesta todo el flujo
    │   │   ├── client/
    │   │   │   └── FacturaApiClient.java            ← HTTP POST a API externa (con reintentos)
    │   │   ├── util/
    │   │   │   ├── CalculadorComprobante.java       ← Cálculo de totales AFIP
    │   │   │   └── ValidadorComprobante.java        ← Validaciones de negocio AFIP
    │   │   └── exception/
    │   │       ├── FacturaValidationException.java
    │   │       ├── ApiComunicacionException.java
    │   │       └── GlobalExceptionHandler.java
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/com/pos/factura/
            └── CalculadorComprobanteTest.java
```

---

## Requisitos

- Java 11+
- Maven 2.7+
- IntelliJ IDEA (Community o Ultimate)

---

## Cómo ejecutar en IntelliJ

1. **Abrir el proyecto**: `File → Open` → seleccionar la carpeta `pos-factura-electronica`
2. **Esperar** que IntelliJ descargue dependencias Maven automáticamente
3. **Configurar credenciales** en `src/main/resources/application.properties`:
   ```properties
   factura.api.usertoken=TU_USERTOKEN
   factura.api.apikey=TU_APIKEY
   factura.api.apitoken=TU_APITOKEN
   factura.emisor.cuit=20123456789
   factura.emisor.punto-venta=1
   ```
4. **Ejecutar**: click derecho en `FacturaElectronicaApplication` → `Run`
5. **Verificar**: `GET http://localhost:8080/api/v1/facturas/ping`

---

## Flujo completo

```
POS (punto de venta)
      │
      │  POST /api/v1/facturas/emitir
      │  { cliente: {...}, comprobante: { detalle: [...] } }
      ▼
FacturaController
      │
      ▼
FacturaService
      ├── 1. Inyectar punto de venta (desde config)
      ├── 2. CalculadorComprobante.calcularTotales()
      ├── 3. ValidadorComprobante.validar()  ← reglas AFIP
      ├── 4. Construir ApiFacturaRequest (+ credenciales)
      └── 5. FacturaApiClient.emitirFactura()
                    │
                    │  POST → API externa (TusFacturas, etc.)
                    ▼
             { cae, pdf_b64, qr, nro_comprobante }
                    │
                    ▼
      FacturaResponse → POS
```

---

## Endpoints

| Método | URL | Descripción |
|--------|-----|-------------|
| POST | `/api/v1/facturas/emitir` | Emitir factura electrónica |
| GET | `/api/v1/facturas/ping` | Health check |

---

## Ejemplo de request — Factura B (ver request_ejemplo_facturaB.json)

```bash
curl -X POST http://localhost:8080/api/v1/facturas/emitir \
  -H "Content-Type: application/json" \
  -d @request_ejemplo_facturaB.json
```

## Ejemplo de respuesta exitosa

```json
{
  "exitoso": true,
  "mensaje": "Comprobante emitido correctamente",
  "cae": "12345678901234",
  "caeVencimiento": "04/05/2026",
  "comprobanteNro": "00000001-00000001",
  "tipoComprobante": "FACTURA B",
  "fecha": "24/04/2026",
  "total": 25833.50,
  "pdfBase64": "JVBERi0xLjQK...",
  "afipQr": "https://www.afip.gob.ar/...",
  "errores": null
}
```
