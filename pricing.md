# Precios (scraping)

Léelo cuando toques `pricing/`.

El precio se obtiene por scraping detrás de una interfaz `PriceProvider`, con una
implementación por tienda (`SamsPriceProvider`, `WalmartPriceProvider`,
`BodegaPriceProvider`).

## Reglas

- **Job programado (decidido).** Un `@Scheduled` diario (de madrugada) recorre los
  productos del catálogo y refresca sus `PriceSnapshot`. El scraping **nunca** ocurre en
  el hilo de la petición de agregar/editar.
- Un producto recién agregado sin precio muestra `price = null` hasta la siguiente
  corrida. (Precio inmediato = encolar un scrape puntual; fuera del MVP.)
- Job **idempotente** y con concurrencia controlada; no re-scrapear si el snapshot es
  reciente (< 24h). Escalonar peticiones.
- **Rate limiting** y reintentos con backoff; respetar `robots.txt` y los términos de
  servicio. El scraping es frágil (puede romperse si cambian su HTML): aislarlo bien
  tras la interfaz para poder sustituirlo.
- Los selectores CSS/estructura de cada sitio van **solo** dentro de su implementación,
  nunca filtrados al resto del código.
- Tests: **mockear** siempre los `PriceProvider`; nunca red real. Guardar HTML de
  ejemplo como fixtures para probar el parseo.

## Contexto

En México, Sam's, Walmart y Bodega Aurrera son del mismo grupo (Walmart de México) y no
ofrecen API pública gratuita de precios; por eso el scraping. Si en el futuro se prefiere
estabilidad, considerar captura manual + enriquecimiento por código de barras
(Open Food Facts) como fuente alterna detrás de la misma interfaz.
