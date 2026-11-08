# BandTracker

## 1. Propósito
Aplicación Android para llevar un registro local de ingresos y egresos de **dinero en efectivo**, organizado en cuentas (libros de registros) independientes. Cada cuenta es solo una base de datos consultable; la app no mueve dinero ni hace conciliaciones.

## 2. Alcance
- Múltiples cuentas, cada una con su propia moneda.
- Registrar ingresos y egresos con monto, fecha, sujeto y nota.
- Historial por cuenta, agrupado por día.
- Editar y borrar registros y cuentas.
- Exportar e importar todos los datos en JSON.
- Interfaz en español e inglés, tema claro/oscuro del sistema.

## 3. Plataforma y stack
- Objetivo: GrapheneOS. Dispositivo de prueba: Pixel 9a con GrapheneOS.
- applicationId: `com.v4atu.bandtracker`.
- Kotlin, Jetpack Compose, Room (SQLite local).
- Solo dependencias AndroidX/Kotlin y serialización JSON. Ninguna dependencia puede traer Google Play services, Firebase, telemetría ni acceso a red.
- minSdk: 33 (Android 13). targetSdk: el vigente.
- Licencia: MIT.
- Distribución: APK firmado en GitHub Releases (compatible con actualización mediante Obtainium).

## 4. Modelo de datos

### Account
| Campo | Tipo | Reglas |
|---|---|---|
| id | Long (PK, autoincrement) | Interno |
| uuid | String | Identificador estable para exportar/importar |
| name | String | Obligatorio; único sin distinguir mayúsculas ni tildes (la `ñ` sí se distingue de la `n`): «Café» = «cafe», «Año» ≠ «Ano» |
| currency | String (ISO 4217, ej. `COP`) | Obligatorio; **inmutable** tras crear la cuenta |

### Entry (registro)
| Campo | Tipo | Reglas |
|---|---|---|
| id | Long (PK, autoincrement) | Desempate de orden dentro del mismo día |
| uuid | String | Identificador estable para exportar/importar |
| accountId | Long (FK → Account) | `ON DELETE CASCADE` |
| type | `INCOME` \| `EXPENSE` | Obligatorio |
| amountMinor | Long | Entero > 0 en la unidad mínima de la moneda. **Nunca float** |
| date | LocalDate | Solo día, sin hora. Por defecto: hoy en la zona horaria del dispositivo. Editable. **No puede ser posterior a hoy** (ni al crear ni al editar) |
| subject | String? | Opcional |
| note | String? | Opcional |

Reglas:
- No hay `created_at` ni `updated_at`; `date` es la única fecha.
- Los decimales de cada moneda son los de ISO 4217 y **los fija la app** (`CurrencyDigits`), no `java.util.Currency`: COP = 2, USD = 2, JPY = 0, KWD = 3. Motivo: el monto se guarda en unidad mínima y los datos de Android cambian entre versiones (en Android 17 `COP` pasó de 2 a 0 decimales), lo que habría multiplicado por 100 los montos de un respaldo exportado en otro teléfono. La lista de monedas disponibles sí sale de `java.util.Currency`.
- Orden global del historial: `date DESC, id DESC`.
- Comparación de texto (nombre de cuenta y sujeto): se ignoran mayúsculas y tildes, pero no la `ñ`. SQLite solo ignora mayúsculas ASCII, así que se guarda una clave normalizada calculada en Kotlin (`nameKey`, `subjectKey`) y las comparaciones se hacen sobre ella.
- Índices: `(accountId, date DESC, id DESC)` y `(accountId, subjectKey)` para el autocompletado; `nameKey` único.

## 5. Pantallas

### 5.1 Main (inicio)
- Tres botones en columna: **Ingreso**, **Egreso**, **Historial**.
- Arriba a la derecha: desplegable de **Cuenta** con la lista de cuentas y una entrada para abrir **Cuentas**. Junto a él, icono **"?"** que abre Ayuda.
- Sin ninguna cuenta creada: el único elemento es un botón **+ Cuenta** centrado (el "?" permanece disponible).
- La última cuenta seleccionada se recuerda entre ejecuciones.
- Todas las acciones operan sobre la cuenta seleccionada.

### 5.2 Ingreso / Egreso (dos pantallas separadas)
Misma estructura, distinto `type`:
- Monto (único campo obligatorio; teclado numérico; decimales según la moneda de la cuenta).
- Fecha (por defecto hoy; selector discreto).
- Sujeto (opcional, con sugerencias locales).
- Nota (opcional).
- Botón de confirmación: "Ingresar" / "Egresar".

Comportamiento:
- Monto vacío o 0 bloquea el botón.
- El selector de fecha no permite elegir días posteriores a hoy.
- Sugerencias de sujeto: sujetos distintos ya usados **en esa cuenta**, coincidencia por prefijo sin distinguir mayúsculas ni tildes (la `ñ` se distingue, §4), máximo 5. Con el campo vacío se sugieren los sujetos más recientes. Solo consulta local.
- Tras guardar, vuelve a Main y muestra un aviso breve y discreto (~2 s), p. ej. «Ingreso guardado» / «Egreso guardado».

### 5.3 Historial
- Lista de la cuenta seleccionada (una cuenta a la vez; nunca se mezclan monedas).
- Del más reciente al más antiguo, con **encabezados planos por día**. Cuando cambia el mes o el año se muestra el separador correspondiente.
- Cada fila: a la izquierda el **sujeto** con la **nota** como subtítulo si existe; a la derecha el monto, verde para ingreso y rojo para egreso. Además del color, el monto lleva signo `+` / `−` para no depender solo del color.
- Si el registro no tiene sujeto, el título de la fila es la nota. Si tampoco hay nota, el título es un texto localizado ("Sin sujeto" / "No subject").
- Sin totales por sección en la v1.
- Tocar una fila abre la edición (mismos campos que la captura, incluida la fecha, con la misma restricción de no futuro).
- Borrar: desde la edición, con diálogo de confirmación. Definitivo, sin deshacer.

### 5.4 Cuentas
- Lista de cuentas; crear (nombre + moneda), renombrar, borrar.
- Al crear o renombrar, si el nombre ya existe (sin distinguir mayúsculas ni tildes, §4) se muestra el mensaje «Ya existe una cuenta con ese nombre» y no se permite confirmar.
- La moneda se elige solo al crear, con un buscador sobre todas las monedas ISO 4217 que ofrece el sistema (`java.util.Currency`). Es inmutable después.
- Borrar una cuenta elimina también todos sus registros, con confirmación fuerte que muestra cuántos registros se perderán.
- **Exportar** e **Importar** viven aquí (ver sección 7).
- Cada cuenta de la lista tiene dos controles, **editar** (renombrar) y **borrar** (decisión de V4atu; falta su diseño en Penpot, ver DESIGN §10).

### 5.5 Ayuda
- Versión de la app, quién la mantiene (v4atu), enlace al repositorio de GitHub y licencia (MIT).
- El enlace se abre con un intent `ACTION_VIEW` al navegador del usuario; si no hay navegador, se captura `ActivityNotFoundException` y se muestra la URL como texto.
- Una fila **Licencias de terceros** abre una pantalla con el texto completo de la licencia de Istok Web (OFL 1.1) y de Lucide (ISC/MIT), leído de lo empaquetado en la app: ambas licencias exigen conservar su aviso (decisión de V4atu).

## 6. Interfaz
- Minimalista: sin elementos decorativos, sin pantallas intermedias, sin ruido visual.
- Tema claro/oscuro según el sistema; los colores verde/rojo deben cumplir contraste suficiente en ambos.
- Idiomas: español (por defecto) e inglés, siguiendo el idioma del dispositivo. Todo texto vive en recursos de strings.
- Formato de montos y fechas según la configuración regional del dispositivo y la moneda de la cuenta.

## 7. Exportar / Importar (JSON)
- Se usa el selector de archivos del sistema (`CreateDocument` / `OpenDocument`); no se piden permisos de almacenamiento.
- El archivo incluye un campo `version` del esquema, y todas las cuentas con sus registros (uuid, nombre, moneda, tipo, monto en unidad mínima, fecha ISO `yyyy-MM-dd`, sujeto, nota).
- **Importar fusiona**: agrega cuentas y registros cuyo `uuid` no exista; los que ya existen se omiten sin modificarse. Nunca borra ni sobrescribe datos existentes.
- La importación es atómica (todo o nada) y rechaza archivos con una `version` mayor a la soportada o con datos inválidos, mostrando un mensaje claro.
- Al rechazar un archivo se dice el motivo y que no se cambió ningún dato. Si sale bien, un diálogo resume cuántas cuentas y registros se agregaron, cuántos ya existían y qué cuentas se renombraron por tener un nombre repetido (decisión de V4atu).
- Un registro importado con fecha posterior a hoy se rechaza como dato inválido y aborta toda la importación (consistente con la regla de `date`).
- Consecuencia asumida: al no haber marca de última modificación, una edición hecha después de exportar no se propaga al importar un registro con el mismo `uuid`.

## 8. Requisitos no funcionales (verificables)

### Privacidad
- El manifest **no declara el permiso `INTERNET`** (ni ningún otro permiso).
- Sin Google Play services, Firebase, analytics, crash reporters ni librerías con telemetría. Nada que pueda interpretarse como monitoreo del usuario.
- Datos únicamente en el almacenamiento privado de la app; `allowBackup="false"` y reglas de extracción que excluyen todo. El único respaldo es la exportación manual.
- Fuentes e íconos empaquetados en la app; ninguna carga de recursos remotos.

### Batería
- Sin servicios en segundo plano, WorkManager, alarmas, notificaciones, wake locks ni receptores de broadcast.
- Consumo en segundo plano nulo por diseño; la app solo trabaja mientras está en primer plano.
- Consultas con índices y paginación del historial (Room + Paging o `LazyColumn` con consultas acotadas).

### Verificación (criterios de aceptación)
1. `aapt dump permissions` sobre el APK no muestra permisos.
2. El árbol de dependencias de Gradle no contiene artefactos `com.google.android.gms`, `com.google.firebase` ni analytics.
3. En un Pixel 9a con GrapheneOS, la app instala y funciona sin Play services ni acceso a red.
4. Crear, editar y borrar registros y cuentas funciona según las secciones 5.3 y 5.4.
5. Un export seguido de un import sobre una instalación limpia reproduce los mismos datos; un segundo import no duplica nada.
6. Los montos admiten tantos decimales como indique ISO 4217 según la tabla fija de la app (COP y USD: 2; JPY: 0), igual en cualquier versión de Android; nunca hay errores de redondeo.
7. No es posible crear ni editar un registro con fecha posterior a hoy.
8. Con la cuenta sin sujeto ni nota en un registro, el historial muestra el texto localizado de respaldo en ambos idiomas.

## 9. Pendientes por decidir
Ninguno.

Decididos:
- URL del repositorio de GitHub: `https://github.com/v4atu/BandTracker` (remoto SSH: `git@github.com:v4atu/BandTracker.git`).
- Nombre del mantenedor mostrado en Ayuda: `v4atu`.