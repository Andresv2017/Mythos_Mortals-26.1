# Sonidos de entidad para Arpía y soldados (Ateniense / Espartano)

Fecha: 2026-08-26

## Objetivo

Integrar los diez `.ogg` de `~/Downloads/MMSounds` como sonidos de entidad, disparados desde
las animaciones existentes mediante el soporte `AnimSound` de DeluxeLib, en lugar de los
placeholders vanilla que hay hoy.

## Contexto verificado

DeluxeLib (`curse.maven:deluxe-lib-1594910:8728008`) expone:

- `BaseAnimation.sound(float frame, SoundEvent)` y `BaseAnimation.sound(AnimSound)`.
- `AnimSound.at(frame, sound)` con `.volume()`, `.pitch()`, `.pitchJitter()`, `.category()`,
  `.condition(Predicate<LivingEntity>)` y `.once()`.

Detalles confirmados desensamblando el jar, porque condicionan el diseño:

1. `BaseAnimation.tick(int)` calcula `client = animator.getEntity().level().isClientSide()` y
   **salta** tanto `frameEvents` como `tickSounds()` cuando es cliente. Los sonidos son
   servidor-autoritativos: `AnimSound.play` llama a
   `Level.playSound(null, x, y, z, sound, category, volume, pitch)`, que difunde a todos los
   clientes. No hay doble reproducción y no hace falta guardia de lado en el código del mod.
2. `durationTicks = (int)(lengthSeconds * 20)` — truncado, no redondeado.
3. El `frame` de `AnimSound` está **en ticks**, no en segundos.
4. Lógica de repetición en `tickSounds()`:
   `if (once || !loop.repeats()) { if (cycle != 0) skip; }`.
   Es decir, en una animación `REPEATING` un sonido normal suena **en cada vuelta**, y uno
   marcado `.once()` suena **sólo en la primera**. Esto es lo que permite que
   `guard`/`dive_attack`/`death_falling` (todas cíclicas) emitan un único sonido de entrada.
5. Si `pitch` no se fija explícitamente, se usa `LivingEntity.getVoicePitch()`.

Estado actual del proyecto: no existe `MythosMortalsSounds`, ni `assets/mythosmortals/sounds.json`,
ni carpeta `sounds/`. Los ataques usan `SoundEvents.PLAYER_ATTACK_SWEEP` vía
`.onFrame(n, e -> e.playSound(...))` en `AthenianEntity` y `SpartanEntity`.

## Decisiones tomadas

- **`soldier_walk.ogg` se trocea.** El clip dura 3.2 s y contiene tres pisadas (onsets medidos
  sobre la envolvente RMS en 0.335 s, 1.105 s y 2.455 s), mientras que el ciclo `walk` dura
  1.3514 s. Reproducirlo entero se solaparía consigo mismo y se desincronizaría de los pies. Se
  corta en tres samples que se registran como variantes del mismo evento, y Minecraft elige una
  al azar en cada pisada.
- **Los sonidos `soldier_*` son compartidos** entre Ateniense y Espartano: un solo juego de
  `SoundEvent` bajo `entity.soldier.*`, un solo juego de `.ogg` en el jar.
- **`soldier_attack` sustituye a `PLAYER_ATTACK_SWEEP`**, en el mismo frame en que estaba.

## Assets

Troceado de `soldier_walk.ogg` con ffmpeg, con fade de 8 ms a la entrada y a la salida para
evitar clicks en los cortes:

| Sample | Recorte |
|---|---|
| `step1` | 0.320 s – 0.960 s |
| `step2` | 1.085 s – 1.700 s |
| `step3` | 2.440 s – 3.000 s |

Destino (`src/main/resources/assets/mythosmortals/sounds/`):

```
entity/arpy/attack.ogg        <- arpy_attack.ogg
entity/arpy/death.ogg         <- arpy_death.ogg
entity/arpy/fly.ogg           <- arpy_fly.ogg
entity/arpy/landing.ogg       <- arpy_landing.ogg
entity/soldier/attack.ogg     <- soldier_attack.ogg
entity/soldier/block.ogg      <- soldier_block.ogg
entity/soldier/death1.ogg     <- soldier_-dead1.ogg
entity/soldier/death2.ogg     <- soldier_dead2.ogg
entity/soldier/poise_break.ogg<- soldier_poise_break.ogg
entity/soldier/step1..3.ogg   <- recortes de soldier_walk.ogg
```

## `assets/mythosmortals/sounds.json`

Diez eventos, todos con `subtitle`. `entity.soldier.step` agrupa los tres samples de pisada.

## `MythosMortalsSounds`

Clase nueva en `registry/`, siguiendo el patrón de `MythosMortalsParticles`:
`DeferredRegister<SoundEvent>` sobre `Registries.SOUND_EVENT`, con un helper privado que
construye cada `SoundEvent.createVariableRangeEvent(Identifier)`. Se engancha en
`MythosMortalsRegistry.register(IEventBus)`.

## Cableado por animación

Ticks derivados de `(int)(withLength * 20)`. Los frames de contacto del pie salen del bob
vertical del hueso `waist` y de la Y de `left_leg` / `right_leg` en cada clip.

### Ateniense y Espartano

`walk` (1.3514 s → 27 ticks): `waist` Y toca mínimo en 0.0 s y 0.6757 s → contactos en tick 0 y 13.

`guard_left` / `guard_right` (2.0 s → 40 ticks): el pie guía planta en 1.5 s
(`left_leg`/`right_leg` Y = -0.12, `waist` en su mínimo -4.1 / -4.22) → tick 30; el pie que
arrastra se asienta en 2.0 s ≡ 0.0 s → tick 0. El de tick 0 va más flojo porque es un arrastre.

| Animación | Ticks | Sonido | Frame(s) | Notas |
|---|---|---|---|---|
| `attack` | 16 (At.) / 15 (Es.) | `soldier.attack` | 2 | sustituye `PLAYER_ATTACK_SWEEP` |
| `attack_slice` | 15 | `soldier.attack` | 4 (At.) / 3 (Es.) | sustituye `PLAYER_ATTACK_SWEEP` |
| `walk` | 27 | `soldier.step` | 0, 13 | vol 0.85 |
| `run` | 10 | `soldier.step` | 0, 5 | vol 1.0 |
| `guard_left` | 40 | `soldier.step` | 0 (vol 0.45), 30 (vol 0.7) | shuffle lateral |
| `guard_right` | 40 | `soldier.step` | 0 (vol 0.45), 30 (vol 0.7) | shuffle lateral |
| `guard` | 40 | `soldier.block` | 0 `.once()` | sólo al alzar escudo |
| `death` | 43 (At.) / 40 (Es.) | `soldier.death1` | 0 | |
| `death2` | 40 | `soldier.death2` | 0 | |
| `guard_break` | 30 | `soldier.poise_break` | 0 | |
| `guard_break2` | 40 | `soldier.poise_break` | 0 | |

`pitchJitter(0.08F)` en pasos y ataques para que dos soldados juntos no suenen clonados.

### Arpía

| Animación | Ticks | Sonido | Frame | Notas |
|---|---|---|---|---|
| `dive_attack` | 20, REPEATING | `arpy.attack` | 0 `.once()` | un chillido por picado |
| `take_off` | 30 | `arpy.fly` | 0 | vol 1.0 |
| `idle_fly` | 21, REPEATING | `arpy.fly` | 0 | vol 0.6, aleteo continuo |
| `fly_sprint` | 20, REPEATING | `arpy.fly` | 0 | vol 0.5 |
| `fly_sprint_angry` | 20, REPEATING | `arpy.fly` | 0 | vol 0.5 |
| `landing` | 10 | `arpy.landing` | 0 | |
| `hit_ground` | 30 | `arpy.landing` | 0 | impacto del cadáver |
| `death_ground` | 47 | `arpy.death` | 0 | |
| `death_ground2` | 48 | `arpy.death` | 0 | |
| `death_falling` | 21, REPEATING | `arpy.death` | 0 `.once()` | un grito, no uno por vuelta |

## Subtítulos

Claves `subtitles.mythosmortals.*` añadidas al provider `Lang` de `MythosMortalsDatagen`, y
`en_us.json` regenerado con el run `data`.

## Riesgos conocidos

- `fly_sprint` corre a `playbackSpeed(1.2F)`, así que su ciclo efectivo es ~16.8 ticks (0.84 s)
  mientras `arpy_fly.ogg` dura 1.2 s: las colas se solapan. A volumen 0.5 eso lee como lecho de
  viento continuo, apropiado para un mob volador, pero queda documentado en un comentario en
  `ArpyEntity` para poder quitarlo de una línea si suena embarrado.
- No hay samples de *hurt* ni *ambient*, así que no se tocan `getHurtSound` / `getAmbientSound`.

## Verificación

- `./gradlew build` compila.
- `./gradlew runData` regenera `en_us.json` con los subtítulos nuevos, sin otros diffs.
- Los diez `.ogg` (doce ficheros contando los tres cortes de pisada) presentes bajo
  `assets/mythosmortals/sounds/` y todas las rutas de `sounds.json` resueltas contra ficheros
  existentes.
