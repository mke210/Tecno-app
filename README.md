# Technology Zaachila — App de Taller

App nativa Android (Kotlin) para el servicio técnico de **Technology Zaachila**
(Guelache 301 · 951 394 5266). Es la versión Android de tu sistema web
(`Technology_Zaachila.html`): mismos campos, mismo formato de ticket, mismo
aviso legal, misma colección de Firestore (`remisiones`) — así que si ya
tienes datos guardados desde el HTML, esta app los va a leer y editar
directamente, sin migrar nada.

## Qué incluye (igual que tu HTML, adaptado a app nativa)

**Pestaña Nuevo**
- Folio automático (3 dígitos + 2 letras) con botón para imprimir solo la etiqueta del folio.
- Nombre, teléfono, dirección del cliente.
- Equipo recibido (Laptop / PC / Celular / Tableta / Disco Duro / Monitor / Otro) + marca manual.
- Tipo de servicio (Formateo de Sistema / Cambio de Pieza / Mantenimiento Preventivo / Diagnóstico General) + falla(s).
- Anotaciones / diagnóstico inicial.
- Checkboxes: dejó cargador / solo equipo / dejó ambos.
- Refacciones y servicios extra dinámicos (nombre + costo, agregar/quitar).
- Anticipo y precio total **editables** (el total se sugiere automáticamente, pero tú lo ajustas).
- Fotos del equipo (cámara o galería), varias por remisión.
- Guardar en Firebase, vista previa del ticket, e imprimir directo en la miniprinter.

**Pestaña Historial**
- Lista en vivo de las remisiones (últimas 30), buscador por nombre/teléfono/folio.
- Tocar una remisión abre edición completa: todos los campos, refacciones, fecha de entrega.
- Reimprimir el ticket actualizado o eliminar la remisión.

**Pestaña Config**
- **🖨️ Impresoras** — sección real (ya no es un botón de prueba suelto):
  lista los dispositivos Bluetooth emparejados, eliges cuál es tu miniprinter,
  puedes probar la impresión, ver el estado del Bluetooth, y "olvidar" la
  impresora guardada para cambiarla.
- **📦 Respaldo** — exportar todas las remisiones a un `.json` (eliges dónde
  guardarlo: Drive, almacenamiento del celular, etc.) e importar un `.json`
  de vuelta.

## El módulo de impresión Bluetooth propio

`BluetoothPrinterHelper.kt` sigue sin depender de librerías externas: abre un
`BluetoothSocket` RFCOMM estándar (perfil SPP, el que usa la enorme mayoría de
miniprinters térmicas de 58mm/80mm) y envía los comandos ESC/POS crudos. Lo
que se ajustó para esta versión:

- **Reintento automático**: si la primera conexión falla (muy común en
  miniprinters chinas al despertar de reposo), reintenta una vez más antes de
  reportar error.
- **Verificación de Bluetooth activado** antes de intentar imprimir, con
  aviso claro en vez de un error críptico.
- **Mensajes de error traducidos** a español y accionables ("verifica que
  esté encendida y con papel", "falta el permiso de Bluetooth", etc.)
  en lugar del mensaje crudo de Android.
- **Impresión de prueba** (`imprimirPrueba`) para la nueva pestaña de
  Configuración, así puedes confirmar que la miniprinter responde sin tener
  que llenar una remisión completa.
- **Impresión de solo folio** (`imprimirFolio`) para la etiqueta rápida, igual
  que el botón "🏷️ Imprimir Folio" del HTML.
- El texto del ticket (`TicketGenerator.kt`) es el mismo formato que
  `generarTicketTexto()` de tu HTML, incluyendo el aviso legal de 3 meses,
  ahora convertido a comandos ESC/POS con encabezado en negritas/tamaño doble.

Si tu miniprinter usa un protocolo distinto al SPP clásico (por ejemplo,
BLE puro en vez de Bluetooth clásico — algunos modelos "Goojprt" o "Phomemo"
recientes), dime la marca/modelo exacto y ajusto `BluetoothPrinterHelper.kt`
para ese protocolo específico.

---

## Paso 1 — Crear el proyecto en Firebase (gratis)

1. Ve a https://console.firebase.google.com y crea un proyecto, por ejemplo
   `technology-zaachila`.
2. Agrega una app **Android** con el paquete exacto: `com.technology.taller`
3. Descarga el **`google-services.json`** real y súbelo a la carpeta `app/`
   de este proyecto (reemplazando o junto a `google-services.json.EJEMPLO`,
   que debes borrar o ignorar).
4. En **Authentication → Sign-in method**, activa **Anónimo** (la app abre
   sesión anónima sola, nadie necesita loguearse).
5. En **Firestore Database**, crea la base de datos (modo producción, región
   más cercana).
6. En **Firestore → Reglas**, pega el contenido de `firestore.rules` de este
   proyecto y publica.

> Si ya usas el HTML con un proyecto de Firebase existente, usa exactamente
> ese mismo proyecto (mismo `google-services.json` / misma `firebaseConfig`)
> para que la app y la página web compartan los mismos datos en tiempo real.

## Paso 2 — Subir el proyecto a GitHub

1. Crea un repositorio (puede ser privado) y sube todos los archivos de esta
   carpeta, incluyendo tu `google-services.json` real dentro de `app/`.
2. En la pestaña **Actions**, el flujo "Compilar APK" corre solo en cada
   push a `main`.

## Paso 3 — Descargar el APK

1. Cuando el workflow termine (✅ en Actions), ve a la pestaña **Releases**.
2. Descarga `app-debug.apk` desde tu celular e instálalo (activa "orígenes
   desconocidos" la primera vez).
3. Cada push genera un nuevo Release con el APK actualizado.

## Paso 4 — Configurar tu miniprinter

1. Empareja la miniprinter desde los ajustes de Bluetooth del celular.
2. Abre la app → pestaña **Config** → sección **🖨️ Impresoras** → tócala en
   la lista para dejarla como "en uso" → toca **Probar impresión** para
   confirmar que imprime.

---

## Estructura del proyecto
```
app/src/main/java/com/technology/remision/
  Nota.kt                    -> modelo de datos (igual a los campos del HTML)
  FolioGenerator.kt          -> folio automático (3 dígitos + 2 letras)
  TicketGenerator.kt         -> texto del ticket + aviso legal
  PhotoUtils.kt               -> fotos <-> Base64 comprimido
  FirebaseHelper.kt          -> CRUD, búsqueda y respaldo JSON en Firestore
  BluetoothPrinterHelper.kt  -> plugin propio de impresión ESC/POS
  MainActivity.kt            -> contenedor de las 3 pestañas (ViewPager2)
  ViewPagerAdapter.kt
  NuevoFragment.kt           -> pestaña "Nuevo"
  HistorialFragment.kt       -> pestaña "Historial"
  HistorialAdapter.kt
  ConfigFragment.kt          -> pestaña "Config" (Impresoras + Respaldo)
  DispositivoAdapter.kt      -> lista de impresoras Bluetooth emparejadas
  EditarNotaActivity.kt      -> editar/reimprimir una remisión existente
```

## Próximos pasos sugeridos
- Firma de release (keystore) si algún día quieres publicarla en Google Play.
- Botón de "compartir por WhatsApp" además de imprimir.
- Subir las fotos a Firebase Storage en vez de Base64 si empiezas a manejar
  muchas fotos por remisión (por ahora se comprimen a ~700px para no pasar
  el límite de 1MB por documento de Firestore).

Dime si tu miniprinter necesita un protocolo distinto al ESC/POS estándar, o
si quieres que agregue alguna de estas.
