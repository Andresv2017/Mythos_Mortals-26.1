# Reestructura de paquetes — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Sustituir el paraguas `content/` por una raíz por tipo de contenido con una frontera client/common/debug explícita, y partir el registry central de 766 líneas en clases por tipo de registro.

**Architecture:** Todo el movimiento de archivos lo hace una herramienta (`tools/repackage.py`) que hace `git mv` y luego **deriva del disco** el `package` y los `import` correctos de todo el árbol. Como el arreglo se deriva del estado real y no de la fase, la herramienta es idempotente: `check` sobre un árbol sano no cambia nada, y esa propiedad es la comprobación que cierra cada tarea. El corte del registry va después, a mano, ya sobre el árbol nuevo.

**Tech Stack:** Java 25 · NeoForge 26.1 · Gradle (wrapper) · Python 3.10 para la herramienta de migración.

## Global Constraints

- **Ninguna tarea cambia comportamiento.** La única excepción autorizada es la ampliación de visibilidad de la Tarea 7, documentada allí.
- **No se tocan `src/main/resources` ni `src/generated`.** Los recursos se resuelven por modid, no por paquete Java. Verificado: ningún recurso ni el template `neoforge.mods.toml` menciona `darkblade`.
- **Los movimientos van con `git mv`**, nunca copiar-y-borrar: `git status` debe mostrarlos como renombrados (`R`).
- **Gate de toda tarea que toque `.java`:** `./gradlew compileJava --offline` termina en éxito. No hay suite de tests en el proyecto (`src/` solo tiene `main` y `generated`), así que el compilador y `runData` son la red de seguridad.
- El proyecto compila hoy con dos avisos de API deprecada. Esos dos avisos son la línea base: no son un fallo, pero no deben aparecer avisos nuevos.
- Nombres de clases nuevas del registry con el prefijo ya establecido en el proyecto: `MythosMortalsEntities`, no `ModEntities`.

**Spec:** `docs/superpowers/specs/2026-08-24-reestructura-paquetes-design.md`

---

## Estructura de archivos

**Se crea:**

- `tools/repackage.py` — herramienta de migración. Temporal: la Tarea 11 la retira.
- `registry/MythosMortalsEntities.java` — `ENTITY_TYPES` y sus 9 holders.
- `registry/MythosMortalsBlocks.java` — `BLOCKS`, 34 bloques y sus `BlockItem`.
- `registry/MythosMortalsBlockEntities.java`, `MythosMortalsMenus.java`, `MythosMortalsParticles.java`, `MythosMortalsEffects.java`, `MythosMortalsRecipes.java`, `MythosMortalsDataComponents.java`, `MythosMortalsStructures.java` — un `DeferredRegister` cada una.
- `registry/MythosMortalsCommonEvents.java` — el `CommonModEvents` anidado.
- `client/MythosMortalsClientEvents.java` — el `ClientModEvents` anidado y sus tres constantes privadas.

**Se modifica:**

- `registry/MythosMortalsRegistry.java` — de 766 líneas a fachada de tres métodos.
- Los 90 archivos que se mueven (solo `package`/`import`, salvo `PegasusEntity`).
- `registry/MythosMortalsItems.java`, `MythosMortalsDatagen.java`, `MythosMortalsBannerPatterns.java`, `MythosMortalsGlintStyles.java` — solo imports.

---

### Task 1: La herramienta de migración

**Files:**
- Create: `tools/repackage.py`

**Interfaces:**
- Produces: la CLI que consumen las tareas 2–7 y 8–10.
  - `python tools/repackage.py move <fase>` — `git mv` de la fase y arreglo del árbol entero.
  - `python tools/repackage.py fix` — solo el arreglo, sin mover nada. Lo usan las tareas del registry.
  - `python tools/repackage.py check` — arreglo en seco; imprime `archivos que cambiarian: N` y sale con 1 si N > 0.
  - Fases válidas: `core`, `small`, `mobs`, `minotaur`, `owl`, `pegasus`.

**Cómo funciona.** `scan_classes()` recorre el disco y construye `nombre simple de clase -> paquete actual`. Para cada `.java`: reescribe la línea `package` según su ruta, reescribe los `import` del mod que apunten a una clase conocida, **elimina** los que hayan quedado dentro del mismo paquete, y **añade** los que ahora faltan (dos clases que antes compartían paquete y ahora no). La detección de "se usa en este archivo" corre sobre el texto con comentarios y literales sustituidos por espacios: sin eso, un comentario como `(see OwlOrderInput)` provoca un import espurio.

- [ ] **Step 1: Crear el archivo**

Crear `tools/repackage.py` con exactamente este contenido:

```python
#!/usr/bin/env python3
"""Mueve clases del mod entre paquetes y deja package/import consistentes con el disco.

    python tools/repackage.py move <fase>   git mv de la fase + arregla todo el arbol
    python tools/repackage.py fix           solo arregla package/import segun el disco
    python tools/repackage.py check         como fix pero sin escribir; sale 1 si cambiaria algo

El arreglo se deriva SIEMPRE de donde estan los archivos, nunca de la fase, asi que es
idempotente: ejecutar `check` sobre un arbol sano no cambia nada.
"""
import os
import re
import subprocess
import sys

SRC = 'src/main/java'
PREFIX = 'net/darkblade/mythosmortals'
PKG_PREFIX = 'net.darkblade.mythosmortals'

# (ruta vieja, ruta nueva) relativas a SRC/PREFIX
MAPPING = [
    # --- fase core ---
    ('MythosMortals.java', 'core/MythosMortals.java'),
    ('Config.java', 'core/Config.java'),
    ('MythosMortalsClient.java', 'client/MythosMortalsClient.java'),
    # --- fase small ---
    ('content/effect/BorealCourageEffect.java', 'effect/BorealCourageEffect.java'),
    ('content/effect/BorealCourageEvents.java', 'effect/BorealCourageEvents.java'),
    ('content/structure/MarkedStructurePiece.java', 'worldgen/structure/MarkedStructurePiece.java'),
    ('content/structure/MarkedTemplateStructure.java', 'worldgen/structure/MarkedTemplateStructure.java'),
    ('content/structure/PeakSiting.java', 'worldgen/structure/PeakSiting.java'),
    ('content/structure/StructureMarkers.java', 'worldgen/structure/StructureMarkers.java'),
    ('content/amphora/AmphoraServings.java', 'block/amphora/AmphoraServings.java'),
    ('content/amphora/FilledAmphoraBlock.java', 'block/amphora/FilledAmphoraBlock.java'),
    ('content/amphora/GreekAmphoraBlock.java', 'block/amphora/GreekAmphoraBlock.java'),
    ('content/amphora/MarinatedFoodEvents.java', 'block/amphora/MarinatedFoodEvents.java'),
    ('content/amphora/MarinatingRecipe.java', 'block/amphora/MarinatingRecipe.java'),
    ('content/amphora/WineEvents.java', 'block/amphora/WineEvents.java'),
    ('content/olive/OliveLeavesBlock.java', 'block/olive/OliveLeavesBlock.java'),
    ('content/olive/OliveTree.java', 'block/olive/OliveTree.java'),
    ('content/vineyard/GrapeStakeBlock.java', 'block/vineyard/GrapeStakeBlock.java'),
    ('content/vineyard/GrapeVineBlock.java', 'block/vineyard/GrapeVineBlock.java'),
    ('content/vineyard/VineyardInteractions.java', 'block/vineyard/VineyardInteractions.java'),
    ('content/spear/DoriSpearItem.java', 'item/spear/DoriSpearItem.java'),
    ('content/spear/ThrownDoriSpear.java', 'item/spear/ThrownDoriSpear.java'),
    ('content/spear/DoriSpearProjectileModel.java', 'item/spear/client/render/DoriSpearProjectileModel.java'),
    # --- fase mobs ---
    ('content/arpy/ArpyEntity.java', 'entity/arpy/ArpyEntity.java'),
    ('content/arpy/ArpyAnimation.java', 'entity/arpy/client/render/ArpyAnimation.java'),
    ('content/arpy/ArpyModel.java', 'entity/arpy/client/render/ArpyModel.java'),
    ('content/arpy/ArpyRenderer.java', 'entity/arpy/client/render/ArpyRenderer.java'),
    ('content/athenian/AthenianEntity.java', 'entity/athenian/AthenianEntity.java'),
    ('content/athenian/AthenianAnimation.java', 'entity/athenian/client/render/AthenianAnimation.java'),
    ('content/athenian/AthenianHelmetInteriorModel.java', 'entity/athenian/client/render/AthenianHelmetInteriorModel.java'),
    ('content/athenian/AthenianModel.java', 'entity/athenian/client/render/AthenianModel.java'),
    ('content/athenian/AthenianRenderer.java', 'entity/athenian/client/render/AthenianRenderer.java'),
    ('content/spartan/SpartanEntity.java', 'entity/spartan/SpartanEntity.java'),
    ('content/spartan/SpartanAnimation.java', 'entity/spartan/client/render/SpartanAnimation.java'),
    ('content/spartan/SpartanHelmetInteriorModel.java', 'entity/spartan/client/render/SpartanHelmetInteriorModel.java'),
    ('content/spartan/SpartanModel.java', 'entity/spartan/client/render/SpartanModel.java'),
    ('content/spartan/SpartanRenderer.java', 'entity/spartan/client/render/SpartanRenderer.java'),
    # --- fase minotaur ---
    ('content/minotaur/MinotaurEntity.java', 'entity/minotaur/MinotaurEntity.java'),
    ('content/minotaur/MinotaurCtx.java', 'entity/minotaur/MinotaurCtx.java'),
    ('content/minotaur/MinotaurState.java', 'entity/minotaur/MinotaurState.java'),
    ('content/minotaur/behavior/ChargeHitBehavior.java', 'entity/minotaur/behavior/ChargeHitBehavior.java'),
    ('content/minotaur/behavior/ChargeRunBehavior.java', 'entity/minotaur/behavior/ChargeRunBehavior.java'),
    ('content/minotaur/behavior/ChargeStunBehavior.java', 'entity/minotaur/behavior/ChargeStunBehavior.java'),
    ('content/minotaur/behavior/ChargeWindupBehavior.java', 'entity/minotaur/behavior/ChargeWindupBehavior.java'),
    ('content/minotaur/behavior/PushBehavior.java', 'entity/minotaur/behavior/PushBehavior.java'),
    ('content/minotaur/behavior/SpottedRoarBehavior.java', 'entity/minotaur/behavior/SpottedRoarBehavior.java'),
    ('content/minotaur/MinotaurAnimDebug.java', 'entity/minotaur/debug/MinotaurAnimDebug.java'),
    ('content/minotaur/MinotaurAnimation.java', 'entity/minotaur/client/render/MinotaurAnimation.java'),
    ('content/minotaur/MinotaurModel.java', 'entity/minotaur/client/render/MinotaurModel.java'),
    ('content/minotaur/MinotaurRenderer.java', 'entity/minotaur/client/render/MinotaurRenderer.java'),
    ('content/minotaur/MinotaurRiderPose.java', 'entity/minotaur/client/render/MinotaurRiderPose.java'),
    # --- fase owl ---
    ('content/owl/OwlEntity.java', 'entity/owl/OwlEntity.java'),
    ('content/owl/perch/OwlPerchPlacement.java', 'entity/owl/OwlPerchPlacement.java'),
    ('content/owl/network/OwlAttackServerPacket.java', 'entity/owl/network/OwlAttackServerPacket.java'),
    ('content/owl/network/OwlMarkServerPacket.java', 'entity/owl/network/OwlMarkServerPacket.java'),
    ('content/owl/network/OwlOrderAttackServerPacket.java', 'entity/owl/network/OwlOrderAttackServerPacket.java'),
    ('content/owl/network/OwlSonicAttackServerPacket.java', 'entity/owl/network/OwlSonicAttackServerPacket.java'),
    ('content/owl/statue/OwlStatueBlock.java', 'entity/owl/statue/OwlStatueBlock.java'),
    ('content/owl/OwlAim.java', 'entity/owl/client/OwlAim.java'),
    ('content/owl/perch/OwlPerchHand.java', 'entity/owl/client/OwlPerchHand.java'),
    ('content/owl/statue/OwlStatueClient.java', 'entity/owl/client/OwlStatueClient.java'),
    ('content/owl/OwlRenderer.java', 'entity/owl/client/render/OwlRenderer.java'),
    ('content/owl/CopperOwlModel.java', 'entity/owl/client/render/CopperOwlModel.java'),
    ('content/owl/OwlAnimation.java', 'entity/owl/client/render/OwlAnimation.java'),
    ('content/owl/OwlEyeGlowLayer.java', 'entity/owl/client/render/OwlEyeGlowLayer.java'),
    ('content/owl/OwlEyeGlowRenderType.java', 'entity/owl/client/render/OwlEyeGlowRenderType.java'),
    ('content/owl/input/OwlOrderInput.java', 'entity/owl/client/input/OwlOrderInput.java'),
    ('content/owl/input/OwlPerchInput.java', 'entity/owl/client/input/OwlPerchInput.java'),
    ('content/owl/input/OwlPerchTuner.java', 'entity/owl/client/input/OwlPerchTuner.java'),
    ('content/owl/input/OwlPossessionInput.java', 'entity/owl/client/input/OwlPossessionInput.java'),
    # --- fase pegasus ---
    ('content/pegasus/PegasusEntity.java', 'entity/pegasus/PegasusEntity.java'),
    ('content/pegasus/PegasusEquipment.java', 'entity/pegasus/PegasusEquipment.java'),
    ('content/pegasus/PegasusFlightController.java', 'entity/pegasus/PegasusFlightController.java'),
    ('content/pegasus/PegasusRiderEvents.java', 'entity/pegasus/PegasusRiderEvents.java'),
    ('content/pegasus/PegasusTameState.java', 'entity/pegasus/PegasusTameState.java'),
    ('content/pegasus/PegasusTaming.java', 'entity/pegasus/PegasusTaming.java'),
    ('content/pegasus/menu/PegasusInventoryMenu.java', 'entity/pegasus/menu/PegasusInventoryMenu.java'),
    ('content/pegasus/network/PegasusDashServerPacket.java', 'entity/pegasus/network/PegasusDashServerPacket.java'),
    ('content/pegasus/PegasusCommands.java', 'entity/pegasus/debug/PegasusCommands.java'),
    ('content/pegasus/PegasusFlightDebug.java', 'entity/pegasus/debug/PegasusFlightDebug.java'),
    ('content/pegasus/menu/PegasusInventoryScreen.java', 'entity/pegasus/client/PegasusInventoryScreen.java'),
    ('content/pegasus/PegasusAnimation.java', 'entity/pegasus/client/render/PegasusAnimation.java'),
    ('content/pegasus/PegasusEquipmentLayer.java', 'entity/pegasus/client/render/PegasusEquipmentLayer.java'),
    ('content/pegasus/PegasusModel.java', 'entity/pegasus/client/render/PegasusModel.java'),
    ('content/pegasus/PegasusRenderState.java', 'entity/pegasus/client/render/PegasusRenderState.java'),
    ('content/pegasus/PegasusRenderer.java', 'entity/pegasus/client/render/PegasusRenderer.java'),
    ('content/pegasus/PegasusRiderPose.java', 'entity/pegasus/client/render/PegasusRiderPose.java'),
    ('content/pegasus/input/PegasusCameraEffects.java', 'entity/pegasus/client/input/PegasusCameraEffects.java'),
    ('content/pegasus/input/PegasusClientInput.java', 'entity/pegasus/client/input/PegasusClientInput.java'),
    ('content/pegasus/input/PegasusDashInput.java', 'entity/pegasus/client/input/PegasusDashInput.java'),
]

PHASES = {
    'core': lambda old: '/' not in old,
    'small': lambda old: old.split('/')[1] in
        ('effect', 'structure', 'amphora', 'olive', 'vineyard', 'spear') if old.startswith('content/') else False,
    'mobs': lambda old: old.startswith(('content/arpy/', 'content/athenian/', 'content/spartan/')),
    'minotaur': lambda old: old.startswith('content/minotaur/'),
    'owl': lambda old: old.startswith('content/owl/'),
    'pegasus': lambda old: old.startswith('content/pegasus/'),
}


def scan_classes():
    """Nombre simple de clase -> paquete actual, leido del disco."""
    found = {}
    for dirpath, _dirs, files in os.walk(os.path.join(SRC, PREFIX)):
        for name in files:
            if not name.endswith('.java'):
                continue
            rel = os.path.relpath(os.path.join(dirpath, name), SRC).replace(os.sep, '/')
            pkg = rel[:-(len(name) + 1)].replace('/', '.')
            cls = name[:-len('.java')]
            if cls in found:
                sys.exit('ERROR: clase duplicada ' + cls + ' en ' + pkg + ' y ' + found[cls])
            found[cls] = pkg
    return found


def strip_comments(text):
    """Sustituye comentarios y literales por espacios, para no confundir una mencion
    en un comentario ('see OwlOrderInput') con un uso real que necesite import."""
    out, i, n = [], 0, len(text)
    bs = chr(92)
    while i < n:
        c = text[i]
        if c in '"\'':
            j = i + 1
            while j < n and text[j] != '\n':
                if text[j] == bs:
                    j += 2
                elif text[j] == c:
                    j += 1
                    break
                else:
                    j += 1
            out.append(' ' * (min(j, n) - i))
            i = j
        elif text.startswith('//', i):
            j = text.find('\n', i)
            j = n if j == -1 else j
            out.append(' ' * (j - i))
            i = j
        elif text.startswith('/*', i):
            j = text.find('*/', i + 2)
            j = n if j == -1 else j + 2
            out.append(re.sub(r'[^\n]', ' ', text[i:j]))
            i = j
        else:
            out.append(c)
            i += 1
    return ''.join(out)


def fix_text(raw, pkg, classes):
    newline = '\r\n' if '\r\n' in raw else '\n'
    lines = raw.replace('\r\n', '\n').split('\n')

    pkg_at = next(i for i, l in enumerate(lines) if l.startswith('package '))
    lines[pkg_at] = 'package ' + pkg + ';'

    kept, imported, last_import, last_mod_import = [], set(), pkg_at, None
    for i, line in enumerate(lines):
        m = re.match(r'import\s+(?:static\s+)?([\w.]+)\.(\w+);', line)
        if not m:
            kept.append(line)
            continue
        owner, cls = m.group(1), m.group(2)
        if owner.startswith(PKG_PREFIX) and cls in classes:
            if classes[cls] == pkg:
                continue                      # mismo paquete: el import sobra
            line = 'import ' + classes[cls] + '.' + cls + ';'
            last_mod_import = len(kept)
        imported.add(cls)
        last_import = len(kept)
        kept.append(line)
    lines = kept

    body = strip_comments('\n'.join(
        l for l in lines if not l.startswith(('import ', 'package '))))
    missing = []
    for cls, owner in classes.items():
        if owner == pkg or cls in imported:
            continue
        if re.search(r'(?<![\w.$])' + re.escape(cls) + r'(?![\w$])', body):
            missing.append('import ' + owner + '.' + cls + ';')

    if missing:
        at = (last_mod_import if last_mod_import is not None else last_import) + 1
        lines[at:at] = sorted(missing)

    return newline.join(lines)


def fix_tree(write):
    classes = scan_classes()
    changed = []
    for dirpath, _dirs, files in os.walk(os.path.join(SRC, PREFIX)):
        for name in files:
            if not name.endswith('.java'):
                continue
            path = os.path.join(dirpath, name)
            rel = os.path.relpath(path, SRC).replace(os.sep, '/')
            pkg = rel[:-(len(name) + 1)].replace('/', '.')
            with open(path, encoding='utf-8', newline='') as fh:
                raw = fh.read()
            out = fix_text(raw, pkg, classes)
            if out != raw:
                changed.append(rel)
                if write:
                    with open(path, 'w', encoding='utf-8', newline='') as fh:
                        fh.write(out)
    return changed


def move(phase):
    if phase not in PHASES:
        sys.exit('fases: ' + ', '.join(PHASES))
    picked = [(o, n) for o, n in MAPPING if PHASES[phase](o)]
    if not picked:
        sys.exit('ERROR: la fase ' + phase + ' no selecciona ningun archivo')
    for old, new in picked:
        src = os.path.join(SRC, PREFIX, old)
        dst = os.path.join(SRC, PREFIX, new)
        if not os.path.exists(src):
            sys.exit('ERROR: no existe ' + src)
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        subprocess.check_call(['git', 'mv', src, dst])
    print('movidos ' + str(len(picked)) + ' archivos en la fase ' + phase)


def main():
    cmd = sys.argv[1] if len(sys.argv) > 1 else ''
    if cmd == 'move':
        move(sys.argv[2] if len(sys.argv) > 2 else '')
        for rel in fix_tree(write=True):
            print('  fix ' + rel)
    elif cmd == 'fix':
        for rel in fix_tree(write=True):
            print('  fix ' + rel)
    elif cmd == 'check':
        pending = fix_tree(write=False)
        for rel in pending:
            print('  CAMBIARIA ' + rel)
        print('archivos que cambiarian: ' + str(len(pending)))
        sys.exit(1 if pending else 0)
    else:
        sys.exit(__doc__)


if __name__ == '__main__':
    main()
```

- [ ] **Step 2: Comprobar que el mapa está completo y sin colisiones**

```bash
python -c "import sys; sys.path.insert(0,'tools'); import repackage as r; d=[n for _,n in r.MAPPING]; assert len(d)==len(set(d)), 'destino duplicado'; print('movimientos:', len(r.MAPPING)); print('fases cubren:', sum(any(f(o) for f in r.PHASES.values()) for o,_ in r.MAPPING))"
```

Esperado: `movimientos: 90` y `fases cubren: 90`. Si el segundo número es menor, hay entradas del mapa que ninguna fase selecciona y se quedarían sin mover.

- [ ] **Step 3: Verificar que la herramienta es un no-op sobre el árbol intacto**

Esta es la prueba de que el arreglo no inventa cambios. Sobre el repo sin mover nada:

```bash
python tools/repackage.py check
```

Esperado: `archivos que cambiarian: 0` y código de salida 0. **Si sale distinto de 0, la herramienta tiene un bug y no se puede seguir** — revisar `strip_comments` antes que nada, porque el fallo típico es un import espurio por una mención en un comentario.

- [ ] **Step 4: Commit**

```bash
git add tools/repackage.py && git commit -m "Add package migration tool"
```

---

### Task 2: Fase core — `core/` y `client/`

**Files:**
- Move: `MythosMortals.java`, `Config.java` → `core/`
- Move: `MythosMortalsClient.java` → `client/`

**Interfaces:**
- Consumes: `tools/repackage.py` de la Tarea 1.
- Produces: `net.darkblade.mythosmortals.core.MythosMortals` — la constante `MODID` y el logger `LOGGER` que importa prácticamente todo el mod. Las tareas 3–10 dependen de este paquete.

Va primera a propósito: es el cambio con más alcance de imports, y conviene absorberlo con el árbol viejo todavía intacto.

- [ ] **Step 1: Mover**

```bash
python tools/repackage.py move core
```

Esperado: `movidos 3 archivos en la fase core`, seguido de una lista larga de líneas `fix ...` — casi todos los archivos del mod importan `MythosMortals`.

- [ ] **Step 2: Comprobar convergencia**

```bash
python tools/repackage.py check
```

Esperado: `archivos que cambiarian: 0`. Confirma que el arreglo llegó a un punto fijo.

- [ ] **Step 3: Compilar**

```bash
./gradlew compileJava --offline
```

Esperado: `BUILD SUCCESSFUL`, con los dos avisos de API deprecada de la línea base y ninguno más.

- [ ] **Step 4: Comprobar que git ve renombrados y no borrados**

```bash
git status --short | cut -c1-3 | sort | uniq -c
```

Esperado: 3 entradas con `R` (renombrado). Si aparecen pares `D`/`??` es que algo copió en vez de mover.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "Move mod entrypoint and config into core/ and client/"
```

---

### Task 3: Fase small — bloques, item, efecto y worldgen

**Files:**
- Move: `content/amphora/` (6) → `block/amphora/`
- Move: `content/olive/` (2) → `block/olive/`
- Move: `content/vineyard/` (3) → `block/vineyard/`
- Move: `content/spear/` (3) → `item/spear/`, con `DoriSpearProjectileModel` a `item/spear/client/render/`
- Move: `content/effect/` (2) → `effect/`
- Move: `content/structure/` (4) → `worldgen/structure/`

**Interfaces:**
- Consumes: `core.MythosMortals` de la Tarea 2.
- Produces: `net.darkblade.mythosmortals.block.amphora.MarinatingRecipe` y `net.darkblade.mythosmortals.worldgen.structure.MarkedTemplateStructure` / `MarkedStructurePiece`, que la Tarea 9 necesita para `MythosMortalsRecipes` y `MythosMortalsStructures`.

Van juntas porque son features sin código de cliente entrelazado (salvo el único modelo de la lanza), así que validan el script antes de tocar búho y pegaso.

- [ ] **Step 1: Mover**

```bash
python tools/repackage.py move small
```

Esperado: `movidos 20 archivos en la fase small`.

- [ ] **Step 2: Comprobar convergencia**

```bash
python tools/repackage.py check
```

Esperado: `archivos que cambiarian: 0`.

- [ ] **Step 3: Compilar**

```bash
./gradlew compileJava --offline
```

Esperado: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "Move blocks, spear, effect and structures out of content/"
```

---

### Task 4: Fase mobs — arpy, ateniense y espartano

**Files:**
- Move: `content/arpy/` (4) → `entity/arpy/`, con `ArpyAnimation`, `ArpyModel`, `ArpyRenderer` a `entity/arpy/client/render/`
- Move: `content/athenian/` (5) → `entity/athenian/`, con las 4 clases de render a `entity/athenian/client/render/`
- Move: `content/spartan/` (5) → `entity/spartan/`, con las 4 clases de render a `entity/spartan/client/render/`

**Interfaces:**
- Consumes: `core.MythosMortals` de la Tarea 2.
- Produces: `entity.arpy.ArpyEntity`, `entity.athenian.AthenianEntity`, `entity.spartan.SpartanEntity` (los usa `MythosMortalsEntities` en la Tarea 8) y `entity.<mob>.client.render.<Mob>Model` con su `LAYER_LOCATION` (los usa `MythosMortalsClientEvents` en la Tarea 10).

Aquí es donde el arreglo de imports empieza a ganarse el sueldo: `ArpyEntity` y `ArpyAnimation` compartían paquete y no se importaban entre sí; al separarlos, el script tiene que **añadir** imports que antes no existían.

- [ ] **Step 1: Mover**

```bash
python tools/repackage.py move mobs
```

Esperado: `movidos 14 archivos en la fase mobs`.

- [ ] **Step 2: Comprobar que se añadieron imports nuevos, no solo reescritos**

```bash
git diff --cached -U0 -- "*Entity.java" | grep "^+import net.darkblade.mythosmortals.entity"
```

Esperado: al menos una línea por mob, del tipo `+import net.darkblade.mythosmortals.entity.arpy.client.render.ArpyAnimation;`. Si no aparece ninguna, el detector de uso no está funcionando y la compilación del paso 4 lo va a confirmar.

- [ ] **Step 3: Comprobar convergencia**

```bash
python tools/repackage.py check
```

Esperado: `archivos que cambiarian: 0`.

- [ ] **Step 4: Compilar**

```bash
./gradlew compileJava --offline
```

Esperado: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "Move arpy, athenian and spartan into entity/ with client/render split"
```

---

### Task 5: Fase minotaur

**Files:**
- Move: `content/minotaur/{MinotaurEntity,MinotaurCtx,MinotaurState}.java` → `entity/minotaur/`
- Move: `content/minotaur/behavior/` (6) → `entity/minotaur/behavior/`
- Move: `content/minotaur/MinotaurAnimDebug.java` → `entity/minotaur/debug/`
- Move: `content/minotaur/{MinotaurAnimation,MinotaurModel,MinotaurRenderer,MinotaurRiderPose}.java` → `entity/minotaur/client/render/`

**Interfaces:**
- Consumes: `core.MythosMortals` de la Tarea 2.
- Produces: `entity.minotaur.MinotaurEntity` (lo usa `MythosMortalsEntities` en la Tarea 8) y `entity.minotaur.client.render.MinotaurModel.LAYER_LOCATION` (Tarea 10).

Primer uso de `debug/`. `MinotaurAnimDebug` solo consume API pública de `MinotaurEntity`, así que separarlo no rompe visibilidad — a diferencia del pegaso en la Tarea 7.

- [ ] **Step 1: Mover**

```bash
python tools/repackage.py move minotaur
```

Esperado: `movidos 14 archivos en la fase minotaur`.

- [ ] **Step 2: Comprobar convergencia**

```bash
python tools/repackage.py check
```

Esperado: `archivos que cambiarian: 0`.

- [ ] **Step 3: Compilar**

```bash
./gradlew compileJava --offline
```

Esperado: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "Move minotaur into entity/ with behavior, debug and client/render split"
```

---

### Task 6: Fase owl

**Files:**
- Move: `content/owl/OwlEntity.java` → `entity/owl/`
- Move: `content/owl/perch/OwlPerchPlacement.java` → `entity/owl/OwlPerchPlacement.java`
- Move: `content/owl/network/` (4) → `entity/owl/network/`
- Move: `content/owl/statue/OwlStatueBlock.java` → `entity/owl/statue/`
- Move: `content/owl/{OwlAim}.java`, `content/owl/perch/OwlPerchHand.java`, `content/owl/statue/OwlStatueClient.java` → `entity/owl/client/`
- Move: `content/owl/{OwlRenderer,CopperOwlModel,OwlAnimation,OwlEyeGlowLayer,OwlEyeGlowRenderType}.java` → `entity/owl/client/render/`
- Move: `content/owl/input/` (4) → `entity/owl/client/input/`

**Interfaces:**
- Consumes: `core.MythosMortals` de la Tarea 2.
- Produces: `entity.owl.OwlEntity`, `entity.owl.statue.OwlStatueBlock` (Tareas 8), los 4 packets de `entity.owl.network` (Tarea 9, `registerPackets()`) y `entity.owl.client.render.CopperOwlModel.LAYER_LOCATION` (Tarea 10).

La feature más enredada: 19 archivos que se reparten entre 5 paquetes. `perch/` desaparece — sus dos archivos se van a lados distintos. `OwlAim` sube a `client/` desde la raíz de la feature, que es la corrección que motivó todo esto.

- [ ] **Step 1: Mover**

```bash
python tools/repackage.py move owl
```

Esperado: `movidos 19 archivos en la fase owl`.

- [ ] **Step 2: Comprobar que `perch/` y `statue/` quedaron como toca**

```bash
ls src/main/java/net/darkblade/mythosmortals/entity/owl src/main/java/net/darkblade/mythosmortals/entity/owl/client src/main/java/net/darkblade/mythosmortals/entity/owl/statue
```

Esperado: `OwlEntity.java` y `OwlPerchPlacement.java` en la raíz; `OwlAim.java`, `OwlPerchHand.java`, `OwlStatueClient.java` en `client/`; solo `OwlStatueBlock.java` en `statue/`. Ningún directorio `perch/`.

- [ ] **Step 3: Comprobar convergencia**

```bash
python tools/repackage.py check
```

Esperado: `archivos que cambiarian: 0`.

- [ ] **Step 4: Compilar**

```bash
./gradlew compileJava --offline
```

Esperado: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "Move owl into entity/ with client, network and statue split"
```

---

### Task 7: Fase pegasus, visibilidad y muerte de `content/`

**Files:**
- Move: `content/pegasus/` (20) → `entity/pegasus/` según el mapa
- Modify: `entity/pegasus/PegasusEntity.java` — 5 miembros pasan de package-private a `public`
- Delete: los directorios vacíos que deja `git mv`

**Interfaces:**
- Consumes: `core.MythosMortals` de la Tarea 2.
- Produces: `entity.pegasus.PegasusEntity` (Tarea 8), `entity.pegasus.menu.PegasusInventoryMenu` (Tarea 9, `MythosMortalsMenus`), `entity.pegasus.network.PegasusDashServerPacket` (Tarea 9, `registerPackets()`), `entity.pegasus.client.PegasusInventoryScreen` y `entity.pegasus.client.render.PegasusModel.LAYER_LOCATION` (Tarea 10).
- Produces: `PegasusEntity.WALK_SPEED`, `PegasusEntity.FLY_MOVE_SPEED` (`public static final float`) y `PegasusEntity.flightDurationForDebug()`, `groundRestForDebug()` (`public int`), `groundHeightForDebug()` (`public double`).

**Esta tarea contiene el único cambio de código real del plan.** `PegasusFlightDebug` accede a cinco miembros package-private de `PegasusEntity`; al mandarlo a `debug/` deja de compartir paquete y el compilador lo rechaza con cinco `is not public in PegasusEntity`. Están declarados así a propósito —el comentario en `PegasusEntity` dice que son "vistas estrechas sobre estado protegido de la base, solo para PegasusFlightDebug"— y `PegasusFlightDebug` es su único consumidor externo, verificado por grep. Ampliarlos a `public` es el precio de separar `debug/`.

- [ ] **Step 1: Mover**

```bash
python tools/repackage.py move pegasus
```

Esperado: `movidos 20 archivos en la fase pegasus`.

- [ ] **Step 2: Compilar y ver fallar exactamente estos cinco errores**

```bash
./gradlew compileJava --offline
```

Esperado: `5 errors`, todos en `entity/pegasus/debug/PegasusFlightDebug.java` (líneas 65, 76, 78, 101, 104), todos de la forma `X is not public in PegasusEntity; cannot be accessed from outside package`. **Si aparecen errores en otros archivos, parar**: significa que hay más acoplamiento package-private del previsto y hay que analizarlo antes de seguir ampliando visibilidad a ciegas.

- [ ] **Step 3: Ampliar la visibilidad de los cinco miembros**

En `src/main/java/net/darkblade/mythosmortals/entity/pegasus/PegasusEntity.java`, añadir `public` a estas cinco declaraciones (están en las líneas 91, 92, 490, 492 y 494):

```java
    public static final float WALK_SPEED = 0.015F;
    public static final float FLY_MOVE_SPEED = 0.08F;
```

```java
    public int flightDurationForDebug() { return this.flightDurationTimer; }

    public int groundRestForDebug() { return this.groundRestTimer; }

    public double groundHeightForDebug() { return this.groundHeight(); }
```

- [ ] **Step 4: Actualizar el comentario que justificaba la visibilidad estrecha**

El comentario sobre esos accesores dice hoy "Narrow views onto protected base state, for PegasusFlightDebug only". Ya no son estrechos. Sustituirlo por:

```java
    // Read-only views onto protected base state, for the debug overlay in debug/PegasusFlightDebug.
    // Public rather than package-private because that class no longer shares this package.
```

- [ ] **Step 5: Compilar**

```bash
./gradlew compileJava --offline
```

Esperado: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Borrar los directorios vacíos que deja `git mv`**

`git mv` no borra el directorio de origen cuando queda vacío, así que `content/` y todos sus subdirectorios siguen existiendo en disco (git los ignora, pero ensucian el árbol y el IDE los muestra).

```bash
find src/main/java -type d -empty -delete
```

- [ ] **Step 7: Comprobar que `content/` ha desaparecido del todo**

```bash
ls src/main/java/net/darkblade/mythosmortals/
grep -rl "mythosmortals.content" src/ ; echo "codigo de salida grep: $?"
```

Esperado: el listado es exactamente `block client core effect entity item registry worldgen` — sin `content`. Y el grep no imprime ninguna ruta (código de salida 1, que aquí significa "ninguna coincidencia" y es el resultado bueno).

- [ ] **Step 8: Comprobar el recuento de archivos**

```bash
find src/main/java -name '*.java' | wc -l
```

Esperado: `95`. Todavía no se ha creado ninguna clase nueva; el reparto del registry llega en las tareas siguientes.

- [ ] **Step 9: Commit**

```bash
git add -A && git commit -m "Move pegasus into entity/ and retire the content/ package"
```

---

### Task 8: Registry — entidades y bloques

**Files:**
- Create: `src/main/java/net/darkblade/mythosmortals/registry/MythosMortalsEntities.java`
- Create: `src/main/java/net/darkblade/mythosmortals/registry/MythosMortalsBlocks.java`
- Modify: `src/main/java/net/darkblade/mythosmortals/registry/MythosMortalsRegistry.java`

**Interfaces:**
- Consumes: las clases de entidad y bloque de las Tareas 3–7.
- Produces:
  - `MythosMortalsEntities.ENTITY_TYPES` (`DeferredRegister<EntityType<?>>`) y los holders `ATHENIAN`, `SPARTAN`, `ARPY`, `OWL`, `MINOTAUR`, `PEGASUS`, `THROWN_DORI_SPEAR`, `OLIVE_BOAT`, `OLIVE_CHEST_BOAT`.
  - `MythosMortalsBlocks.BLOCKS` (`DeferredRegister.Blocks`), sus 34 `DeferredBlock` y los `DeferredItem<BlockItem>` correspondientes, incluidos `OWL_STATUE`, `OWL_STATUE_ITEM`, `TIN_ORE`, `OLIVE_SIGN`, `OLIVE_HANGING_SIGN`, `OLIVE_BOAT_ITEM`, `OLIVE_CHEST_BOAT_ITEM`, `GREEK_AMPHORA_ITEM`.
- La Tarea 9 depende de que `register(bus)` siga registrando `ENTITY_TYPES` y `BLOCKS`.

Las dos clases grandes van juntas y solas porque entre ellas se referencian: `OLIVE_BOAT` (un `EntityType`) usa `OLIVE_BOAT_ITEM::get` (un `DeferredItem`), y ese cruce es lo único delicado del corte.

- [ ] **Step 1: Crear `MythosMortalsEntities`**

Cortar de `MythosMortalsRegistry` el campo `ENTITY_TYPES` y los holders `ATHENIAN`, `SPARTAN`, `ARPY`, `OWL`, `MINOTAUR`, `PEGASUS`, `THROWN_DORI_SPEAR` (declarados juntos al principio de la clase), más `OLIVE_BOAT` y `OLIVE_CHEST_BOAT` (declarados más abajo, entremezclados con los bloques del olivo). Pegarlos **literalmente, sin reescribir ninguna expresión** en:

```java
package net.darkblade.mythosmortals.registry;

public final class MythosMortalsEntities {

    // ... aqui las declaraciones cortadas, en el mismo orden ...

    private MythosMortalsEntities() {
    }
}
```

Dejar los imports en blanco: el paso 4 los rellena.

Dentro de `OLIVE_BOAT` y `OLIVE_CHEST_BOAT` hay dos referencias cualificadas `MythosMortalsRegistry.OLIVE_BOAT_ITEM::get` y `MythosMortalsRegistry.OLIVE_CHEST_BOAT_ITEM::get`. No hace falta tocarlas a mano: el script del paso 3 las reescribe a `MythosMortalsBlocks.` junto con todo lo demás.

- [ ] **Step 2: Crear `MythosMortalsBlocks`**

Cortar de `MythosMortalsRegistry` el campo `BLOCKS` y **todas** las declaraciones `DeferredBlock<...>` y `DeferredItem<...>` que lo acompañan, desde `OWL_STATUE` hasta la última antes de `PARTICLE_TYPES`. Pegarlas literalmente en:

```java
package net.darkblade.mythosmortals.registry;

public final class MythosMortalsBlocks {

    // ... aqui las declaraciones cortadas, en el mismo orden ...

    private MythosMortalsBlocks() {
    }
}
```

Las llamadas a `MythosMortalsItems.ITEMS.registerSimpleBlockItem(...)` y `registerItem(...)` se quedan tal cual: los `BlockItem` siguen registrándose contra el `DeferredRegister` de ítems, solo cambia la clase donde vive la constante.

**No mover** `OLIVE_BOAT` ni `OLIVE_CHEST_BOAT` aquí aunque estén físicamente en medio de esta región: son `EntityType` y van a `MythosMortalsEntities`.

- [ ] **Step 3: Reescribir las referencias en todo el mod**

Los símbolos ya no viven en `MythosMortalsRegistry`. Este script reescribe las referencias cualificadas:

```bash
python - <<'PYEOF'
import io, os, re
ENT = ['ATHENIAN','SPARTAN','ARPY','OWL','MINOTAUR','PEGASUS','THROWN_DORI_SPEAR',
       'OLIVE_BOAT','OLIVE_CHEST_BOAT','ENTITY_TYPES']
for dirpath, _d, files in os.walk('src/main/java'):
    for f in files:
        if not f.endswith('.java'):
            continue
        p = os.path.join(dirpath, f)
        raw = io.open(p, encoding='utf-8', newline='').read()
        def repl(m):
            sym = m.group(1)
            # OLIVE_BOAT_ITEM y OLIVE_CHEST_BOAT_ITEM son bloques, no entidades
            if sym in ENT and not sym.endswith('_ITEM'):
                return 'MythosMortalsEntities.' + sym
            return 'MythosMortalsBlocks.' + sym
        out = re.sub(r'MythosMortalsRegistry\.([A-Z][A-Z0-9_]*)', repl, raw)
        if out != raw:
            io.open(p, 'w', encoding='utf-8', newline='').write(out)
            print('reescrito', p)
PYEOF
```

Cuidado con el orden de la comprobación: `OLIVE_BOAT` está en `ENT` y `OLIVE_BOAT_ITEM` empieza igual, por eso el `endswith('_ITEM')` va explícito. La regex `[A-Z][A-Z0-9_]*` solo toca constantes en mayúsculas, así que no altera llamadas a métodos como `MythosMortalsRegistry.register(bus)`.

- [ ] **Step 4: Arreglar imports**

```bash
python tools/repackage.py fix
```

Añade `import net.darkblade.mythosmortals.registry.MythosMortalsEntities;` y `MythosMortalsBlocks` donde ahora hagan falta, y quita los `MythosMortalsRegistry` que hayan quedado sin uso... salvo que sigan usándose para otros símbolos. Comprobar el resultado con el compilador.

- [ ] **Step 5: Compilar**

```bash
./gradlew compileJava --offline
```

Esperado: `BUILD SUCCESSFUL`. Los fallos típicos aquí son un holder que se quedó en las dos clases (símbolo duplicado) o uno que no se movió a ninguna (símbolo no encontrado).

- [ ] **Step 6: Comprobar que `register(bus)` sigue completo**

```bash
grep -c "\.register(bus);" src/main/java/net/darkblade/mythosmortals/registry/MythosMortalsRegistry.java
```

Esperado: `10`. `ENTITY_TYPES` y `BLOCKS` ahora se referencian cualificados (`MythosMortalsEntities.ENTITY_TYPES.register(bus)`), pero siguen siendo diez llamadas.

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "Split entities and blocks out of the central registry"
```

---

### Task 9: Registry — registros pequeños, estructuras y fachada

**Files:**
- Create: `registry/MythosMortalsBlockEntities.java`, `MythosMortalsMenus.java`, `MythosMortalsParticles.java`, `MythosMortalsEffects.java`, `MythosMortalsRecipes.java`, `MythosMortalsDataComponents.java`, `MythosMortalsStructures.java`
- Modify: `registry/MythosMortalsRegistry.java`

**Interfaces:**
- Consumes: `MythosMortalsEntities` y `MythosMortalsBlocks` de la Tarea 8; `block.amphora.MarinatingRecipe` de la Tarea 3; `worldgen.structure.{MarkedTemplateStructure,MarkedStructurePiece}` de la Tarea 3; `entity.pegasus.menu.PegasusInventoryMenu` de la Tarea 7.
- Produces:
  - `MythosMortalsBlockEntities.BLOCK_ENTITY_TYPES` y `OWL_STATUE_BLOCK_ENTITY`
  - `MythosMortalsMenus.MENU_TYPES` y `PEGASUS_MENU` — lo consume `MythosMortalsClientEvents` en la Tarea 10
  - `MythosMortalsParticles.PARTICLE_TYPES` y `OWL_BOOM` — Tarea 10
  - `MythosMortalsEffects.MOB_EFFECTS` y `BOREAL_COURAGE`
  - `MythosMortalsRecipes.RECIPE_SERIALIZERS` y `MARINATING`
  - `MythosMortalsDataComponents.DATA_COMPONENTS` y `MARINATED`
  - `MythosMortalsStructures.STRUCTURE_TYPES`, `STRUCTURE_PIECES`, `MARKED_TEMPLATE_STRUCTURE`, `MARKED_STRUCTURE_PIECE`
- Y `MythosMortalsRegistry` reducido a `register(IEventBus)`, `registerGameplay()` y `registerPackets()`.

Seis de estas clases nacen con un solo holder. Es fragmentación deliberada, decidida en el spec: cada una es el sitio evidente donde va la siguiente partícula o el siguiente menú.

- [ ] **Step 1: Crear las siete clases**

Para cada bloque de declaraciones que queda en `MythosMortalsRegistry`, cortarlo literalmente a su clase nueva con esta forma (aquí el ejemplo real de partículas; repetir el patrón para las otras seis):

```java
package net.darkblade.mythosmortals.registry;

public final class MythosMortalsParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
        // ... la inicializacion tal cual estaba ...

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> OWL_BOOM =
        // ... tal cual estaba ...

    private MythosMortalsParticles() {
    }
}
```

Reparto exacto:

| Clase nueva | Se lleva |
|---|---|
| `MythosMortalsBlockEntities` | `BLOCK_ENTITY_TYPES`, `OWL_STATUE_BLOCK_ENTITY` |
| `MythosMortalsMenus` | `MENU_TYPES`, `PEGASUS_MENU` |
| `MythosMortalsParticles` | `PARTICLE_TYPES`, `OWL_BOOM` |
| `MythosMortalsEffects` | `MOB_EFFECTS`, `BOREAL_COURAGE` |
| `MythosMortalsRecipes` | `RECIPE_SERIALIZERS`, `MARINATING` |
| `MythosMortalsDataComponents` | `DATA_COMPONENTS`, `MARINATED` |
| `MythosMortalsStructures` | `STRUCTURE_TYPES`, `STRUCTURE_PIECES`, `MARKED_TEMPLATE_STRUCTURE`, `MARKED_STRUCTURE_PIECE` |

- [ ] **Step 2: Dejar la fachada**

Tras los cortes, `MythosMortalsRegistry` debe contener exactamente tres métodos y nada de estado. `register(bus)` queda así:

```java
    public static void register(IEventBus bus) {
        MythosMortalsEntities.ENTITY_TYPES.register(bus);
        MythosMortalsMenus.MENU_TYPES.register(bus);
        MythosMortalsBlocks.BLOCKS.register(bus);
        MythosMortalsBlockEntities.BLOCK_ENTITY_TYPES.register(bus);
        MythosMortalsParticles.PARTICLE_TYPES.register(bus);
        MythosMortalsEffects.MOB_EFFECTS.register(bus);
        MythosMortalsDataComponents.DATA_COMPONENTS.register(bus);
        MythosMortalsRecipes.RECIPE_SERIALIZERS.register(bus);
        MythosMortalsStructures.STRUCTURE_TYPES.register(bus);
        MythosMortalsStructures.STRUCTURE_PIECES.register(bus);
    }
```

`registerGameplay()` y `registerPackets()` se quedan como están salvo que sus referencias a holders pasen a la clase nueva (`MythosMortalsEntities.ATHENIAN`, `MythosMortalsEntities.PEGASUS`).

- [ ] **Step 3: Reescribir las referencias que queden**

```bash
python - <<'PYEOF'
import io, os, re
OWNER = {
    'BLOCK_ENTITY_TYPES': 'MythosMortalsBlockEntities', 'OWL_STATUE_BLOCK_ENTITY': 'MythosMortalsBlockEntities',
    'MENU_TYPES': 'MythosMortalsMenus', 'PEGASUS_MENU': 'MythosMortalsMenus',
    'PARTICLE_TYPES': 'MythosMortalsParticles', 'OWL_BOOM': 'MythosMortalsParticles',
    'MOB_EFFECTS': 'MythosMortalsEffects', 'BOREAL_COURAGE': 'MythosMortalsEffects',
    'RECIPE_SERIALIZERS': 'MythosMortalsRecipes', 'MARINATING': 'MythosMortalsRecipes',
    'DATA_COMPONENTS': 'MythosMortalsDataComponents', 'MARINATED': 'MythosMortalsDataComponents',
    'STRUCTURE_TYPES': 'MythosMortalsStructures', 'STRUCTURE_PIECES': 'MythosMortalsStructures',
    'MARKED_TEMPLATE_STRUCTURE': 'MythosMortalsStructures', 'MARKED_STRUCTURE_PIECE': 'MythosMortalsStructures',
}
for dirpath, _d, files in os.walk('src/main/java'):
    for f in files:
        if not f.endswith('.java'):
            continue
        p = os.path.join(dirpath, f)
        raw = io.open(p, encoding='utf-8', newline='').read()
        out = re.sub(r'MythosMortalsRegistry\.([A-Z][A-Z0-9_]*)',
                     lambda m: OWNER.get(m.group(1), 'MythosMortalsRegistry') + '.' + m.group(1), raw)
        if out != raw:
            io.open(p, 'w', encoding='utf-8', newline='').write(out)
            print('reescrito', p)
PYEOF
```

- [ ] **Step 4: Arreglar imports y compilar**

```bash
python tools/repackage.py fix && ./gradlew compileJava --offline
```

Esperado: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Comprobar que la fachada quedó sin estado**

```bash
grep -cE "DeferredRegister|DeferredHolder|DeferredBlock|DeferredItem" src/main/java/net/darkblade/mythosmortals/registry/MythosMortalsRegistry.java
```

Esperado: `0` fuera de la línea de `import`. Si sale más de 0, quedó algún holder sin repartir.

- [ ] **Step 6: Comprobar que los diez registros siguen ahí**

```bash
grep -c "\.register(bus);" src/main/java/net/darkblade/mythosmortals/registry/MythosMortalsRegistry.java
```

Esperado: `10`.

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "Split remaining registries out and reduce MythosMortalsRegistry to a facade"
```

---

### Task 10: Eventos común y cliente

**Files:**
- Create: `registry/MythosMortalsCommonEvents.java`
- Create: `client/MythosMortalsClientEvents.java`
- Modify: `registry/MythosMortalsRegistry.java`

**Interfaces:**
- Consumes: todas las clases de registro de las Tareas 8–9, las entidades de las Tareas 4–7 y sus modelos de cliente.
- Produces: nada que consuman tareas posteriores. NeoForge descubre ambas clases por la anotación `@EventBusSubscriber`, no por referencia desde código.

Con esto la frontera client/common queda completa: deja de haber código de cliente dentro de `registry/`.

- [ ] **Step 1: Sacar `CommonModEvents`**

Mover la clase anidada `CommonModEvents` de `MythosMortalsRegistry` a su propio archivo, de clase anidada a clase de primer nivel:

```java
package net.darkblade.mythosmortals.registry;

@EventBusSubscriber(modid = MythosMortals.MODID)
public final class MythosMortalsCommonEvents {

    // ... onAttributes, onRegisterSpawnPlacements y el resto de @SubscribeEvent, tal cual ...

    private MythosMortalsCommonEvents() {
    }
}
```

Sus métodos ya son `public static`, así que solo cambia el contenedor. Las referencias a holders ya quedaron cualificadas en las Tareas 8–9.

- [ ] **Step 2: Sacar `ClientModEvents`**

Mover la clase anidada `ClientModEvents` a `client/MythosMortalsClientEvents.java`, arrastrando las tres constantes privadas que solo ella usa:

```java
package net.darkblade.mythosmortals.client;

@EventBusSubscriber(modid = MythosMortals.MODID, value = Dist.CLIENT)
public final class MythosMortalsClientEvents {

    private static final Identifier DORI_SPEAR_TEXTURE =
        Identifier.fromNamespaceAndPath(MythosMortals.MODID, "textures/entity/dori_spear_entity.png");

    private static final ModelLayerLocation OLIVE_BOAT_LAYER =
        // ... la inicializacion tal cual estaba en MythosMortalsRegistry ...

    private static final ModelLayerLocation OLIVE_CHEST_BOAT_LAYER =
        // ... tal cual estaba ...

    // ... onRegisterLayers, onRegisterParticleProviders, onRegisterMenuScreens,
    //     onRegisterRenderers, tal cual ...

    private MythosMortalsClientEvents() {
    }
}
```

`OLIVE_BOAT_LAYER` y `OLIVE_CHEST_BOAT_LAYER` eran `public` en `MythosMortalsRegistry` sin que nadie fuera de `ClientModEvents` las leyera; aquí pasan a `private`. `DORI_SPEAR_TEXTURE` ya era `private`.

- [ ] **Step 3: Comprobar que nadie más usaba las constantes**

```bash
grep -rn "OLIVE_BOAT_LAYER\|OLIVE_CHEST_BOAT_LAYER\|DORI_SPEAR_TEXTURE" src/main/java --include=*.java
```

Esperado: todas las coincidencias dentro de `client/MythosMortalsClientEvents.java`. Si aparece alguna fuera, esa constante no puede ser `private`.

- [ ] **Step 4: Arreglar imports y compilar**

```bash
python tools/repackage.py fix && ./gradlew compileJava --offline
```

Esperado: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Comprobar que `registry/` ya no tiene código de cliente**

```bash
grep -rn "net.minecraft.client\|Dist.CLIENT" src/main/java/net/darkblade/mythosmortals/registry/
echo "codigo de salida grep: $?"
```

Esperado: ninguna coincidencia (código de salida 1). Este es el objetivo declarado de la reestructura, comprobado.

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "Extract common and client mod events from the registry"
```

---

### Task 11: Verificación final y retirada de la herramienta

**Files:**
- Delete: `tools/repackage.py`
- Modify: `docs/superpowers/specs/2026-08-24-reestructura-paquetes-design.md` (línea de estado)

**Interfaces:**
- Consumes: el árbol completo de las Tareas 2–10.
- Produces: nada. Es la tarea de cierre.

- [ ] **Step 1: Recuento de archivos**

```bash
find src/main/java -name '*.java' | wc -l
```

Esperado: `106` — los 95 originales más las 11 clases extraídas del registry (`Entities`, `Blocks`, `BlockEntities`, `Menus`, `Particles`, `Effects`, `Recipes`, `DataComponents`, `Structures`, `CommonEvents`, `ClientEvents`).

- [ ] **Step 2: Build completo**

```bash
./gradlew build
```

Esperado: `BUILD SUCCESSFUL`. Va más allá de `compileJava`: empaqueta el jar y pasa las tareas de recursos.

- [ ] **Step 3: Datagen — la comprobación que de verdad importa**

```bash
./gradlew runData
```

Esperado: `BUILD SUCCESSFUL`. Un `DeferredRegister` que se hubiera quedado fuera de `register(bus)` compila perfectamente y solo se cae aquí, al arrancar el registro de verdad. Es el riesgo principal del corte del registry.

- [ ] **Step 4: Comprobar que el datagen no cambió su salida**

```bash
git status --short src/generated
```

Esperado: sin cambios. La reestructura no debe alterar un solo JSON generado; si `src/generated` aparece modificado, algo cambió de verdad y hay que investigarlo antes de cerrar.

- [ ] **Step 5: Retirar la herramienta**

```bash
git rm tools/repackage.py
```

Cumplió su función. Queda en el historial (commit de la Tarea 1) por si hiciera falta otra migración.

- [ ] **Step 6: Marcar el spec como implementado**

En `docs/superpowers/specs/2026-08-24-reestructura-paquetes-design.md`, cambiar la línea de estado:

```markdown
**Estado:** Implementado
```

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "Verify restructure end to end and retire the migration tool"
```

---

## Notas para quien ejecute esto

**Si una fase de movimiento falla a medias**, el estado queda con parte de los `git mv` hechos y en el índice. `python tools/repackage.py fix` es idempotente y siempre se puede volver a lanzar; la forma de retroceder una fase entera es deshacer los renombrados con git, nunca editar los `package` a mano.

**El acoplamiento package-private es el riesgo transversal.** La Tarea 7 documenta el único caso conocido (cinco miembros de `PegasusEntity`), encontrado ejecutando la migración de verdad. Si aparece otro `is not public in X; cannot be accessed from outside package` en cualquier otra tarea, el patrón de resolución es el mismo: comprobar por grep quién usa el miembro, y si el único consumidor externo es la clase que acaba de mudarse, ampliar a `public` y actualizar el comentario que justificaba la visibilidad estrecha.

**No fusionar `fix` con ediciones a mano en el mismo commit** en las tareas del registry. Primero el corte, luego `fix`, luego compilar: si algo sale mal, así se distingue un error de reparto de un error de la herramienta.
