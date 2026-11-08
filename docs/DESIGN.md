# BandTracker — Traspaso de diseño a código

Este documento traduce el diseño hecho en Penpot a lo que hay que implementar en Compose. La fuente de verdad funcional es [`SPEC.md`](SPEC.md); aquí solo se describe **cómo se ve** y **cómo se mapea** al código.

- **Archivo de diseño:** Penpot, archivo «BandTracker», con las páginas `Pantallas` (18 tableros) y `Componentes` (14 componentes de interfaz y 14 íconos).
- **Referencia de pantalla:** 411 × 923 dp, que son las medidas de fábrica del Pixel 9a (1080 × 2424 px a 420 dpi). Los tableros no dibujan barras de sistema: en Compose se resuelven con *insets*.
- **Cambiar de tema en Penpot:** panel de Tokens → Temas → `Oscuro` o `Claro`. El tema activo es global para todo el archivo.
- **Estado:** diseño de la v1, todavía sin implementar. Los textos y montos de las pantallas (p. ej. «Account 1», «Viaje», 15.000) son datos de ejemplo.

> Las secciones de Compose siguen la documentación oficial de Android consultada con Context7 (`developer.android.com/develop/ui/compose`: *designsystems/custom*, *designsystems/material3*, *text/fonts*, *graphics/images/material*, *accessibility/api-defaults*). Lo marcado como **propuesta** es una sugerencia de mapeo, no algo ya decidido.

---

## 1. Decisiones de diseño que no están en el SPEC

| Decisión | Detalle |
|---|---|
| Sin saldo | Inicio e Historial no muestran saldo ni totales (SPEC §5.3: «Sin totales por sección en la v1»). |
| Ayuda arriba | El botón «?» va a la derecha del selector de cuenta (SPEC §5.1). |
| Acciones abajo | Las tres acciones de Inicio están ancladas en el tercio inferior (zona del pulgar), como filas de 64 dp con ícono, etiqueta y chevron. El SPEC §5.1 pide «tres botones en columna»; no fija dónde. |
| Íconos, no símbolos | Los símbolos `+`, `−`, `=`, `?`, `▾`, `✓`, `↑`, `↓`, `›` y `↗` se reemplazaron por íconos SVG (§5). Un ícono nunca va solo: siempre lleva etiqueta, salvo el botón de ayuda, que lleva `contentDescription`. |
| Signo menos | Los **montos** conservan el signo: `+` / `−` (U+2212, no el guion `-`) como texto (SPEC §5.3). |
| Color con significado | Verde = ingreso, rojo = egreso. El resto es neutro. El color nunca es la única señal: los montos llevan `+` / `−` (SPEC §5.3) y las acciones llevan ícono y etiqueta. |
| Campo Fecha | Las pantallas de captura y edición muestran «Hoy» por defecto, con ícono de calendario que abre el selector (los días futuros se muestran deshabilitados). |
| Botón de captura | Dice «Ingresar» o «Egresar» (SPEC §5.2). Mientras el monto está vacío o en 0 se muestra deshabilitado. |
| Aviso al guardar | Al volver a Inicio tras guardar aparece un aviso breve («Ingreso guardado» / «Egreso guardado»). Agregado al SPEC §5.2. |
| Nombre duplicado | Al crear o renombrar una cuenta con un nombre ya usado (sin distinguir mayúsculas ni tildes, SPEC §4) se muestra «Ya existe una cuenta con ese nombre» y el botón queda deshabilitado. Agregado al SPEC §5.4. |
| Nota vs. sujeto | Sujeto como título de la fila y nota como subtítulo. Sin sujeto, el título es la nota; sin ambos, «Sin sujeto» en gris. |
| Esquinas | 12 dp en todas las superficies (antes 5 dp). Los íconos tonales son círculos. |
| Áreas táctiles | Todo lo interactivo mide **al menos 48 dp** de alto y de ancho (§4). |

---

## 2. Tokens de color → Kotlin

Penpot define los mismos nombres en dos sets (`Oscuro` y `Claro`). Todos los colores de las pantallas, incluidos los trazos de los íconos, están enlazados a estos tokens: no hay hex sueltos.

| Token | Oscuro | Claro | Uso |
|---|---|---|---|
| `color.bg` | `#0A0A0A` | `#FAFAFA` | Fondo de pantalla; círculo de los íconos tonales; texto del aviso |
| `color.surface` | `#141414` | `#EFEFEF` | Tarjetas, filas, selector, diálogos |
| `color.text.primary` | `#EDEDED` | `#141414` | Texto principal; fondo del aviso |
| `color.text.secondary` | `#A0A0A0` | `#595959` | Etiquetas, notas, placeholders, chevrons |
| `color.income` | `#02A45E` | `#00703C` | Ingreso, acción principal |
| `color.expense` | `#FF6B6B` | `#B3261E` | Egreso, acciones destructivas, errores |
| `color.on-accent` | `#0A0A0A` | `#FFFFFF` | Texto sobre botones de color |
| `color.divider` | `#2A2A2A` | `#D0D0D0` | Líneas bajo los campos y separadores |

### Contraste medido (WCAG)

Mínimos: 4.5:1 para texto y 3:1 para íconos y gráficos (*Core app quality* de Android).

| Par | Oscuro | Claro |
|---|---|---|
| Texto principal sobre `bg` / `surface` | 16.91 / 15.74 | 17.65 / 16.02 |
| Texto secundario sobre `bg` / `surface` | 7.57 / 7.04 | 6.71 / 6.09 |
| Ícono o monto de ingreso sobre `bg` | 6.10 | 5.95 |
| Ícono o monto de egreso sobre `bg` | 7.13 | 6.26 |
| Monto de ingreso sobre `surface` | 5.68 | 5.40 |
| Monto de egreso sobre `surface` | 6.64 | 5.68 |
| Mensaje de error (12 sp) sobre `bg` | 7.13 | 6.26 |
| `on-accent` sobre `income` (botón, día seleccionado) | 6.10 | 6.21 |
| `on-accent` sobre `expense` (botón) | 7.13 | 6.54 |
| Aviso: `bg` sobre `text.primary` | 16.91 | 17.65 |

Todos los pares pasan. El texto deshabilitado (días futuros del calendario, botón deshabilitado) queda atenuado a propósito, como permite WCAG para componentes inactivos. El rojo original del diseño en Figma (`#601410`) daba cerca de 1.5:1 y por eso se reemplazó. El mantenedor verificará el contraste con la app implementada.

### Propuesta de implementación

`ColorScheme` de Material 3 no trae ingreso/egreso, así que el docs de Android recomienda extender el tema con un `CompositionLocal` (patrón `ExtendedColors` de *Custom design systems in Compose*):

```kotlin
@Immutable
data class BandTrackerColors(
    val income: Color,
    val expense: Color,
    val onAccent: Color,
    val divider: Color,
)

val LocalBandTrackerColors = staticCompositionLocalOf {
    BandTrackerColors(
        income = Color.Unspecified, expense = Color.Unspecified,
        onAccent = Color.Unspecified, divider = Color.Unspecified,
    )
}
```

Mapeo sugerido a `ColorScheme` (lo demás se queda en los valores por defecto):

| Token | `ColorScheme` / extendido |
|---|---|
| `color.bg` | `background` |
| `color.surface` | `surface` |
| `color.text.primary` | `onBackground`, `onSurface` (y `inverseSurface` para el aviso) |
| `color.text.secondary` | `onSurfaceVariant` |
| `color.income` | `BandTrackerColors.income` |
| `color.expense` | `BandTrackerColors.expense` |
| `color.on-accent` | `BandTrackerColors.onAccent` |
| `color.divider` | `BandTrackerColors.divider` |

### Color dinámico: desactivado (decidido)

La app usa su propia paleta, no la del fondo de pantalla del usuario. `ui/theme/Theme.kt` hoy trae `dynamicColor: Boolean = true` y el proyecto tiene `minSdk = 33`, así que el esquema dinámico se aplicaría **siempre** y taparía esta paleta. La documentación de Android indica que el color dinámico es opcional y que el `ColorScheme` propio es la alternativa. Al implementar hay que dejar `dynamicColor = false` o quitar esa rama, y usar solo `DarkColorScheme` / `LightColorScheme` con los valores de la tabla.

Los colores de `Color.kt` (`Purple80`, `Pink40`, …) y los esquemas de `Theme.kt` son del *scaffold* de Android Studio y no pertenecen al diseño.

---

## 3. Tipografía → `TextStyle`

Una sola familia: **Istok Web**. Interlineado: 1.2 × el tamaño de fuente.

| Estilo en Penpot | Tamaño | Peso | Dónde se usa |
|---|---|---|---|
| `Monto` | 48 | Bold (700) | Monto en captura y edición |
| `Título` | 36 | Bold (700) | Título de pantalla |
| `Acción` | 24 | Bold (700) | Montos de fila, títulos de diálogo, botón primario |
| `Cuerpo` | 16 | Regular (400) | Etiquetas, mensajes, sugerencias, aviso |
| `Cuerpo/Énfasis` | 16 | Bold (700) | Etiqueta de las filas de acción, botones de texto, valores de campo, nombres |
| `Nota` | 12 | Regular (400) | Nota de la fila, encabezados de día, mensaje de error, avisos |

**Interlineado en Compose.** Istok Web mide ~1.44× de alto natural y Compose no baja una línea suelta de esa altura con ningún `LineHeightStyle` (medido en dispositivo; solo respeta el interlineado *entre* líneas). Para que una línea mida 1.2× como en Penpot, los componentes usan `BandText` / `Modifier.lineBox` (`ui/components/BandText.kt`), que fija la caja de la línea y centra el texto, como CSS. Los textos de varias líneas (mensaje del diálogo) conservan la altura natural arriba y abajo.

En Compose (SPEC §8: las fuentes van empaquetadas en la app, sin cargas remotas), según *Fonts in Compose*:

```kotlin
val IstokWeb = FontFamily(
    Font(R.font.istok_web_regular, FontWeight.Normal),
    Font(R.font.istok_web_bold, FontWeight.Bold),
)
```

Los archivos van en `app/src/main/res/font/`. `ui/theme/Type.kt` hoy usa `FontFamily.Default` y solo define `bodyLarge`. Como los 6 estilos no coinciden con los 15 de Material, la propuesta es definirlos como `TextStyle` propios dentro de un objeto del tema (y no forzarlos en `Typography`).

### Licencia de la fuente

Istok Web está bajo la **SIL Open Font License 1.1** (Copyright © 2008–2012, 2014 Andrey V. Panov, con el nombre reservado «Istok»). Permite empaquetarla en la app y redistribuirla, con estas condiciones:

- **Cada copia debe llevar el aviso de copyright y el texto completo de la licencia** (puede ir como archivo de texto). Al implementar: guardar el texto de la OFL junto a la fuente, tanto en el repositorio como dentro del APK.
- **La fuente no se vende por sí sola** y se distribuye completa bajo la OFL, no bajo otra licencia. La app sigue siendo MIT; la licencia de la fuente es independiente.
- **No se modifica ni se renombra** la fuente: una versión modificada no puede usar el nombre reservado «Istok». Android exige nombres de recurso en minúsculas, así que `IstokWeb-Regular.ttf` se empaqueta como `res/font/istok_web_regular.ttf`: **mismos bytes** y mismo nombre interno de la fuente (solo cambia el nombre del archivo). El origen y los SHA-256 están en [`licenses/istok-web-ORIGEN.md`](licenses/istok-web-ORIGEN.md).

---

## 4. Espaciado, radios, forma y áreas táctiles

| Token / valor | dp | Uso |
|---|---|---|
| `radius.md` | 12 | Tarjetas, filas, botones, campos de lista y diálogos |
| `radius.full` | círculo | Íconos tonales (40 dp) y botón de ícono (48 dp) |
| `space.8`, `space.16`, `space.24`, `space.28` | 8 / 16 / 24 / 28 | Tokens de espaciado definidos en Penpot |
| Margen horizontal de pantalla | 28 | Todas las pantallas (16 en el selector de fecha, para que quepan 7 columnas de 48 dp) |
| Margen superior / inferior | 38 / 32 | Contenido de pantalla (más los *insets* del sistema) |
| Padding de tarjeta o fila | 16 horizontal, 12–16 vertical | Filas de registro, tarjetas de cuenta |
| Padding de diálogo | 24 (16 en el selector de fecha) | Diálogos |
| Separación entre filas de acción | 12 | Bloque de acciones de Inicio y Cuentas |

### Áreas táctiles (mínimo 48 dp)

La guía de accesibilidad de Compose pide un mínimo de **48 dp** para cualquier elemento que se toque. En el diseño todo lo interactivo cumple y fue verificado (110 elementos en los 18 tableros):

| Elemento | Medida |
|---|---|
| Fila de acción | 64 dp de alto |
| Botón de ícono («?») | 48 × 48 dp |
| Selector de cuenta | 48 dp de alto |
| Campo y campo con ícono | 56 dp de alto |
| Opción de lista, sugerencia, resultado de moneda | 48 dp de alto |
| Botón de texto | 48 dp de alto |
| Botón primario | 54 dp de alto |
| Celda del calendario | 48 × 48 dp de zona táctil, indicador de 40 dp |
| Flechas de mes | 48 × 48 dp |
| Fila de registro, tarjeta de cuenta | 52–61 dp de alto (la tarjeta de cuenta, 52) |

En Compose: `Modifier.minimumInteractiveComponentSize()` (reserva al menos 48 dp) o `sizeIn(minWidth = 48.dp, minHeight = 48.dp)`. Los componentes de Material ya lo hacen cuando son interactivos.

Todo el diseño usa *flex layout* en Penpot, lo que equivale a `Column` / `Row` con `Arrangement` y `Modifier.fillMaxWidth()` en Compose. Nada tiene tamaño fijo de pantalla.

---

## 5. Íconos

14 íconos de **Lucide** (trazo de 2 px, cuadrícula de 24 dp, puntas redondeadas), fijados al commit `53fab88ff9d1b48f4704b5b5b560d6b4d560bf4c` del repositorio `lucide-icons/lucide`. Están en Penpot como componentes `Ícono / <nombre>` (24 × 24) con el trazo enlazado a un token de color.

| Ícono (Lucide) | Recurso sugerido | Dónde se usa |
|---|---|---|
| `plus` | `ic_plus` | Ingreso, Nueva cuenta, «Cuenta» (Inicio sin cuentas) |
| `minus` | `ic_minus` | Egreso |
| `list` | `ic_list` | Historial |
| `circle-question-mark` | `ic_circle_question_mark` | Ayuda |
| `chevron-down` | `ic_chevron_down` | Selector de cuenta |
| `chevron-right` | `ic_chevron_right` | Chevron de las filas de acción; mes siguiente |
| `chevron-left` | `ic_chevron_left` | Mes anterior |
| `check` | `ic_check` | Opción o moneda seleccionada; aviso |
| `upload` | `ic_upload` | Exportar |
| `download` | `ic_download` | Importar |
| `external-link` | `ic_external_link` | Enlace al repositorio |
| `trash` | `ic_trash` | Borrar registro |
| `search` | `ic_search` | Campo de moneda |
| `calendar` | `ic_calendar` | Campo de fecha |

### Hacia Compose

Según la guía de *Icons* de Compose:

- Android Studio convierte cada SVG a un `VectorDrawable` (`Vector Asset` → archivo local) en `app/src/main/res/drawable/ic_*.xml`. Los íconos de Lucide usan trazo (*stroke*): **verificar tras importar** que el trazo, las puntas y las uniones redondeadas se conservan.
- Se dibujan con `Icon(painterResource(R.drawable.ic_plus), contentDescription = null)`: toma el color de `LocalContentColor` (se puede pasar `tint`) y mide 24 dp por defecto. Los íconos del diseño se tiñen con el color semántico de su fila.
- `contentDescription = null` cuando el ícono es decorativo (hay una etiqueta al lado). El botón de ayuda es el único ícono solo y necesita `contentDescription` con texto localizado.
- Todos los recursos van empaquetados (SPEC §8: sin recursos remotos). No requiere permisos ni dependencias.

### Licencia de los íconos

Lucide está bajo licencia **ISC**; varios íconos (`plus`, `minus`, `check`, `chevron-*`, `calendar`, `search`, `upload`, `download`, `external-link`, `trash`…) provienen del proyecto Feather y están bajo **MIT**. Ambas son permisivas y exigen conservar el aviso de copyright. Una copia del texto completo está en [`licenses/lucide-LICENSE.txt`](licenses/lucide-LICENSE.txt); al implementar, incluirla también dentro de la app junto a los demás avisos de licencia.

---

## 6. Componentes → composables

Cada componente de Penpot guarda el nombre del composable sugerido como dato del componente (clave `compose`). Las variantes de Penpot son los parámetros.

| Componente en Penpot | Variantes | Composable sugerido |
|---|---|---|
| Fila de acción | Tono: Ingreso / Egreso / Neutro | `ActionRow(icon, label, tone, onClick)` |
| Botón de ícono | — | `IconAction(icon, contentDescription, onClick)` |
| Selector de cuenta | — | `AccountSelector(name, onClick)` |
| Botón primario | Tipo: Ingreso / Egreso · Estado: Habilitado / Deshabilitado | `PrimaryButton(text, type, enabled)` |
| Botón de texto | Tono: Neutro / Ingreso / Egreso · Ícono: No / Sí | `TextButton(text, tone, icon, onClick)` |
| Campo | Estado: Vacío / Lleno | `FormField(label, value)` |
| Campo con ícono | Estado: Vacío / Lleno | `FormField(label, value, trailingIcon)` |
| Fila de registro | Tipo: Ingreso / Egreso · Nota: Sí / No | `EntryRow(title, note, amount, type)` |
| Encabezado de día | — | `DayHeader(date)` |
| Separador de mes | — | `MonthHeader(yearMonth)` |
| Tarjeta de cuenta | — | `AccountCard(name, currencyCode)` |
| Opción de lista | Seleccionada: Sí / No | `ListOption(text, selected)` |
| Aviso | — | `SavedSnackbar(message)` |
| Diálogo de confirmación | Destructivo: Sí / No | `ConfirmDialog(title, message, confirm, destructive)` |

En la fila de acción el ícono y el tono van juntos en el diseño (Ingreso = `plus` verde, Egreso = `minus` rojo, Neutro = `list`); para otras acciones se cambia el ícono de la instancia (Exportar con `upload`, Importar con `download`).

### Implementación (capa 5)

Los 14 componentes están en `app/src/main/java/com/v4atu/bandtracker/ui/components/` con los nombres de la tabla. Diferencias respecto a la firma sugerida:

- `FormField` tiene dos sobrecargas: de solo lectura (`value: String`, con `onClick` y `trailingIcon` opcionales) y editable (`state: TextFieldState`, la API de texto vigente de Compose, con `inputTransformation` para filtrar). En la editable la etiqueta va dentro del decorador para que el lector de pantalla la asocie con el campo.
- `TextButton` conserva ese nombre aunque Material tiene uno propio; se importa el del paquete `ui.components`.
- `PrimaryButton` recibe `EntryType` como `type`; `ActionRow` y `TextButton` reciben `Tone` (`Income`, `Expense`, `Neutral`).
- `EntryRow` recibe un `EntryRowText` ya resuelto (sujeto/nota/sin sujeto) y el monto ya formateado con signo.
- Para las capturas de revisión visual, `ConfirmDialogContent` está separado de la ventana del diálogo.

### Piezas que NO son componentes (composición propia de cada pantalla)

- Calendario del selector de fecha: cuadrícula de 7 columnas con celdas de 48 dp, los días posteriores a hoy atenuados y el día seleccionado con un círculo de `income`.
- Lista de sugerencias de sujeto (máximo 5, texto `Cuerpo`, filas de 48 dp).
- Lista de resultados de moneda (código en énfasis + nombre en secundario, filas de 48 dp, `check` en la seleccionada).
- Diálogos de renombrar cuenta, borrar cuenta (con conteo de registros) y error de importación: cada uno cambia de estructura. Sus botones sí son `TextButton`.

Detalles de comportamiento que el diseño asume:

- El botón «Ingresar» / «Egresar» usa el color de su tipo; el deshabilitado usa `surface` con texto `text.secondary`.
- El título de `EntryRow` cambia a gris (`text.secondary`) solo en el caso «Sin sujeto».
- El aviso usa los colores invertidos (fondo `text.primary`, texto `bg`), que en Material equivalen a `inverseSurface`. Dura unos 2 s; confirmar el ajuste de duración con la API al implementar.
- Los montos y fechas se formatean según la configuración regional y la moneda de la cuenta (SPEC §6); el diseño muestra miles con punto y COP sin decimales solo como ejemplo.

---

## 7. Pantallas

18 tableros, todos en la página `Pantallas` de Penpot.

| Tablero | SPEC | Notas |
|---|---|---|
| 5.1 Inicio | §5.1 | Selector y «?» arriba; tres filas de acción abajo |
| 5.1 Inicio · sin cuentas | §5.1 | Solo la fila «Cuenta» centrada y «?» |
| 5.1 Inicio · selector abierto | §5.1 | Lista de cuentas con la seleccionada marcada y la opción «Cuentas» |
| 5.1 Inicio · aviso de guardado | §5.2 | Aviso «Ingreso guardado» sobre las acciones |
| 5.2 Ingreso | §5.2 | Monto, Fecha, Sujeto, Nota, botón «Ingresar» |
| 5.2 Egreso | §5.2 | Igual que Ingreso, con el color y el botón de egreso |
| 5.2 Ingreso · sugerencias de sujeto | §5.2 | Hasta 5 sugerencias bajo el campo Sujeto |
| 5.2 Selector de fecha | §5.2 | Calendario; días posteriores a hoy deshabilitados |
| 5.3 Historial | §5.3 | Separador de mes, encabezado de día, filas, caso «Sin sujeto» |
| 5.3 Editar registro | §5.3 | Mismos campos que la captura, más «Borrar» con ícono |
| 5.3 Confirmar borrado de registro | §5.3 | «Definitivo, sin deshacer» |
| 5.4 Cuentas | §5.4 | Lista de cuentas (nombre y moneda); abajo Nueva cuenta, Exportar e Importar como filas de acción |
| 5.4 Crear cuenta | §5.4 | Nombre y buscador de moneda; aviso de moneda inmutable |
| 5.4 Crear cuenta · nombre duplicado | §4, §5.4 | Línea y mensaje de error en rojo; botón «Crear» deshabilitado |
| 5.4 Renombrar cuenta | §5.4 | Diálogo con el nombre actual |
| 5.4 Borrar cuenta | §5.4 | Confirmación fuerte con el conteo de registros que se pierden |
| 5.4 Error de importación | §7 | Mensaje claro; no se cambió ningún dato |
| 5.5 Ayuda | §5.5 | Versión, mantenedor, licencia y enlace al repositorio. El botón «Repositorio en GitHub» (con ícono de enlace externo) es clickeable y abre `https://github.com/v4atu/BandTracker` con `ACTION_VIEW`; no se muestra la URL en pantalla. Sin navegador, se captura `ActivityNotFoundException` y se muestra la URL como texto (SPEC §5.5) |

---

## 8. Criterios de usabilidad aplicados

El diseño se revisó contra las 10 heurísticas de Nielsen y contra las pautas de calidad y accesibilidad de Android:

- [Core app quality guidelines](https://developer.android.com/docs/quality-guidelines/2021/02): áreas táctiles de 48 dp, contraste de 4.5:1 (texto) y 3:1 (gráficos), tema oscuro y claro, patrones estándar de Android.
- [Accesibilidad en Compose](https://developer.android.com/develop/ui/compose/accessibility/api-defaults) y [Icons](https://developer.android.com/develop/ui/compose/graphics/images/material): tamaño mínimo táctil y `contentDescription`.
- Zona del pulgar (Steven Hoober), solo como guía: [Smashing Magazine](https://smashingmagazine.com/2016/09/the-thumb-zone-designing-for-mobile-users) y [UXmatters](https://www.uxmatters.com/mt/archives/2013/02/how-do-users-really-hold-mobile-devices.php). El propio autor aclara que su mapa original no es el único estándar.

| Heurística de Nielsen | Cómo se aplica |
|---|---|
| 1. Visibilidad del estado del sistema | Aviso breve al guardar; botón de captura deshabilitado mientras el monto es 0 |
| 2. Relación con el mundo real | Lenguaje cotidiano («Ingreso», «Egreso», «Historial»), montos con signo |
| 3. Control y libertad del usuario | Botón Atrás estándar de Android; confirmación antes de cualquier borrado |
| 4. Consistencia y estándares | Una sola fila de acción, un solo botón de texto, íconos del mismo set y trazo |
| 5. Prevención de errores | Fechas futuras bloqueadas, monto obligatorio, moneda inmutable, nombre único |
| 6. Reconocer antes que recordar | Ícono + etiqueta en cada acción; nunca solo ícono |
| 7. Flexibilidad y eficiencia | Acciones principales a un toque, en la zona del pulgar; sugerencias de sujeto |
| 8. Diseño estético y minimalista | Fondo plano, sin sombras ni bordes, un solo acento de color por fila |
| 9. Ayudar a reconocer y recuperarse de errores | Mensajes en lenguaje claro («Ya existe una cuenta con ese nombre», error de importación) |
| 10. Ayuda y documentación | Botón de ayuda siempre visible en Inicio |

---

## 9. Cómo mantener diseño y código sincronizados

- Si cambia un color, una tipografía o un espaciado: se cambia primero el **token** en Penpot y luego su equivalente en Kotlin, usando las tablas de este documento.
- Si se agrega un componente: se registra en Penpot con su dato `compose` y se agrega una fila en §6.
- Si el comportamiento cambia: se actualiza primero `SPEC.md`.

### Notas para editar en Penpot

- Volver a aplicar un token que una forma ya tiene **lo quita** (alterna). Si el color queda fijo en hex, el ícono no cambiará con el tema Claro.
- Al cambiar el ícono de una instancia con *Swap component*, el color vuelve al del ícono original: hay que re-aplicar el token de color del trazo.
- Los botones de texto se ajustan al ancho de su texto, pero tras editar el texto de una instancia puede hacer falta forzar el ajuste.

---

## 10. Pendientes

Resueltos:

- **Color dinámico:** se desactiva; la app usa su propia paleta (§2).
- **Licencia de Istok Web:** OFL 1.1, se puede empaquetar cumpliendo las condiciones de §3.
- **Licencia de los íconos:** Lucide (ISC, con parte MIT de Feather); texto guardado en `docs/licenses/` (§5).
- **Contraste:** todos los pares medidos pasan (§2), incluido el texto sobre el botón de egreso en tema claro (6.54).
- **Mantenedor en Ayuda:** `v4atu` (nombre definitivo). SPEC §9 ya quedó sin pendientes.
- **Enlace al repositorio:** la URL web es `https://github.com/v4atu/BandTracker` (el remoto SSH es `git@github.com:v4atu/BandTracker.git`). El botón queda sin mostrar la URL y es clickeable (§7).

Abiertos:

0. **Velo del diálogo:** el tablero de Penpot dibuja el diálogo sobre el fondo plano, sin velo. En Android `Dialog` atenúa la pantalla de atrás con el valor estándar de la plataforma; confirmar si se prefiere así (recomendado: es lo esperado en un diálogo modal) o sin atenuar.

1. **Contraste en la app real:** a cargo del mantenedor, con la app implementada.
2. ~~**Importación de los íconos**~~ **Resuelto (capa 4):** los 14 `VectorDrawable` se generaron desde la geometría de los componentes de Penpot (rectángulos y elipses con trazo centrado, como Lucide; Penpot los dibuja con trazo interior) y se comprobó en un dispositivo, dibujándolos, que se conservan el grosor de 2, las puntas y uniones redondeadas y el vacío interior (`IconRenderTest`).
3. **Textos nuevos:** «Ingreso guardado», «Egreso guardado» y «Ya existe una cuenta con ese nombre» son propuestas, no cadenas finales; revisar antes de pasarlas a `strings.xml`.
4. **Duración del aviso:** ~2 s; confirmar cómo ajustarla con la API de Snackbar al implementar.
5. **Versión en Ayuda:** «1.0.0» es un valor de muestra.
6. **Idioma inglés:** el diseño está en español; las cadenas en inglés solo se verifican en código (por ejemplo, «No subject»).
7. **Textos de ejemplo:** «Account 1», «Viaje» y los montos son datos de muestra; no son cadenas finales.
8. **Controles de editar y borrar en la tarjeta de cuenta (pendiente de diseño):** V4atu decidió que cada tarjeta de «5.4 Cuentas» lleve dos íconos, editar y borrar. Hace falta diseñarlos en Penpot (el tablero no los tiene, la tarjeta mide 52 dp), sumar el ícono Lucide de lápiz (los 14 actuales no lo incluyen) y conectarlos: los diálogos «Renombrar cuenta» y «Borrar cuenta» y su lógica ya están implementados y probados, falta solo el acceso desde la tarjeta.
9. **Fila «Licencias de terceros» en Ayuda:** decidida por V4atu, no está en el tablero «5.5 Ayuda»; se implementó como un cuarto campo con chevron. Falta dibujarla en Penpot.
10. **Resumen de importación exitosa:** diálogo de un solo botón decidido por V4atu; no hay tablero. Textos propuestos (es/en) en `strings.xml`.
11. **Medidas de Penpot que se siguieron y conviene confirmar:** la tarjeta de cuenta pasó de 56 a 52 dp (decisión de V4atu); el monto de captura muestra `$ 0.00` (ceros según la moneda y la región) en vez del `$ 00.00` de muestra del tablero y con el mismo color que el texto escrito; el primer día de la semana del selector de fecha sigue la región del dispositivo y las iniciales de los días son las del sistema (en español el miércoles es «X», Penpot dice «M»).
12. **Versión:** Ayuda muestra la `versionName` real de la app (hoy `1.0`), no el `1.0.0` de muestra.
