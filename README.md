# BibleSearchUM

Aplicación Android para consultar versículos de la Biblia (Reina-Valera 1960). Es una práctica de laboratorio de **Desarrollo de Aplicaciones Móviles**.

El usuario elige **Libro → Capítulo → Versículo** con tres menús desplegables dependientes. La app consulta la API REST de [Bolls Bible](https://bolls.life) y muestra el texto en una tarjeta de Material 3.

## Tecnologías

- Kotlin 2.0 + Jetpack Compose + Material 3 (`ExposedDropdownMenuBox`)
- Arquitectura **MVVM**: `ViewModel` + `StateFlow` inmutable (`asStateFlow`)
- **Retrofit 2** + Gson + **Kotlin Coroutines** (`viewModelScope`). Las llamadas de red nunca bloquean el hilo principal.
- `collectAsStateWithLifecycle` para observar el estado desde Compose
- Tests unitarios del ViewModel con JUnit 4 y `kotlinx-coroutines-test`

`minSdk 26` · `targetSdk 35` · AGP 8.7.3 · Gradle 8.11.1

## Estructura

```
app/src/main/java/com/example/biblesearchum/
├── MainActivity.kt                 Punto de entrada: tema + BibleSearchScreen
├── model/
│   └── BibleModels.kt              BibleBook, BibleVerse y limpieza del texto
├── network/
│   └── BibleApiService.kt          Interfaz Retrofit + RetrofitInstance
└── ui/
    ├── BibleUiState.kt             Estado inmutable de la pantalla
    ├── BibleViewModel.kt           Lógica, corrutinas y manejo de errores
    ├── BibleSearchScreen.kt        UI en Compose (Loading / Error / Success)
    ├── components/
    │   └── DropdownSelector.kt     Dropdown genérico reutilizable
    └── theme/                      Colores, tipografía y tema Material 3
```

El flujo de datos va en una sola dirección. La UI llama a funciones del ViewModel (`onBookSelected`, `onChapterSelected`, `onVerseSelected`, `retry`, `clearSelection`). El ViewModel llama a la API dentro de `viewModelScope.launch` y publica un nuevo `BibleUiState` en el `StateFlow`. Compose se recompone con ese estado. **No hay ninguna llamada de red en la capa de Compose.**

## API utilizada

| Uso | Endpoint |
|---|---|
| Lista de libros | `GET https://bolls.life/get-books/RV1960/` |
| Versículos de un capítulo | `GET https://bolls.life/get-text/RV1960/{libro}/{capítulo}/` |
| Un versículo | `GET https://bolls.life/get-verse/RV1960/{libro}/{capítulo}/{versículo}/` |

## Correcciones respecto al enunciado

Partí del código de referencia de la práctica. Al probarlo contra la API real aparecieron varios fallos, que corregí así:

| Problema en el enunciado | Corrección |
|---|---|
| La traducción `RVR1960` **no existe** en Bolls: la API responde `There is no such translation: RVR1960` y la app no cargaba nada. | Se usa el código correcto, `RV1960`, definido en una constante (`TRANSLATION`). |
| La lista de versículos se fijaba en 1–150. Salmos 119 tiene **176** versículos, y la mayoría de capítulos tiene muchos menos, así que se ofrecían versículos inexistentes. | Al elegir capítulo se consulta `get-text` y el menú muestra solo los versículos reales. |
| Algunos textos traen etiquetas HTML (`<br>`) y espacios dobles. | `toPlainVerseText()` limpia el texto antes de mostrarlo. |
| Si el usuario cambiaba rápido de selección, una respuesta lenta podía pisar a la más reciente. | Cada petición guarda su `Job`, y la anterior se cancela al cambiar la selección. |
| `catch (e: Exception)` dentro de una corrutina también atrapa `CancellationException`. | La cancelación se relanza. |
| Los errores mostraban el mensaje técnico de la excepción. | Mensajes claros según el tipo de error (sin conexión, código HTTP o error inesperado) y botón **Reintentar**. |
| Al elegir otro versículo seguía visible el texto anterior mientras cargaba. | El texto se limpia al cambiar de versículo. |
| Los menús se podían abrir vacíos (por ejemplo, mientras cargaban los libros). | El dropdown se desactiva si no tiene opciones. |
| `Modifier.menuAnchor()` sin argumentos está obsoleto en Material 3 1.3. | Se usa `menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled)`. |
| El paquete de ejemplo `com.example.blesearchum` tenía una errata. | Se usa `com.example.biblesearchum`. |

## Cómo ejecutarla

1. Clona el repositorio y ábrelo en Android Studio (Ladybug o superior).
2. Espera a que termine el *Gradle Sync*.
3. Ejecuta la configuración `app` en un emulador o dispositivo con Internet.

También se puede compilar desde la terminal:

```bash
./gradlew assembleDebug        # genera app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # ejecuta los tests unitarios
```

Cada *push* ejecuta el workflow de GitHub Actions, que pasa los tests y genera el APK de depuración. El APK se descarga como artefacto desde la pestaña **Actions**.

## Prueba manual

1. Al abrir la app aparece un indicador de carga mientras se descargan los 66 libros.
2. Elige un libro y se habilita **Capítulo**.
3. Elige un capítulo y se cargan sus versículos reales; se habilita **Versículo**.
4. Elige un versículo y aparece la tarjeta con la cita y el texto.
5. Si cambias el libro o el capítulo, se limpian los valores que dependen de él.
6. El botón ⟳ de la barra superior reinicia la selección.
7. En modo avión aparece un mensaje de error con el botón **Reintentar**.

## Rúbrica

| Criterio | Cómo se cumple |
|---|---|
| Conexión de red | Retrofit con funciones `suspend`, llamadas dentro de `viewModelScope.launch` |
| Arquitectura MVVM | Paquetes `model` / `network` / `ui`; `StateFlow` expuesto con `asStateFlow()`; nada de red en Compose |
| Reactividad de dropdowns | Libro → Capítulo → Versículo; cada cambio limpia los valores posteriores (cubierto por tests) |
| Manejo de errores y UX | `CircularProgressIndicator`, tarjeta de error con *Reintentar*, mensaje inicial y diseño Material 3 |
