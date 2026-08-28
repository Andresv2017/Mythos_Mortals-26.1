# Brief de generación: sonidos de Pegaso, Minotauro y Búho

Fecha: 2026-08-26

Continuación de [2026-08-26-entity-sounds-design.md](2026-08-26-entity-sounds-design.md). Ese
documento cubre la arpía y los soldados. Este cubre los tres mobs que quedan y **no es un plan de
implementación**: es la lista de compra para generar los `.ogg` en ElevenLabs. El cableado se hace
en una segunda pasada, cuando las muestras existan.

## Reglas que salen del documento anterior

1. **Mono obligatorio.** OpenAL sólo aplica atenuación por distancia y paneo 3D a fuentes de un
   canal. ElevenLabs devuelve estéreo; pasa por `ffmpeg -ac 1` antes de entrar en `assets/`.
2. **Los frames de `AnimSound` van en ticks**, y `durationTicks = (int)(lengthSeconds * 20)`,
   truncado.
3. **Un sonido atado a una animación `REPEATING` no puede durar más que el ciclo**, o se solapa
   consigo mismo. Es lo que ya pasa con `fly_sprint` de la arpía (ciclo efectivo 16.8 ticks contra
   un sample de 1.2 s) y está documentado como riesgo conocido. Las duraciones de abajo respetan
   este límite: la columna «Ciclo» es el techo duro.
4. Pico normalizado a **−0.5 dBFS**, que es donde está el resto de la librería.

## Reglas de prompt para ElevenLabs

- **Prompts en inglés.** El modelo de efectos responde bastante mejor.
- **Termina todos con `dry close recording, no music, no reverb, isolated`.** Minecraft aplica su
  propia atenuación y reverberación por bioma; una cola de reverb grabada hace que el mob suene
  como si estuviera siempre en una catedral, independientemente de dónde esté.
- **Sube el *prompt influence* a ~0.7.** Quieres diseño de sonido literal, no interpretación.
- **Duración mínima ~0.5 s.** Lo que la tabla pide por debajo de eso se genera a 0.5 s y se recorta
  con ffmpeg.
- Genera **3–4 variantes de cada uno** y quédate con la mejor. Para los que van en bucle (`step`,
  `wing_flap`, `charge_loop`) mira específicamente que no haya silencio ni fade en la cabeza ni en
  la cola: cualquier hueco se oye como un tartamudeo cada vuelta.

---

## Pegaso

> **Estado: integrado.** Las cinco muestras (las cuatro obligatorias más la ambient opcional) están
> en `assets/mythosmortals/sounds/entity/pegasus/`, registradas y cableadas. Lo de abajo queda como
> registro de lo que se pidió y por qué.

**Es un caballo.** Los sonidos vanilla de caballo son buenos y ya están puestos en el código; lo que
vanilla no tiene son alas de pluma. La recomendación es generar sólo cuatro muestras y dejar el
resto en vanilla.

### Cómo se corrigió `take_off`

La primera generación sonaba a criatura gigante. El análisis por bandas explicó por qué: la banda de
20–80 Hz estaba a −13.2 dB y la de 200–800 Hz a −37.3 dB, o sea **24 dB de diferencia**. El tamaño
percibido de una criatura se codifica casi entero en dónde se apoya el fundamental, y un sample que
es prácticamente sólo sub-graves no puede leerse como algo del tamaño de un caballo.

No se regeneró: se procesó. Filtro paso alto de 24 dB/oct a 95 Hz para quitar el sub, más realces en
450 Hz, 1.2 kHz y 2.5 kHz para devolver el cuerpo y el roce de casco que estaban 24 dB por debajo.
El pico espectral se movió de 20–80 Hz a **80–200 Hz**, que es donde resuena un cuerpo de ese
tamaño. Se probaron tres variantes — sólo EQ, EQ con pitch +20 % y con pitch +40 % (varispeed, el
truco clásico para encoger una criatura) — y se eligió la de sólo EQ, que conserva la interpretación
original.

Lección general para las tandas que quedan: **generar por debajo de ~1 s produce sólo el
transitorio**, sin cuerpo, que es lo que hizo que la primera versión de `wing_flap` sonara a bolsa de
plástico. Genera con holgura y recorta después; el techo de duración es del asset final, no de la
generación.

### Se queda en vanilla

| Ranura | Evento vanilla | Estado |
|---|---|---|
| ambient | `entity.horse.ambient` | falta engancharlo (`getAmbientSound` no está sobrescrito) |
| hurt | `entity.horse.hurt` | falta engancharlo |
| death | `entity.horse.death` | falta engancharlo |
| pisada | `entity.horse.step` | falta engancharla a `walk` / `sprint` |
| corcoveo / doma fallida | `entity.horse.angry` | ya está en el código |
| comer | `entity.horse.eat` | ya está |
| ensillar / brida | `entity.horse.saddle` | ya está |
| resoplido de vínculo | `entity.horse.breathe` | ya está |

### A generar

| Fichero | Duración | Ciclo (techo) | Prompt |
|---|---|---|---|
| `pegasus/wing_flap.ogg` | **0.5 s** | `fly_sprint` 10 ticks = 0.5 s | `Single powerful downstroke of a large feathered wing, deep whoomph of displaced air with a crisp feather rustle on the leading edge, like a swan taking off, dry close recording, no music, no reverb, isolated` |
| `pegasus/take_off.ogg` | **1.0 s** | `take_off` 15 ticks, PLAY_ONCE | `A large winged horse launching from the ground: hooves shoving off packed dirt, then two rapid heavy feathered wingbeats pushing air downward, gritty scuff into a whooshing gust, dry close recording, no music, no reverb, isolated` |
| `pegasus/landing.ogg` | **1.2 s** | `landing` 25 ticks, PLAY_ONCE | `A large winged animal flaring its wings to brake and touching down, a broad feathered wing flare followed by four heavy hooves landing on packed earth in quick succession with dust and grit, dry close recording, no music, no reverb, isolated` |
| `pegasus/dash.ogg` | **1.0 s** | evento, sin animación | `A sudden burst of wind as something large accelerates away, a compressed whoosh with a low pressure thump at the front and an airy tail, powerful and magical but not musical, dry close recording, no music, no reverb, isolated` |

`hit_ground` (el cadáver cayendo, 11 ticks) reutiliza `landing` a más volumen, igual que hace la
arpía. No hace falta muestra propia.

**Opcional.** Si no quieres que el pegaso suene exactamente como un caballo vanilla, la única que
merece la pena sustituir es el ambient:

`pegasus/ambient.ogg`, **1.5 s** — `A horse whinny with an ethereal airy quality, breathy and noble,
a single call with a soft resonant tail, dry close recording, no music, no reverb, isolated`

`BREEZE_WIND_CHARGE_BURST` (el dash actual) funciona, pero se lee como el objeto de la brisa. Por eso
`dash.ogg` está en la lista principal y no en las opcionales.

---

## Minotauro

**Ahora mismo no tiene ni un sonido**, ni siquiera placeholder vanilla. Es el mob más vacío de los
tres. La buena noticia es que vanilla tiene dos familias que le sirven casi literalmente: el
*ravager* (pisada) y la *goat* (preparar y ejecutar la embestida).

> **Aviso sobre los frames.** Ocho de sus trece animaciones son `sinKeyframes(...)` — `AnimSource`
> a `null`, marcadas con `debug.markMissing`. La animación sigue avanzando en ticks, así que los
> sonidos dispararían bien, pero los frames de impacto habrá que revisarlos cuando existan los clips
> reales. Afecta a `charge_loop`, `death`, `target_spotted`, `attack_vertical`, `attack_push`,
> `charge_start`, `charge_hit` y `charge_stun`.

### Se queda en vanilla

| Ranura | Evento vanilla | Por qué |
|---|---|---|
| pisada | `entity.ravager.step` | pezuña pesada sobre tierra, es exactamente eso |
| inicio de embestida | `entity.goat.prepare_ram` | vanilla ya tiene el sonido de «animal cornúpeta que va a embestir» |
| impacto de embestida | `entity.goat.ram_impact` | mismo caso, y el golpe seco encaja con `charge_hit` (10 ticks) |
| aturdimiento tras embestir | `entity.ravager.stunned` | literalmente el mismo estado que `charge_stun` |

Si `entity.ravager.step` te suena demasiado a ravager, es la primera candidata a sustituir; por eso
está también abajo, marcada como opcional.

### A generar

| Fichero | Duración | Ciclo (techo) | Prompt |
|---|---|---|---|
| `minotaur/ambient.ogg` | **1.5 s** | evento, intervalo vanilla 80 ticks | `A huge bull-like monster breathing and snorting while idle, a deep chesty rumble ending in a wet nostril snort, low frequency and threatening, dry close recording, no music, no reverb, isolated` |
| `minotaur/roar.ogg` | **1.5 s** | `target_spotted` 20 ticks, PLAY_ONCE | `A massive bull-headed monster roaring in rage, a deep guttural bellow with a bovine lowing core that rises into a distorted shout, enormous chest, dry close recording, no music, no reverb, isolated` |
| `minotaur/hurt.ogg` | **0.6 s** | evento | `A huge bull-like monster taking a wound, a short pained guttural grunt cut off sharply, deep and wet, dry close recording, no music, no reverb, isolated` |
| `minotaur/death.ogg` | **2.0 s** | `death` 40 ticks | `A massive bull-headed monster dying, a long collapsing bellow falling in pitch into an exhausted wet breath, ending with the thud of a heavy body hitting the ground, dry close recording, no music, no reverb, isolated` |
| `minotaur/swing.ogg` | **0.8 s** | combo A/B 16 ticks = 0.83 s | `A huge double-headed battle axe swung hard through the air, a deep heavy whoosh with a metallic edge tone, slow and weighty, ending in a sharp snap, dry close recording, no music, no reverb, isolated` |
| `minotaur/slam.ogg` | **1.2 s** | `attack_vertical` 35 ticks, impacto en tick 17 | `A giant battle axe brought down overhead and smashing into the ground, a heavy whoosh into a massive blunt impact with stone cracking and debris scattering, dry close recording, no music, no reverb, isolated` |
| `minotaur/push.ogg` | **0.6 s** | `attack_push` 12 ticks | `A huge monster shoving forward with both arms, a short explosive grunt with a low air thump of displaced weight, dry close recording, no music, no reverb, isolated` |
| `minotaur/charge_loop.ogg` | **0.5 s exactos, en bucle** | `charge_loop` 10 ticks, REPEATING | `Seamlessly looping gallop of a huge bull monster charging at full speed, two heavy hoof impacts on packed dirt with strained snorting breath underneath, perfectly loopable with no fade in or out, dry close recording, no music, no reverb, isolated` |

**`charge_loop` es la única que necesita bucle perfecto.** Si ElevenLabs te ofrece la opción *loop*
en efectos de sonido, actívala para ésta. Es `REPEATING` a 10 ticks y cualquier silencio en los
extremos se oye como un hueco dos veces por segundo.

**Opcional** — `minotaur/step.ogg`, **0.35 s** (genera a 0.5 s y lo recorto):

`A single very heavy hoof stomping on packed dirt, a deep low thud with grit, the weight of a
two-ton animal, one hit only, dry close recording, no music, no reverb, isolated`

El techo de 0.35 s no es negociable: `run` dura 15 ticks con dos contactos, o sea 7.5 ticks (0.375 s)
entre pisada y pisada. Cualquier cosa más larga se pisa a sí misma al correr. Si sale demasiado
corta para sonar con peso, la alternativa es lo que se hizo con el soldado — un sample por pie, tres
variantes tras un solo evento — pero probablemente `entity.ravager.step` te ahorre el trabajo.

---

## Búho de Atenea

**El que más trabajo necesita.** Vanilla no tiene búho: el loro es tropical, el allay es de campanas
y el murciélago es demasiado pequeño. La voz entera hay que generarla. Lo único vanilla defendible
es el ataque sónico.

### Se puede quedar en vanilla

| Ranura | Evento vanilla | Matiz |
|---|---|---|
| carga sónica | `entity.warden.sonic_charge` | ya está en el código y funciona muy bien |
| grito sónico | `entity.warden.sonic_boom` | ídem, pero está fuertemente asociado al Warden |

Suenan genial y son temáticamente correctos. La única razón para sustituirlos es identidad propia:
ahora mismo el ataque estrella del búho suena a otro mob. Los prompts para reemplazarlos están al
final, como opcionales.

### A generar

| Fichero | Duración | Ciclo (techo) | Prompt |
|---|---|---|---|
| `owl/ambient.ogg` | **1.5 s** | evento, intervalo vanilla 80 ticks | `A large owl hooting at night, two deep resonant hoots, breathy and hollow with a soft tail, a single bird, dry close recording, no music, no reverb, isolated` |
| `owl/hurt.ogg` | **0.5 s** | evento | `A large owl shrieking in pain, a short sharp raspy screech cut off abruptly, dry close recording, no music, no reverb, isolated` |
| `owl/death.ogg` | **1.0 s** | `death_falling` 10 ticks, REPEATING con `.once()` | `A large owl final cry, a raspy screech falling in pitch into a weak breathy hoot that dies out, dry close recording, no music, no reverb, isolated` |
| `owl/wing_flap.ogg` | **0.5 s** | `fly_sprint` 10 ticks = 0.5 s | `A single soft owl wingbeat, a muffled velvety feathered whoosh of air, almost silent the way owl flight is, close mic, dry close recording, no music, no reverb, isolated` |
| `owl/dive.ogg` | **0.8 s** | `dive_attack` 10 ticks, REPEATING con `.once()` | `A large owl screeching as it dives to attack, a piercing raspy shriek with a rush of air underneath, aggressive and sharp, dry close recording, no music, no reverb, isolated` |
| `owl/dive_return.ogg` | **0.5 s** | `dive_attack_return` 4 ticks, PLAY_ONCE | `A large bird pulling hard out of a dive, two fast strained wingbeats fighting the air, a taut feathered whoosh, dry close recording, no music, no reverb, isolated` |
| `owl/awake.ogg` | **2.8 s** | `awake` 55 ticks, PLAY_ONCE | `A carved stone statue coming to life: fine cracks spreading through marble, dust and small fragments falling away, then the grinding of stone joints moving for the first time, ending in a low magical resonance, dry close recording, no music, no reverb, isolated` |

`hit_ground` (20 ticks) reutiliza `owl/dive_return.ogg` o vanilla `entity.generic.big_fall`; no
necesita muestra propia.

`wing_flap` tiene un techo de 0.5 s por `fly_sprint`, pero el detalle importante es el otro: el vuelo
del búho es silencioso, ése es su rasgo característico. Generado demasiado fuerte pierde justo lo que
lo distingue de la arpía. Al cablearlo irá a volumen bajo, sobre 0.35.

`awake` es la muestra con más carácter de las diecinueve: es el momento en que una estatua de mármol
se convierte en un animal vivo, y dura 2.8 s de reloj. Merece varias generaciones.

### Opcional: sustituir el sónico del Warden

| Fichero | Duración | Anclaje | Prompt |
|---|---|---|---|
| `owl/sonic_charge.ogg` | **0.7 s** | `sonic_screech` tick 0 → release en tick 14 | `A rising energy charge, air being pulled inward with a building high pitched resonant tone, ancient and divine rather than electronic, tension building to a peak, dry close recording, no music, no reverb, isolated` |
| `owl/sonic_boom.ogg` | **1.2 s** | `sonic_screech` tick 14 | `A concentrated blast of sound fired forward in a beam, a piercing shriek fused with a low pressure shockwave punching through the air, ending with a long airy tail, dry close recording, no music, no reverb, isolated` |

Los 0.7 s de la carga no son arbitrarios: `SONIC_TOTAL_TICKS = 26` y `SONIC_RELEASE_TICK = 14`, así
que la carga tiene exactamente 14 ticks antes de que salga el rayo. Más larga y la cola sigue
sonando por debajo del disparo.

---

## Resumen

| Mob | A generar | Vanilla | Opcionales |
|---|---|---|---|
| Pegaso | 4 | 8 | 1 |
| Minotauro | 8 | 4 | 1 |
| Búho | 7 | 2 | 2 |

**19 muestras obligatorias, 4 opcionales.** El pegaso es el más barato y el búho el más caro.

Si sólo vas a generar una tanda corta, el orden por impacto es: el minotauro entero (es el único que
hoy no hace ningún ruido), luego `owl/awake` y la voz del búho, y por último las alas del pegaso.

## Después

Cuando las muestras estén en `~/Downloads/MMSounds`, la segunda pasada hace lo mismo que la tanda de
la arpía: `ffmpeg -ac 1` y normalizado a −0.5 dBFS, copia a
`src/main/resources/assets/mythosmortals/sounds/entity/<mob>/`, eventos nuevos en
`MythosMortalsSounds`, entradas en `sounds.json`, subtítulos en el provider `Lang` de
`MythosMortalsDatagen`, y el cableado por animación con `AnimSound` más los hooks de
`getAmbientSound` / `getHurtSound` / `getDeathSound`.
