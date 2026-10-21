# Testing — Dragon Block: Tenkaichi

Tutte le prove sotto sono state eseguite davvero l'11 ottobre 2026 su Windows 11, Java 25.0.1,
Minecraft 26.3, Fabric Loader 0.19.5 + Fabric API 0.162.0+26.3, NeoForge 26.3.0.71-beta.

## 1. Build pulita (entrambi i loader)

```bash
./scripts/gw.sh clean build
```

Esito: **BUILD SUCCESSFUL**, 151 classi per JAR, asset/dati/mixin/servizi presenti in entrambi:
`build/release/dragon-block-tenkaichi-0.1.0-26.3-fabric.jar`, `...-neoforge.jar`.

## 2. Test unitari (JUnit 5, modulo common)

```bash
./scripts/gw.sh :common:test
```

19/19 superati (`BalanceTest`): monotonicità del BP, range di partenza e fine gioco, condizione e
soppressione, limiti del fattore di potenza, "un combattente più debole può vincere con la tecnica",
perfect guard, combo scaling, compressione dei moltiplicatori di forma, costi superlineari,
convergenza dell'allenamento ripetuto (anti-grinding), penalità di fatica, ricompense anti-farm,
cap delle statistiche, Zenkai, razze meccanicamente distinte.

## 3. Smoke test su server dedicato

```bash
./scripts/gw.sh :fabric:runServer -PdbtSmoke
./scripts/gw.sh :neoforge:runServer -PdbtSmoke
```

Il server si avvia fino a `Done`, applica tutti i mixin comuni, carica il chunk (0,0) forzato,
genera Maestro, Predone e un bersaglio, lascia lavorare l'IA per 400 tick, spara un Kamehameha,
fa esplodere un colpo all'aperto, poi si arresta. Esito su **entrambi i loader: RESULT PASS**
(forme da datapack, 4 tipi di danno, suoni, oggetti, tab, entità; l'IA ha visitato
IDLE→APPROACH→COMBO (Fabric) e IDLE→APPROACH→COMBO→GUARD→ENRAGE (NeoForge) e ha colpito il
bersaglio con danni della mod).

Nota: richiede `runs/server/eula.txt` (accettata dal proprietario del progetto per lo sviluppo).

## 4. Test client scriptato con screenshot

Preparazione: copiare il mondo del server di test in `runs/client/saves/dbt_test` e usare
`runs/client/options.txt` con `pauseOnLostFocus:false`.

```bash
./scripts/gw.sh :fabric:runClient -PdbtSmoke
./scripts/gw.sh :neoforge:runClient -PdbtSmoke
```

Il client entra nel mondo con Quick Play ed esegue `ClientSmokeTest`, ridotto alle novità della 0.3.0:
editor del personaggio, HUD con icone vanilla, aura con particelle pixel art, le quattro schede del menu
personaggio a schermo intero (anche dopo lo scorrimento), menu di allenamento, sfera Ki, Kamehameha e colpi in
volo; controlla che tutti gli eventi sonori della mod siano risolti dal client. Salva gli screenshot in
`runs/client/screenshots/dbt_*.png` e chiude il gioco.

Lo smoke test del server verifica la registrazione di tutti i 40 eventi sonori (compresi i quattro nuovi suoni
da scouter) e i suoni vanilla sovrapposti: passi e cadute sul blocco sotto i piedi (saltati a mezz'aria),
colpi corpo a corpo dell'IA, raggio ed esplosione.

## 5. Verificato visivamente (dagli screenshot)

- Editor del personaggio con anteprima 3D, skin procedurale, capelli e coda.
- HUD (HP, Ki, stamina, BP, TP, padronanza, slot tecniche, lock-on, barra del clash, letterbox).
- Aura in carica e aura Super Saiyan; capelli dorati e occhi verdi in forma Super Saiyan.
- Kamehameha (raggio multistrato con esplosioni lungo la traiettoria) e Beam Clash.
- Raggio viola del Predone, Maestro con barba, menu statistiche.
- Nuova UI: HUD con ritratto, barre inclinate, icone delle tecniche (X/C), hotbar con Senzu e uova; cuori e fame
  vanilla nascosti; menu e creator con pannelli/pulsanti custom (`docs/screenshots/neoforge_*`).
- Prima persona in posizione di combattimento: entrambe le braccia visibili con la skin custom (manica del gi).
- Aura Super Saiyan senza guscio a cubi; Kamehameha blu anche in forma Super Saiyan.
- Galleria delle 10 razze (`docs/screenshots/race_gallery.png`, da NeoForge; identica su Fabric):
  ogni razza ha colori, skin HD e modelli razziali distinti e riconoscibili.

- 0.2.0: Kaioken x2 (BP esattamente raddoppiato, 755.982 contro 378.173) anche sopra il Super Saiyan; prova di Karin
  con arena, conto alla rovescia, avversario presente e ritorno nel punto di partenza (0,078 blocchi su NeoForge);
  Kame House, Torre di Karin e Palazzo del Supremo; menu delle lezioni; pose di carica e di sparo di Galick Gun,
  Masenko, Makankosappo, Final Flash, Big Bang Attack e Raggio della Morte (`dbt_18*`); Maestri canonici (`dbt_07_npcs`).

- Cutscene SS2 e SS3 forzate dal test client su entrambi i loader (`dbt_19*`, `dbt_20*`): sfondo nero con mezzo busto e
  scia rossa, sfondo giallo-verde, lampo, esplosione, cielo con le nuvole in corsa (disposizione diversa a 9 tick di
  distanza), criniera SS3 vista da dietro, di fronte e con la testa alzata; forma finale corretta (SS2 / SS3).

- Galleria delle 10 acconciature canoniche in forma base e Super Saiyan (`dbt_21_hair_*`, `dbt_22_ssj_*`) su entrambi i loader.

- Outfit equipaggiato alla creazione (`outfit equipped: gi_top, gi_pants, gi_boots`) e visibile in terza e prima persona su entrambi i loader.

- Prove delle trasformazioni contro il proprio clone (SS su Namecc, SS2 sul ring del Cell Game, SS3 nel canyon) e Stanza dello
  Spirito e del Tempo vuota; contenimento nel ring (spinto 40 blocchi fuori, riportato sul bordo) su entrambi i loader.

## 6. Non ancora verificato

- Gioco con due client reali sullo stesso server dedicato (sincronizzazione tra giocatori distinti).
- Riconnessione durante una trasformazione/volo/clash, cambio di dimensione, morte durante il clash.
- Parata, perfect block, schivata, vanish, dash e recupero aereo con input umano reale.
- Sessioni di allenamento (meditazione, controllo Ki, sparring, schivate) e apprendimento del Kamehameha dal Maestro.
- Sblocco del Super Saiyan per rabbia e Zenkai in condizioni di gioco naturali.
- Passive razziali in partita (rigenerazione, assorbimento, portata allungata, furia, invisibilità ai sensi).
- JAR di release in un'installazione di produzione (launcher ufficiale) invece dell'ambiente di sviluppo.
- Prestazioni (FPS, MSPT, RAM) e benchmark: non misurati.
