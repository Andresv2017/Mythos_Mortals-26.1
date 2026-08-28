# Barra de vida (boss bar) del Minotauro

Fecha: 2026-08-27

## Objetivo

Dar al Minotauro una boss bar propia en pantalla, dibujada con las tres texturas de
`~/Downloads/new_textures` (`mino_bar_base`, `mino_bar_full`, `mino_bar_overlay`), visible por
proximidad y sin el texto del nombre que vanilla dibuja centrado.

Hoy el mod no tiene ninguna boss bar: no hay `BossEvent` en ningún archivo, ni carpeta
`assets/mythosmortals/textures/gui/`. Es una funcionalidad nueva completa.

## Contexto verificado

Comprobado contra el classpath real del proyecto —
`build/moddev/artifacts/minecraft-patched-26.1.0.19-beta.jar` y
`neoforge-26.1.0.19-beta-universal.jar` — porque condiciona el diseño. **Aviso para quien lea
esto luego:** las cachés `ng_execute` de descompilado contienen versiones anteriores (usan
`ResourceLocation` y no `Identifier`); consultarlas da firmas equivocadas. Verificar siempre
contra el jar del classpath.

1. **Existe el gancho de cancelación.** `BossHealthOverlay.render` llama a
   `net.neoforged.neoforge.client.ClientHooks.onCustomizeBossEventProgress(...)` y, si el evento
   se cancela, **se salta tanto `extractBar` como el bloque que dibuja el nombre** (comprobado en
   el bytecode: el `isCanceled()` compila a un `ifne` que salta por encima de ambos). Es decir,
   cancelar nos da gratis el requisito de "sin nombre".
2. **`CustomizeGuiOverlayEvent.BossEventProgress`** es `ICancellableEvent` y expone
   `getBossEvent()` (un `LerpingBossEvent`), `getGuiGraphics()`, `getX()`, `getY()`,
   `getIncrement()` y `setIncrement(int)`.
3. **Nombres de API propios de 26.1**, distintos de versiones anteriores:
   - `GuiGraphics` se llama aquí **`GuiGraphicsExtractor`** (`net.minecraft.client.gui`), y es lo
     que devuelve `getGuiGraphics()`.
   - `blit` exige un `RenderPipeline` como primer argumento; para GUI con transparencia,
     `RenderPipelines.GUI_TEXTURED`. La sobrecarga útil es
     `blit(pipeline, Identifier, x, y, u, v, w, h, texW, texH)`.
   - `ServerBossEvent` **no** tiene el constructor de 3 argumentos: es
     `ServerBossEvent(UUID, Component, BossBarColor, BossBarOverlay)`.
   - `ResourceLocation` es `Identifier`.
4. **La geometría encaja al píxel.** Vanilla dibuja la barra en `x = guiWidth()/2 - 91`, o sea
   182 px de ancho — exactamente el ancho de las tres texturas.
5. **Layout de las texturas** (medido por ocupación de filas, no supuesto):

   | filas | contenido | ancho ocupado |
   |---|---|---|
   | y 0–9 | medallón (cabeza de minotauro) | x79–100 |
   | y 10–14 | **banda de la barra** | x0–181 |
   | y 15–16 (+17 sólo en `full`) | mandíbula del medallón | x84–95 |

   `base` (182×17) y `full` (182×18) son **pixel a pixel el mismo layout**; sólo cambia el color.
   `overlay` (182×3) es la fila de muescas, con hueco central para el medallón.

   La banda de 5 px (filas 10–14) coincide con la altura de la barra vanilla. El medallón
   sobresale ~10 px por encima y ~2–3 px por debajo.
6. **El recorte horizontal enciende el medallón.** Como el medallón vive en x79–100 y `base`/`full`
   comparten layout, recortar `full` por la izquierda según la vida hace que el medallón pase de
   apagado a encendido al cruzar el 50 %. Es un efecto intencionado del arte, no un accidente.
7. **`clientTrackingRange(10)`** en `MythosMortalsEntities:62` = 160 bloques. Un radio de 48 queda
   holgadamente dentro: todo jugador que vea la barra tendrá la entidad cargada en cliente.
8. **La clave de idioma ya existe**: `entity.mythosmortals.minotaur` = `"Minotaur"` en el
   `en_us.json` generado. No hace falta añadir nada al `Lang` de `MythosMortalsDatagen`.
9. **Precedente de GUI en el mod**: `PegasusInventoryScreen` y `OwlPerchHand`. No hay todavía
   ningún suscriptor de eventos de HUD.

## Diseño

### Assets

Las tres PNG a `src/main/resources/assets/mythosmortals/textures/gui/`:

- `mino_bar_base.png` (182×17)
- `mino_bar_full.png` (182×18)
- `mino_bar_overlay.png` (182×3)

Los `.aseprite` **no** se copian: son fuentes de autoría, no recursos de runtime. Si se quieren
versionar, van fuera de `src/main/resources`.

Van como texturas directas bajo `textures/gui/`, no bajo `textures/gui/sprites/`: como sprites de
atlas exigirían `.mcmeta` y un `SpriteSource`, y aquí se dibujan con `blit` directo.

### Servidor — `MinotaurEntity`

Un `ServerBossEvent` como campo de la entidad:

- Nombre: `Component.translatable("entity.mythosmortals.minotaur")`, reutilizando la clave que ya
  existe. **Nunca se dibuja** (cancelamos el texto de vanilla); su única función es servir de marca
  para que el cliente reconozca cuál de las barras en pantalla es la nuestra.
- Color y overlay vanilla: irrelevantes, porque no usamos el dibujado de vanilla. Se fijan a
  `RED` / `PROGRESS` por coherencia.

Cada tick de servidor:

- `bossEvent.setProgress(getHealth() / getMaxHealth())`
- Reconciliar la lista de jugadores: añadir los `ServerPlayer` a ≤ `BOSS_BAR_RADIUS` bloques,
  quitar los que salgan del radio.

Al morir o al retirarse la entidad, `bossEvent.removeAllPlayers()`.

`BOSS_BAR_RADIUS = 48.0` (bloques) como constante en `MinotaurCtx`, junto al resto de valores de
ajuste del mob. Se compara al cuadrado para no calcular raíces por tick.

### Cliente — `MinotaurBossBarRenderer`

Clase nueva en `net.darkblade.mythosmortals.entity.minotaur.client`, anotada
`@EventBusSubscriber(modid = MythosMortals.MODID, value = Dist.CLIENT)`.

Suscriptor de `CustomizeGuiOverlayEvent.BossEventProgress`:

1. **Identificar la barra.** Si el `getName()` del `LerpingBossEvent` no es un `TranslatableContents`
   con clave `entity.mythosmortals.minotaur`, salir sin tocar nada. Las boss bars de cualquier otro
   mob o mod siguen dibujándose con normalidad.
2. **Cancelar el evento.** Vanilla no pinta ni la barra ni el nombre.
3. **Pintar las tres capas** en `getX()` (que ya es `guiWidth()/2 - 91`), con un desplazamiento
   vertical tal que **la banda (filas 10–14) caiga donde vanilla habría puesto su barra de 5 px**.
   Como la banda empieza en la fila 10 de la textura, eso es dibujar el origen del arte en
   `getY() - 10`. Así la banda queda exactamente en `getY()` y el medallón sobresale hacia arriba:
   1. `base` completa en `(getX(), getY() - 10)`,
   2. `full` en la misma posición, recortada a `round(182 × progreso)` px desde la izquierda,
   3. `overlay` encima, en `(getX(), getY() - 10 + BANDA_Y + 1)` con `BANDA_Y = 10` — es decir
      centrado en la banda de 5 px (ver *Supuestos*).
4. **`setIncrement(getIncrement() + 10)`.** El arte ocupa 10 px por encima del ancla vanilla
   (el medallón) frente a los 0 de vanilla, así que la siguiente boss bar necesita justo esos
   10 px extra de separación para no solaparse con el medallón.

## Decisiones y alternativas descartadas

**Sobrescribir los sprites vanilla de boss bar** — descartado. Están fijados por color
(`boss_bar/red_background`, `boss_bar/red_progress`, …); sustituirlos cambiaría la barra de **todos**
los jefes del juego, no sólo la del Minotauro.

**HUD puramente de cliente, sin `BossEvent`** — descartado. Ahorra la red, pero obliga a
reimplementar a mano lo que el `ServerBossEvent` da hecho: a quién se le muestra, sincronización,
qué pasa con varios minotauros, ciclo de vida (muerte, descarga de chunk, desconexión) y apilado
con otras boss bars.

**Identificar la barra por UUID sincronizado** — descartado por innecesario. La comprobación de la
clave de traducción del nombre es suficiente y no requiere paquetes propios, dado que la única
fuente de barras con ese nombre somos nosotros.

## Supuestos

- **Altura del `overlay`.** Mide 3 px y la banda 5; el arte no fija a qué altura exacta va. Se asume
  **centrado en la banda**. Es una única constante y se mueve un píxel si no era la intención.
- **Sentido del relleno.** Se asume anclado a la izquierda (drena de derecha a izquierda, como
  vanilla). El punto 6 de *Contexto verificado* lo respalda: es lo que hace que el medallón se
  encienda al cruzar el 50 %.
- **`overlay` siempre a ancho completo**, decorativo, sin segmentar por fases. Las muescas de la
  textura son una fila continua de marcas, no separadores de fase.

## Verificación

- `compileJava` en verde.
- En juego:
  - acercarse y alejarse del Minotauro: la barra aparece y desaparece al cruzar los 48 bloques;
  - bajarle la vida y comprobar que el medallón se enciende al pasar del 50 %;
  - matarlo: la barra desaparece;
  - tener a la vista otra boss bar cualquiera (Wither, `/bossbar`) y confirmar que se sigue
    dibujando con su aspecto vanilla y su nombre, y que no se solapa con la del Minotauro.
