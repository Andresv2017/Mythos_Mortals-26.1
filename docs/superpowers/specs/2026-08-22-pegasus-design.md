# Pégaso y las Bridas de Atenea — Diseño

**Fecha:** 2026-08-22
**Mod:** Mythos & Mortals (`mythosmortals`) — NeoForge 26.1
**Estado:** Aprobado, listo para plan de implementación

---

## 1. Resumen

El Pégaso es una montura voladora mítica de élite: asustadiza, de vuelo alto y gran
movilidad aérea. No combate — solo huye volando. Domarlo es un ritual de dos fases
(tierra y aire) que exige preparación previa: manzanas suficientes y las **Bridas de
Atenea**, un ítem que solo se obtiene explorando estructuras.

Una vez domado se equipa con silla, bridas y armadura, y se convierte en la única
montura de vuelo libre tridimensional del mod, con un **Impulso de Viento** (dash aéreo)
en cooldown.

### Assets de partida

Todo el material visual ya existe en `C:\Users\Usuario\Downloads\Pegasus`:

- `Pegasus (2).bbmodel` — modelo 256×256. Jerarquía de huesos:
  `pegasus → body → {top → {neck → head → reins, tail, belly, wing3 → wing4, wing2 → wing5}, Leg, Leg2, Leg3, Leg4}`
- `PegasusAnims.java` — 12 `AnimationDefinition`: `IDLE`, `WALK`, `SPRINT`, `FLY_IDLE`,
  `FLY`, `FLY_SPRINT`, `LANDING`, `TAKE_OFF`, `DEATH`, `FALL`, `HIT_GROUND`, `TAME_FAIL`
- Texturas de entidad 256×256: `pegasus.png`, `pegasus_bridle_layer.png`,
  `pegasus_saddle_layer.png`, `pegasus_{iron,gold,diamond,netherite}_armor_layer.png`
- Texturas de ítem 16×16: `athena_bridle.png`, `pegasus_saddle.png`, `arpy_feather.png`

`pegasus_all.png` es un atlas de referencia del autor y no se usa en runtime.

---

## 2. Decisiones de diseño tomadas

| Tema | Decisión |
|---|---|
| Equipamiento | Completo: silla propia + Bridas de Atenea + armadura de 4 tiers |
| Fallo en la fase aérea | Te lanza al suelo y el Pégaso **se resetea a salvaje**, pero se queda en la zona |
| Inicio de la fase aérea | Auto-monta al jugador y despega en cuanto se completa la fase de tierra |
| Fuentes de las Bridas | Solo cofres: `athenas_ruins` (existente) + `minotaur_arena` (nueva tabla) |
| Modelo de vuelo | Vuelo libre 3D + dash con cooldown. Sin estamina |
| Spawn | Biomas de montaña, raro y solitario |
| Combate | Ninguno. El Pégaso nunca ataca; solo huye volando |
| `arpy_feather` | Se registra como drop del **Arpy** y se añade a tablas de cofres |
| Doma en tierra | Probabilidad incremental estilo caballo vanilla |
| Clase base | `AbstractFlyingEntity` de deluxelib, moviendo `implements Enemy` a las subclases |

---

## 3. Arquitectura

### 3.1 Cambio previo en deluxelib

`net.darkblade.deluxelib.entity.AbstractFlyingEntity` declara hoy
`extends PathfinderMob implements Enemy`. Un mob `Enemy` se elimina en dificultad
Pacífica y varios sistemas lo tratan como hostil, lo cual es incorrecto para un Pégaso
pacífico y montable — y también para el Búho, que ya está registrado como
`MobCategory.CREATURE` pese a heredar `Enemy`.

**Cambio:** quitar `implements Enemy` de `AbstractFlyingEntity` y añadirlo explícitamente
a `ArpyEntity` (`content/arpy/ArpyEntity.java`), la única subclase que sí es hostil.
`OwlEntity` queda como criatura no hostil, coherente con su registro.

Requiere recompilar deluxelib desde `C:\Andrés\Deluxelib-Neoforge-26.1` con
`./gradlew publishToMavenLocal` antes de compilar el mod.

### 3.2 Paquete nuevo `content/pegasus/`

Cada archivo con una responsabilidad clara y comprobable por separado:

| Archivo | Responsabilidad | Depende de |
|---|---|---|
| `PegasusEntity.java` | Entidad. Orquesta estados, movimiento y montura | Todos los de abajo |
| `PegasusTameState.java` | Enum de estados de doma + serialización a `int` | — |
| `PegasusTaming.java` | Lógica pura de doma: temper, tiradas, temporizadores | `PegasusTameState` |
| `PegasusEquipment.java` | Contenedor de 3 slots + NBT + validación de ítems | Registro de ítems |
| `PegasusFlightController.java` | Traduce el `Input` del jinete a movimiento y dash | — |
| `PegasusAnimation.java` | Las 12 `AnimationDefinition` (copia de `PegasusAnims.java`) | — |
| `PegasusModel.java` | `ModelPart` + `createBodyLayer()` del bbmodel | — |
| `PegasusRenderer.java` | `FlyingMobRenderer`, selección de animación, pose del jinete | `PegasusModel` |
| `PegasusEquipmentLayer.java` | Capas visuales de bridas, silla y armadura | `PegasusModel` |
| `PegasusRiderPose.java` | Pose del jinete, patrón de `MinotaurRiderPose` | — |
| `input/PegasusDashInput.java` | `KeyMapping` del Impulso de Viento | Packet |
| `network/PegasusDashServerPacket.java` | Packet cliente → servidor del dash | — |

`PegasusTaming` y `PegasusFlightController` se separan de `PegasusEntity`
deliberadamente: son las dos piezas con lógica no trivial y se pueden razonar y ajustar
sin abrir la entidad, que ya de por sí crecerá.

### 3.3 Atributos

```
MAX_HEALTH        30.0
MOVEMENT_SPEED     0.25
FLYING_SPEED       0.70
FOLLOW_RANGE      24.0
STEP_HEIGHT        1.0
```

Registro en `MythosMortalsRegistry.CommonModEvents.onAttributes`.

### 3.4 Datos sincronizados (`SynchedEntityData`)

| Accessor | Tipo | Uso |
|---|---|---|
| `DATA_TAME_STATE` | `INT` | Ordinal de `PegasusTameState`; el cliente lo usa para elegir animación |
| `DATA_OWNER` | `OPTIONAL_UUID` | Dueño una vez domado |
| `DATA_HAS_SADDLE` | `BOOLEAN` | Capa visual de silla |
| `DATA_HAS_BRIDLE` | `BOOLEAN` | Capa visual de bridas |
| `DATA_ARMOR_TIER` | `INT` | 0 = ninguna, 1..4 = hierro/oro/diamante/netherita |
| `DATA_DASH_COOLDOWN` | `INT` | Ticks restantes, para feedback visual |

Los datos persistentes (`tameState`, `temper`, `owner`, contenido del contenedor) se
guardan en `addAdditionalSaveData` / `readAdditionalSaveData` con `ValueOutput`/`ValueInput`,
igual que `OwlEntity`.

---

## 4. Máquina de estados de la doma

```
                    jugador NO agachado a < 12 bloques
   ┌── WILD ───────────────────────────────────────────► huye volando + discard()
   │     │
   │     └── manzana (jugador agachado) ──► FEEDING
   │                                          │
   │                    temper += 1;  P(éxito) = 0.15 × temper
   │                                          │
   │        ┌── manzana dorada ───────────────┤ (éxito garantizado)
   │        │                                 │
   │        │   sin manzanas en inventario, 100 ticks ──► TAME_FAIL ──► huye + discard()
   │        │                                 │
   │        └──────────► tirada exitosa ──────┘
   │                            │
   │                            ▼
   │                        BONDED ── auto-monta + TAKE_OFF ──► BUCKING
   │                                                              │
   │                        clic derecho con Bridas (100%) ───────┼──► TAMED
   │                                                              │
   └──────────────── 300 ticks sin bridas: lanza al jinete ───────┘
```

### 4.1 `WILD` — aproximación y huida

- `FleeApproachingPlayerGoal`: busca al jugador más cercano en radio 12. Dispara solo si
  **no** está agachado (`player.isCrouching() == false`).
- Al dispararse: `beginTakeoff()`, ascenso vertical sostenido, y `discard()` cuando supera
  los 40 bloques sobre su posición de partida. El Pégaso se pierde para siempre.
- Con el jugador agachado el goal nunca dispara: puede acercarse hasta rango de interacción.

### 4.2 `FEEDING` — alimentación

- Clic derecho con `minecraft:apple` estando agachado y a rango: consume la manzana,
  `temper += 1`, partículas de corazón pequeñas y sonido de comer.
- Tirada de éxito tras cada manzana: `random.nextFloat() < 0.15f * temper`. En la práctica
  cae entre la 3ª y la 8ª manzana, con media ~5.
- `minecraft:golden_apple` (y `enchanted_golden_apple`): éxito inmediato, sin tirada.
- Al dar la primera manzana se guarda el UUID del alimentador en un campo transitorio
  `feederUuid` (no persiste al recargar; si el chunk se descarga durante `FEEDING`, el
  Pégaso vuelve a `WILD` con `temper` intacto).
- **Regla de abandono:** en cuanto `tameState == FEEDING` y el inventario del alimentador
  deja de contener manzanas, arranca `escapeTimer = 100` ticks. Al
  llegar a 0: animación `TAME_FAIL`, luego huye y `discard()` como en `WILD`.
  Si el jugador consigue una manzana antes de que expire, el temporizador se cancela.
- Si el jugador se aleja más de 16 bloques durante `FEEDING`, también arranca `escapeTimer`.

### 4.3 `BONDED` → `BUCKING` — la fase aérea

- Al éxito: corazones de doma, `player.startRiding(pegasus, true)` forzado (no requiere
  silla — el Pégaso te sube él), `beginTakeoff()` y animación `TAKE_OFF`.
- `BUCKING` dura 300 ticks (15 s):
  - Vuelo errático: el `SmoothFlyingMoveControl` recibe un destino que salta cada 20-30
    ticks a un punto aleatorio en un radio de 12 bloques, con cambios bruscos de altura.
  - Animación `FLY_SPRINT` como base, con `TAME_FAIL` mezclado en capa aditiva para el
    corcoveo.
  - `ScreenShake` de deluxelib sobre el jinete, en pulsos.
  - El jinete no tiene control: `getControllingPassenger()` devuelve `null` en `BUCKING`.
- **Éxito:** clic derecho con `athena_bridle` en la mano en cualquier momento de los 300
  ticks. Probabilidad 100%, sin tirada. La brida se consume del inventario y se coloca en
  el slot correspondiente. Transición a `TAMED`: corazones, el vuelo se estabiliza, el
  jinete pasa a tener control.
- **Fallo (300 ticks agotados):**
  - `removePassenger(player)`; el jugador cae y recibe daño de caída normal.
  - El Pégaso desciende, animaciones `FALL` y `HIT_GROUND` al tocar suelo.
  - `temper = 0`, `tameState = WILD`, sin dueño. **No despawnea**: se queda en la zona y
    puede volver a domarse desde cero (sigilo, manzanas, fase aérea).

### 4.4 `TAMED`

- `owner` guardado en NBT. Ya no huye del jugador (el `FleeApproachingPlayerGoal` se
  desactiva para cualquier jugador cuando `tameState == TAMED`).
- Sin dueño montado, usa los goals de vagabundeo/vuelo de `AbstractFlyingEntity`.
- **Otros jugadores:** solo el dueño puede montarlo y abrir su inventario. Un jugador
  distinto recibe `InteractionResult.FAIL` y un mensaje de acción
  (`pegasus.not_your_mount`). No hay transferencia de propiedad en esta iteración.
- **Persistencia:** al pasar a `TAMED` se llama `setPersistenceRequired()` para que no
  despawnee por distancia. En `WILD` y `FEEDING` sí puede despawnear con las reglas
  normales de `MobCategory.CREATURE`.

---

## 5. Equipamiento y montura

### 5.1 Reparto de roles

| Ítem | Efecto |
|---|---|
| `pegasus_saddle` | Permite montar y controlar al Pégaso **en tierra** (galope) |
| `athena_bridle` | Desbloquea el **vuelo**: despegue, control 3D y dash |
| Armadura de caballo vanilla (hierro/oro/diamante/netherita) | Reduce el daño recibido; capa visual por tier |

Para volar hacen falta **ambos**: silla y bridas. Un Pégaso domado con bridas pero sin
silla no puede montarse; con silla pero sin bridas galopa pero no despega bajo control
del jinete.

### 5.2 Contenedor

`PegasusEquipment` es un `SimpleContainer` de 3 slots con validación por slot:

0. silla → solo `mythosmortals:pegasus_saddle`
1. bridas → solo `mythosmortals:athena_bridle`
2. armadura → cualquier ítem del tag `minecraft:horse_armor`

Se abre con shift + clic derecho sobre un Pégaso domado del propio dueño, con un menú de
3 slots equivalente al de caballo. Cada cambio de contenido actualiza los
`EntityDataAccessor` de capa visual. El contenido se serializa completo en NBT y los ítems
se sueltan al morir el Pégaso.

Las bridas se pueden retirar del slot: hacerlo no revierte la doma (`tameState` sigue en
`TAMED`), solo deshabilita el vuelo hasta volver a ponerlas.

---

## 6. Vuelo montado

`getControllingPassenger()` devuelve al jinete si y solo si `tameState == TAMED`, el
jinete es el dueño y hay silla equipada.

### 6.1 Controles

| Entrada | Efecto |
|---|---|
| Mirada | Marca yaw y pitch del Pégaso (subes y bajas mirando) |
| `W` | Acelera hacia adelante en la dirección de vuelo |
| `S` | Frena |
| `Espacio` (en tierra) | Despegue vertical: `beginTakeoff()` + animación `TAKE_OFF`. Requiere bridas |
| `Espacio` (en vuelo) | Ascenso adicional |
| `Shift` | Descenso; mantenido cerca del suelo inicia el aterrizaje |
| `R` (configurable) | Impulso de Viento |

`R` es libre: el Búho ya ocupa `H` (`OwlPossessionInput`) y los tuners de desarrollo usan
el teclado numérico.

`PegasusFlightController` lee el `Input` del jinete y produce el `Vec3` de movimiento;
`SmoothFlyingMoveControl` suaviza el resultado, igual que en el resto de voladores del mod.

### 6.2 Impulso de Viento (dash)

- El cliente detecta la pulsación y envía `PegasusDashServerPacket` (registrado en
  `MythosMortalsRegistry.registerPackets`, patrón de los packets del Búho).
- El servidor valida: `tameState == TAMED`, el emisor es el jinete controlador, está en el
  aire y `dashCooldown == 0`.
- Efecto: impulso de magnitud 1.8 en la dirección de la mirada, `dashCooldown = 100` ticks
  (5 s), partículas de viento (`ParticleFx` de deluxelib) y sonido.
- `DATA_DASH_COOLDOWN` se decrementa en el servidor y se sincroniza para feedback visual.

### 6.3 Aterrizaje

Con jinete: `Shift` sostenido cerca del suelo dispara `beginLanding()` y la animación
`LANDING`. Sin jinete, el ciclo de `AbstractFlyingEntity` (`LandingGoal`, `TakeoffGoal`,
`FlightWanderGoal`) gobierna solo, con `getMinFlightAltitude()` y `getMaxFlightAltitude()`
sobrescritos para reflejar el vuelo alto del Pégaso (12 y 45 bloques).

El Pégaso nunca recibe daño de caída (`causeFallDamage` devuelve `false`, como el resto de
voladores de deluxelib).

---

## 7. Contenido nuevo

### 7.1 Ítems

Registrados en `MythosMortalsItems` con `ITEMS.registerSimpleItem` y añadidos a
`ARMORY_TAB` en `onBuildCreativeTabs`:

| Ítem | Textura | Propiedades |
|---|---|---|
| `mythosmortals:athena_bridle` | `athena_bridle.png` | `stacksTo(1)`, rareza `rare` |
| `mythosmortals:pegasus_saddle` | `pegasus_saddle.png` | `stacksTo(1)` |
| `mythosmortals:arpy_feather` | `arpy_feather.png` | Apilable, material |

Ninguno es crafteable. Cada uno necesita su `models/item/*.json` con `minecraft:item/generated`.

### 7.2 Entidad

```java
ENTITY_TYPES.register("pegasus",
    () -> EntityType.Builder.<PegasusEntity>of(PegasusEntity::new, MobCategory.CREATURE)
        .sized(1.4f, 2.0f)
        .clientTrackingRange(12)
        .build(...));
```

### 7.3 Spawn

En `MythosMortalsRegistry.registerGameplay()`:

```java
DeluxeBiomeSpawns.builder(() -> PEGASUS.get(), MobCategory.CREATURE)
    .spawnRate(5, 1, 1)
    .biomes(Biomes.MEADOW, Biomes.GROVE, Biomes.JAGGED_PEAKS,
            Biomes.STONY_PEAKS, Biomes.WINDSWEPT_HILLS)
    .submit();
```

Más una regla `checkSpawnRules` que exige `blockPosition().getY() >= 100` y cielo abierto
(`level.canSeeSky(pos)`).

### 7.4 Loot

**Bridas de Atenea** — peso bajo, solo estas dos fuentes:

- `data/mythosmortals/loot_table/chests/athenas_ruins.json`: entrada nueva
  `mythosmortals:athena_bridle`, peso 2 frente a los pesos 8-10 existentes.
- `data/mythosmortals/loot_table/chests/minotaur_arena.json`: **tabla nueva**, con
  `athena_bridle` (peso 3) más ítems temáticos de la guarida (bronce, tin, comida).

**Pluma de Arpía** — `mythosmortals:arpy_feather`:

- Drop del Arpy: entrada nueva en `MythosMortalsDatagen.Loot.addLootTables()` para
  `MythosMortalsRegistry.ARPY`, 1-2 unidades con `randomChance(0.6f)`.
- Cofres: añadida a `chests/arpy_nest.json` y a `chests/athenas_ruins.json`.

El Pégaso no tiene tabla de loot propia: al no combatir, matarlo no debe premiarse.

### 7.5 Assets a copiar

De `C:\Users\Usuario\Downloads\Pegasus` al proyecto:

```
textures/entity/pegasus/pegasus.png
textures/entity/pegasus/bridle_layer.png
textures/entity/pegasus/saddle_layer.png
textures/entity/pegasus/iron_armor_layer.png
textures/entity/pegasus/gold_armor_layer.png
textures/entity/pegasus/diamond_armor_layer.png
textures/entity/pegasus/netherite_armor_layer.png
textures/item/athena_bridle.png
textures/item/pegasus_saddle.png
textures/item/arpy_feather.png
```

### 7.6 Traducciones

`DeluxeLangProvider` genera los nombres automáticamente a partir de los ids. Se añaden a
mano solo la tecla del dash (`key.mythosmortals.pegasus_dash` → "Impulso de Viento") y su
categoría.

---

## 8. Render y animaciones

### 8.1 Registro

- `PegasusModel.LAYER_LOCATION` en `ClientModEvents.onRegisterLayers`.
- `PegasusRenderer::new` en `ClientModEvents.onRegisterRenderers`.
- `PegasusRenderer extends FlyingMobRenderer` (deluxelib), con `PegasusEquipmentLayer`
  añadida como capa y `applyRiderPose` delegando en `HumanoidPoseApplier` con
  `PegasusRiderPose.RIDER_POSE`, igual que `MinotaurRenderer`.

### 8.2 Adaptación del modelo exportado

`Pegasus (2).bbmodel` exporta la clase como `Pegasus (2)` con `ResourceLocation("modid", ...)`.
Al portarlo hay que: renombrar la clase a `PegasusModel`, usar
`Identifier.fromNamespaceAndPath(MythosMortals.MODID, "pegasus")` y adaptar la firma al
patrón de `MinotaurModel`/`CopperOwlModel` del proyecto.

El hueso `reins` del modelo base se oculta salvo que haya bridas equipadas — la capa
`bridle_layer` se pinta sobre él.

### 8.3 Mapeo de animaciones

| Estado | Animación |
|---|---|
| Quieto en tierra | `IDLE` |
| Caminando | `WALK` |
| Corriendo | `SPRINT` |
| Despegue | `TAKE_OFF` |
| Planeando sin avance | `FLY_IDLE` |
| Volando | `FLY` |
| Volando rápido / dash | `FLY_SPRINT` |
| Aterrizaje | `LANDING` |
| Cayendo | `FALL` |
| Impacto contra el suelo | `HIT_GROUND` |
| Nervios antes de huir; base del corcoveo en `BUCKING` | `TAME_FAIL` |
| Muerte | `DEATH` |

Se usa `MobAnimator` de deluxelib, con `PegasusEntity implements Animatable<PegasusEntity>`,
como `OwlEntity`.

**Nota:** antes de dar las animaciones por buenas conviene pasar `LANDING`, `FALL`,
`HIT_GROUND` y `DEATH` por la skill `anim-clip`, que detecta huesos que se hunden en el
suelo — son justo las poses de riesgo.

---

## 9. Puntos de integración

| Archivo existente | Cambio |
|---|---|
| `deluxelib/entity/AbstractFlyingEntity.java` | Quitar `implements Enemy` |
| `content/arpy/ArpyEntity.java` | Añadir `implements Enemy` |
| `registry/MythosMortalsRegistry.java` | `ENTITY_TYPES.register("pegasus", ...)`; atributos; spawn; packet del dash; layer definition y renderer |
| `registry/MythosMortalsItems.java` | Tres ítems nuevos + entradas en `ARMORY_TAB` |
| `registry/MythosMortalsDatagen.java` | Loot del Arpy; lang de la tecla del dash |
| `loot_table/chests/athenas_ruins.json` | Entradas de `athena_bridle` y `arpy_feather` |
| `loot_table/chests/arpy_nest.json` | Entrada de `arpy_feather` |
| `loot_table/chests/minotaur_arena.json` | Archivo nuevo |

---

## 10. Verificación

**Automática:** `./gradlew build` compila sin errores ni warnings nuevos, y `runData`
regenera lang y loot sin diferencias inesperadas.

**Manual in-game**, en este orden:

1. Un Pégaso aparece en un bioma de montaña por encima de Y 100.
2. Acercarse caminando a menos de 12 bloques: despega y desaparece.
3. Acercarse agachado hasta rango de interacción: no huye.
4. Alimentar con manzanas normales: se doma entre la 3ª y la 8ª.
5. Alimentar con manzana dorada: doma inmediata.
6. Dar una manzana y vaciar el inventario: tras ~5 s reproduce `TAME_FAIL` y huye.
7. Al completar la fase de tierra: auto-monta, despega y corcovea 15 s.
8. Dejar expirar los 15 s: lanza al jinete, recibe daño de caída, el Pégaso vuelve a
   `WILD` en la zona y puede re-domarse desde cero.
9. Repetir y usar las Bridas durante el corcoveo: doma al 100% a la primera.
10. Equipar silla: se puede montar y galopar; sin bridas no despega.
11. Con silla y bridas: despegue, vuelo 3D, dash con cooldown de 5 s, aterrizaje.
12. Las 6 capas visuales (bridas, silla, 4 armaduras) se pintan correctamente.
13. Guardar y recargar la partida: estado, dueño, temper y contenido del contenedor se
    conservan.
14. `athena_bridle` aparece en cofres de `athenas_ruins` y de `minotaur_arena`;
    `arpy_feather` dropea del Arpy y aparece en `arpy_nest`.
15. Un segundo jugador no puede montar ni abrir el inventario de un Pégaso ajeno.
16. En dificultad Pacífica ni el Pégaso ni el Búho se eliminan; el Arpy sí.

---

## 11. Fuera de alcance

Se dejan explícitamente para más adelante:

- **Ánforas rompibles con loot** en la guarida del Minotauro. La mecánica no existe hoy y
  es un subsistema aparte; las Bridas se obtienen solo de cofres en esta iteración.
- **Estamina de vuelo.** El límite del vuelo es el cooldown del dash, no un recurso.
- **Cría de Pégasos.**
- **Uso funcional de `arpy_feather`** en recetas: por ahora es solo material de drop.
