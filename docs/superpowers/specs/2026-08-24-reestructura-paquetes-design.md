# Reestructura de paquetes: client / common / debug — Diseño

**Fecha:** 2026-08-24
**Mod:** Mythos & Mortals (`mythosmortals`) — NeoForge 26.1
**Estado:** Aprobado, listo para plan de implementación

---

## 1. Resumen

Los 95 archivos Java del mod viven hoy bajo un paraguas `content/<feature>/` que no
distingue qué corre en cliente y qué en servidor: el renderer del búho, sus packets de
red y su entidad comparten carpeta. Esta reestructura hace dos cosas:

1. Sustituye `content/` por una raíz **por tipo de contenido** (`entity/`, `block/`,
   `item/`, `effect/`, `worldgen/`), manteniendo cada feature como carpeta
   autocontenida dentro.
2. Introduce una frontera **client / common / debug** explícita dentro de cada feature.

Y de paso parte el registry central de 766 líneas en clases por tipo de registro.

**No cambia ni una línea de comportamiento.** Es movimiento de archivos, `package`,
`import` y el corte del registry. Los recursos (`assets/`, `data/`) no se tocan: se
resuelven por modid, no por paquete Java.

### Por qué ahora

`OwlAim` importa `LocalPlayer` y vive en la raíz de la feature del búho, indistinguible
de `OwlEntity`. Con 37 clases de cliente repartidas entre las comunes, no hay forma de
saber de un vistazo qué puede romper un servidor dedicado.

---

## 2. Clasificación de partida

Hecha por **imports reales**, no por el nombre del archivo:

| Categoría | Nº | Destino |
|---|---|---|
| **Cliente** — importa `net.minecraft.client.*` o `com.mojang.blaze3d.*` | 39 | `client/**` |
| **Server-only** — usa `ServerPlayer` / `Commands` y nada de cliente | 3 | `debug/` |
| **Common** — el resto | 53 | raíz de su feature, `core/`, `registry/` |

Suman los 95 archivos. El grep de imports de cliente da 39 coincidencias que **no** son
exactamente esas 39, por dos matices que se cancelan en el total:

- `MythosMortalsRegistry` aparece en el grep solo por el `ClientModEvents` que tiene
  anidado. La clase se queda en `registry/`; ese bloque sale en la fase 4.
- `PegasusRenderState` no aparece —no importa nada de Minecraft— pero es de cliente
  (ver abajo).

De los 39 destinos de cliente, `MythosMortalsClient` va a la raíz `client/` y los otros
38 a un `client/**` dentro de su feature.

Las tres server-only son herramientas de diagnóstico: `MinotaurAnimDebug`,
`PegasusFlightDebug`, `PegasusCommands`.

Dos casos que el nombre engaña:

- **`OwlPerchPlacement`** parece de render pero no importa nada de Minecraft: la
  consumen la entidad (para colocar la hitbox), el renderer y el tuner. Es compartida.
- **`PegasusRenderState`** tampoco importa nada de Minecraft, pero sus únicos
  consumidores son `PegasusModel`, `PegasusRenderer` y `PegasusEquipmentLayer`. Va a
  cliente.

---

## 3. Árbol final

```
net.darkblade.mythosmortals/
  core/
    MythosMortals.java
    Config.java
  client/
    MythosMortalsClient.java
    MythosMortalsClientEvents.java
  registry/
    (sección 5)
  entity/
    arpy/
      ArpyEntity.java
      client/render/   ArpyAnimation · ArpyModel · ArpyRenderer
    athenian/
      AthenianEntity.java
      client/render/   AthenianAnimation · AthenianModel ·
                       AthenianHelmetInteriorModel · AthenianRenderer
    spartan/
      SpartanEntity.java
      client/render/   SpartanAnimation · SpartanModel ·
                       SpartanHelmetInteriorModel · SpartanRenderer
    minotaur/
      MinotaurEntity.java · MinotaurCtx.java · MinotaurState.java
      behavior/        ChargeHit · ChargeRun · ChargeStun · ChargeWindup ·
                       Push · SpottedRoar
      debug/           MinotaurAnimDebug
      client/render/   MinotaurAnimation · MinotaurModel · MinotaurRenderer ·
                       MinotaurRiderPose
    owl/
      OwlEntity.java · OwlPerchPlacement.java
      network/         OwlAttackServerPacket · OwlSonicAttackServerPacket ·
                       OwlMarkServerPacket · OwlOrderAttackServerPacket
      statue/          OwlStatueBlock
      client/          OwlAim · OwlPerchHand · OwlStatueClient
      client/render/   OwlRenderer · CopperOwlModel · OwlAnimation ·
                       OwlEyeGlowLayer · OwlEyeGlowRenderType
      client/input/    OwlOrderInput · OwlPerchInput · OwlPerchTuner ·
                       OwlPossessionInput
    pegasus/
      PegasusEntity.java · PegasusTaming.java · PegasusTameState.java
      PegasusEquipment.java · PegasusFlightController.java · PegasusRiderEvents.java
      menu/            PegasusInventoryMenu
      network/         PegasusDashServerPacket
      debug/           PegasusCommands · PegasusFlightDebug
      client/          PegasusInventoryScreen
      client/render/   PegasusAnimation · PegasusModel · PegasusRenderer ·
                       PegasusRenderState · PegasusRiderPose · PegasusEquipmentLayer
      client/input/    PegasusClientInput · PegasusDashInput · PegasusCameraEffects
  block/
    amphora/    AmphoraServings · FilledAmphoraBlock · GreekAmphoraBlock ·
                MarinatedFoodEvents · MarinatingRecipe · WineEvents
    olive/      OliveLeavesBlock · OliveTree
    vineyard/   GrapeStakeBlock · GrapeVineBlock · VineyardInteractions
  item/
    spear/
      DoriSpearItem.java · ThrownDoriSpear.java
      client/render/   DoriSpearProjectileModel
  effect/
    BorealCourageEffect.java · BorealCourageEvents.java
  worldgen/
    structure/  MarkedStructurePiece · MarkedTemplateStructure ·
                PeakSiting · StructureMarkers
```

---

## 4. Reglas de reparto

Reglas, no decisiones caso por caso, para que un archivo nuevo tenga sitio obvio:

1. **`client/`** = importa `net.minecraft.client` o `blaze3d`. Se aplica por imports,
   no por el nombre.
2. **`client/render/`** siempre para renderers, modelos, `AnimationDefinition`, layers y
   render types — incluso cuando es un único archivo (la lanza), para que "los renderers
   están en `client/render`" no tenga excepciones que recordar.
3. **`client/input/`** para lo que engancha teclado, rueda o cámara.
4. **`client/`** a secas para lo de cliente que no es ni render ni input (`OwlAim`,
   `PegasusInventoryScreen`).
5. **`debug/`** para lo server-only de diagnóstico. Es el `server/` que motivó esta
   reestructura, con el nombre que describe lo que realmente contiene.
6. Todo lo demás es **common** y vive en la raíz de su feature.

### Decisiones puntuales

- **`OwlStatueBlock` se queda en `entity/owl/statue/`**, no en `block/`: existe para
  invocar al búho y comparte estado con él. Agruparlo con las ánforas por ser "un
  bloque" separaría dos mitades de la misma mecánica.
- **`perch/` desaparece**: sus dos archivos se separan por lado
  (`OwlPerchPlacement` a la raíz de la feature, `OwlPerchHand` a `client/`), y ninguno
  justifica una carpeta propia.
- **`menu/` se queda con un solo archivo** (`PegasusInventoryMenu`): el `MenuType` es
  común y el `Screen` es de cliente. La carpeta sobrevive porque el menú puede crecer
  (slots, contenedores) y el screen tiene su sitio natural en `client/`.
- **`OliveTree` va a `block/olive/`, no a `worldgen/`**: es el `TreeGrower` del sapling,
  acoplado al bloque, no una feature de generación de mundo.

---

## 5. Corte del registry

`MythosMortalsRegistry` sobrevive como **fachada delgada**: solo `register(IEventBus)`,
`registerGameplay()` y `registerPackets()`. Los `DeferredRegister` y sus holders salen a
clases por tipo, con el prefijo del mod ya establecido en el proyecto:

| Clase | Contenido |
|---|---|
| `MythosMortalsRegistry` | fachada: los tres métodos de arranque |
| `MythosMortalsEntities` | `ENTITY_TYPES` + 6 mobs, `THROWN_DORI_SPEAR`, `OLIVE_BOAT`, `OLIVE_CHEST_BOAT` |
| `MythosMortalsBlocks` | `BLOCKS` + 34 bloques + sus `BlockItem` |
| `MythosMortalsItems` | ya existe: `ITEMS` + 23 ítems |
| `MythosMortalsBlockEntities` | `BLOCK_ENTITY_TYPES` + `OWL_STATUE_BLOCK_ENTITY` |
| `MythosMortalsMenus` | `MENU_TYPES` + `PEGASUS_MENU` |
| `MythosMortalsParticles` | `PARTICLE_TYPES` + `OWL_BOOM` |
| `MythosMortalsEffects` | `MOB_EFFECTS` + `BOREAL_COURAGE` |
| `MythosMortalsRecipes` | `RECIPE_SERIALIZERS` + `MARINATING` |
| `MythosMortalsDataComponents` | `DATA_COMPONENTS` + `MARINATED` |
| `MythosMortalsStructures` | `STRUCTURE_TYPES` + `STRUCTURE_PIECES` y sus dos holders |
| `MythosMortalsCommonEvents` | el `CommonModEvents` anidado (atributos, spawn placements, creative tabs) |
| `MythosMortalsBannerPatterns`<br>`MythosMortalsGlintStyles`<br>`MythosMortalsDatagen` | sin cambios |

Y en `client/`:

| Clase | Contenido |
|---|---|
| `MythosMortalsClientEvents` | el `ClientModEvents` anidado: layer definitions, renderers, particle providers, menu screens |

### Detalles del corte

- **Los `BlockItem` se quedan junto a su bloque** en `MythosMortalsBlocks`. Hoy
  `OWL_STATUE` y `OWL_STATUE_ITEM` son líneas contiguas; separarlos por tipo rompería esa
  adyacencia sin ganar nada. Siguen registrándose contra `MythosMortalsItems.ITEMS`.
- **Tres constantes privadas viajan con `ClientModEvents`**, porque solo se usan ahí:
  `DORI_SPEAR_TEXTURE`, `OLIVE_BOAT_LAYER` y `OLIVE_CHEST_BOAT_LAYER`. Las dos últimas
  son `public` hoy sin que nadie externo las lea; pasan a privadas.
- El volumen de renombrado es acotado: **46 referencias a `MythosMortalsRegistry.X` en
  20 archivos**.

---

## 6. Implementación

Cuatro fases, cada una compilando antes de pasar a la siguiente. Los movimientos van con
`git mv` para conservar el historial de archivos que llevan meses de iteración
(`OwlEntity` y las clases de animación son de las más tocadas del repo).

**Fase 1 — Raíz.** `core/` y `client/`: mover `MythosMortals`, `Config` y
`MythosMortalsClient`. Es el cambio con más alcance de imports (todo el mod importa
`MythosMortals.MODID`) y conviene absorberlo primero, con el árbol viejo aún intacto.

**Fase 2 — Features.** `content/` → `entity/`, `block/`, `item/`, `effect/`,
`worldgen/`, con los subpaquetes `client/`, `client/render/`, `client/input/` y
`debug/`. Feature por feature, empezando por las pequeñas (spear, effect, olive) para
validar el script de reescritura de `package`/`import` antes de tocar búho y pegaso.

**Fase 3 — Registry.** Extraer las clases por tipo, dejando la fachada. Al final,
`MythosMortalsRegistry.register(bus)` debe seguir invocando los diez
`DeferredRegister.register(bus)`.

**Fase 4 — Eventos.** Sacar `CommonModEvents` y `ClientModEvents` a sus clases propias,
con las tres constantes privadas.

### Herramienta

Un script que, dado un mapa `ruta vieja → ruta nueva`, hace el `git mv`, reescribe la
línea `package` del archivo movido y actualiza los `import` en todo `src/`. Los imports
del mod son todos explícitos y cualificados (no hay `import ...*`), así que la
reescritura es una sustitución textual exacta, no heurística.

---

## 7. Verificación

| Cuándo | Comprobación |
|---|---|
| Tras **cada fase** | `./gradlew compileJava --offline` sin errores |
| Tras la fase 2 | ningún archivo bajo `src/` menciona ya `mythosmortals.content` |
| Tras la fase 3 | los diez `DeferredRegister` siguen apareciendo en `register(bus)` |
| Al final | `./gradlew build` |
| Al final | `./gradlew runData` — el datagen ejercita el registro completo |
| Al final | `find src/main/java -name '*.java' \| wc -l` da **106**: los 95 originales más las 11 clases extraídas del registry |
| Al final | `git status` muestra los 95 movimientos como renombrados (`R`), no como pares borrado/añadido |

`runData` es la comprobación que importa: un `DeferredRegister` que se quedara fuera de
`register(bus)` compila perfectamente y solo se cae al arrancar.

---

## 8. Riesgos

**Orden de carga estática.** Partir el registry cambia cuándo se inicializa cada clase.
Los holders son perezosos (`::get`) y `OLIVE_BOAT` ya referencia
`OLIVE_BOAT_ITEM::get` como supplier, así que no debería haber ciclo de inicialización;
pero es exactamente el fallo que `runData` destaparía y por eso está en la lista.

**Registros huérfanos.** Al repartir diez `DeferredRegister` entre nueve clases es fácil
olvidar uno en `register(bus)`. Mitigado por la comprobación explícita de la fase 3.

**Colisión con trabajo en curso.** La reestructura toca los 95 archivos; cualquier rama
paralela va a conflictuar en masa. Debe hacerse de una sentada y fusionarse pronto.

---

## 9. Fuera de alcance

- Renombrar clases más allá de las nuevas del registry. `MythosMortalsRegistry` sigue
  llamándose así, y `CopperOwlModel` no pasa a `OwlModel`.
- Partir archivos grandes. `ArpyAnimation` (2396 líneas) y `OwlEntity` (1549) se mueven
  enteros; son problemas reales pero de otro trabajo.
- Tocar `assets/` o `data/`. No dependen del paquete Java.
- Cualquier cambio de comportamiento. Si el mod se comporta distinto tras esto, es un
  bug de la reestructura.

---

## 10. Alternativas descartadas

**Separación `client/` y `common/` de primer nivel** (`client/owl/OwlRenderer` +
`common/owl/OwlEntity`). Hace trivial auditar qué carga en cliente, pero parte cada mob
en dos árboles lejanos: tocar el pegaso obligaría a saltar entre dos carpetas
constantemente.

**Features al ras de la raíz**, sin paraguas (`mythosmortals.owl`,
`mythosmortals.amphora`). Imports más cortos y cero indirección, pero deja doce paquetes
al mismo nivel que `registry/` y `core/` sin distinguir un mob de un bloque.

**Un paquete `server/`.** No hay contenido server-only más allá de tres herramientas de
diagnóstico. En NeoForge lo que no es cliente es *common* — corre en ambos lados — así
que un `server/` sería una etiqueta falsa; `debug/` dice la verdad.

**Dejar el registry entero y solo sacar `ClientModEvents`.** Menos riesgo, pero deja un
archivo de 700 líneas que ya cuesta navegar y que va a crecer con cada mob nuevo.

### Compromiso asumido

Seis de las clases nuevas del registry (`Particles`, `Effects`, `Recipes`,
`DataComponents`, `BlockEntities`, `Menus`) nacen con **un solo holder** cada una: son
archivos de diez líneas. Es fragmentación deliberada — cada una es el sitio evidente
donde va la siguiente partícula o el siguiente menú, y la alternativa (un cajón
`MythosMortalsMisc`) reproduce a pequeña escala el problema que esto viene a resolver.
