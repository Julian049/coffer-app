---
title: Avisos de terceros de Coffer
layout: default
---

# Avisos de terceros

Coffer se construye con software de código abierto. Cada componente conserva su propia licencia; ninguna de las siguientes obliga a abrir el código de Coffer. Inventario revisado el 2026-10-22 contra `libs.versions.toml` (app) y `pom.xml` (servidor). La versión exacta de cada una está en esos archivos.

## App Android

| Componente | Licencia |
| --- | --- |
| Kotlin, kotlinx.coroutines, kotlinx.serialization | Apache-2.0 |
| AndroidX: Compose, Material 3, Activity, Lifecycle, Room, WorkManager, DataStore, Credential Manager, Hilt | Apache-2.0 |
| Material Icons | Apache-2.0 |
| Dagger y Hilt | Apache-2.0 |
| OkHttp, Retrofit | Apache-2.0 |
| SQLCipher para Android (Zetetic) | BSD-style (aviso de copyright de Zetetic) |
| Servicios de Google Play, Google Identity (inicio con Google) | Términos del SDK de Android / Google |

Solo en pruebas (no van en la app instalada): JUnit (EPL-2.0), Robolectric (MIT), Roborazzi (Apache-2.0), Konsist (Apache-2.0), MockWebServer (Apache-2.0).

## Servidor

| Componente | Licencia |
| --- | --- |
| Spring Boot, Spring Framework, Spring Security, Spring Data | Apache-2.0 |
| Hibernate ORM | Apache-2.0 |
| Flyway (edición Community) | Apache-2.0 |
| Controlador JDBC de PostgreSQL | BSD-2-Clause |
| springdoc-openapi | Apache-2.0 |
| HikariCP, Nimbus JOSE + JWT | Apache-2.0 |

Solo en pruebas: Testcontainers (MIT), ArchUnit (Apache-2.0), swagger-request-validator (Apache-2.0), JaCoCo (EPL-2.0).

## Tipografía, íconos, marca y mascota

- **Tipografía:** la fuente del sistema de Android (`FontFamily.Default`). Coffer no incluye archivos de fuentes propios.
- **Íconos:** Material Icons de Google (Apache-2.0).
- **Logo y mascota (zorro-grano de café):** generados con ChatGPT (OpenAI) a partir de indicaciones del autor. Según los términos de OpenAI, el usuario es titular de los resultados que genera; aun así, una obra creada solo por IA puede no tener protección de autor, por lo que el autor reserva su uso como marca sin afirmar un registro.
- **Prototipo de diseño:** Figma, uso interno.

## Importar desde Cashew

Coffer lee el archivo CSV que el propio usuario exporta de Cashew para traer sus movimientos. No incluye código, marca ni recursos de Cashew.
