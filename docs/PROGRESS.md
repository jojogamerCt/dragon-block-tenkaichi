# Progress — Dragon Block: Tenkaichi

## Sessione 1 — 11 ottobre 2026

### Implementato
- Repository multi-loader (common/fabric/neoforge/build-logic/tools/scripts/docs) su toolchain
  verificata per Minecraft 26.3; due JAR di release con nome `dragon-block-tenkaichi-<ver>-26.3-<loader>.jar`.
- Seam piattaforma via ServiceLoader (registrazioni, rete, allegati persistenti, permessi blocchi).
- Dati giocatore persistenti e versionati; stato runtime autoritativo; sincronizzazione differenziale.
- Statistiche (12), Battle Power, progressione anti-grinding, fatica, pratica, Zenkai.
- Combattimento: combo, colpi pesanti contestuali, counter, rush, guard break, clash corpo a corpo,
  parata/perfect block, schivata/vanish/teleport, dash, knockdown, recupero aereo, lock-on.
- Volo predetto lato client con validazione server; atterraggi con onda d'urto; bang sonico.
- Ki: carica, rigenerazione, esaurimento; aura 3D; Ki Blast; Kamehameha; Raider Cannon; Beam Clash.
- Distruzione ambientale a 4 modalità con budget e protezioni.
- Trasformazioni da datapack; Super Saiyan con sblocco per rabbia, cinematica, padronanza.
- NPC: Maestro (allenamenti, insegnamento) e Predone Saiyan (FSM gerarchica, 10 stati, seconda fase).
- Client: editor personaggio, skin procedurale, 10 acconciature 3D, coda, animazioni a keyframe,
  HUD, menu personaggio con impostazioni, schermata di allenamento con minigiochi, effetti di
  combattimento (hitstop, scossa camera, particelle), mixin di input.
- Asset originali generati da script: 257 chiavi EN/IT, 50 suoni OGG (34 eventi), texture.
- Comandi `/tenkaichi`, configurazioni comune/client.

### Testato
- Build pulita di entrambi i loader, 19/19 test JUnit.
- Smoke test su server dedicato Fabric e NeoForge: PASS.
- Test client scriptato con screenshot su Fabric e NeoForge: tutti i passaggi completati.
- Dettagli e limiti in `TESTING.md`.

### Problemi trovati e risolti durante la sessione
- Certificati HTTPS intercettati dall'antivirus → wrapper Gradle con trust store di Windows.
- `CreativeModeTab.Output` protetto nel common → access transformer (i loader lo rendono già pubblico).
- Mixin anti-kick non applicabile su Fabric → riscritto su `tick()`.
- Smoke test che non faceva "tickare" le entità senza giocatori → chunk forzato e bersaglio persistente.
- Aura troppo opaca, layout HUD sovrapposto, testo del creator fuori schermo, sfera del clash troppo
  grande → corretti e riverificati con nuovi screenshot.

### Mancante (prossimi passi consigliati)
1. Test multiplayer con due client reali e prove di riconnessione/cambio dimensione.
2. Benchmark (FPS/MSPT) e ottimizzazione di aura/raggi.
3. Altre tecniche (Galick Gun, Masenko, Destructo Disc, Special Beam Cannon) e Super Saiyan 2/3.
4. Spawn naturale del Predone e strutture iniziali; trasformazioni per le razze non Saiyan.
5. Fasi 7–10 della roadmap.

### Verifiche fallite ancora aperte
Nessuna verifica automatica fallisce allo stato attuale.

## Sessione 2 — 11 ottobre 2026

### Implementato
- Tutte le 10 razze abilitate e giocabili (migrazione della config comune allo schema 2).
- `RaceMechanics`: ogni passiva razziale ha un effetto server-side (rigenerazione pagata in Ki,
  assorbimento alle uccisioni, portata allungata, corpo elastico, sopravvivenza nello spazio,
  invisibilità ai sensi dei nemici, furia demoniaca, Ki divino, fame di battaglia, maestria tecnica,
  riserva di energia, potenziale nascosto).
- Aspetto per razza: colori predefiniti, palette della pelle, "colore dettagli" sincronizzato in rete.
- Skin procedurali HD 128×128 con abiti 3D sui layer esterni; dettagli razziali sulla skin.
- Nuovi modelli: tratti razziali (3 gruppi di colore), coda spessa, spalline dell'armatura, barba;
  ciocche dei capelli curve a 4 segmenti; coda Saiyan a 8 segmenti; nuove texture HD.
- Nuova icona della mod fornita dal proprietario del progetto.

### Testato
- Build pulita, 19/19 JUnit; smoke server Fabric e NeoForge: PASS.
- Test client su entrambi i loader con galleria delle 10 razze: tutti i passaggi completati.

### Problemi trovati e risolti
- Il creator sostituito da un altro creator azzerava l'anteprima (`removed()` dopo il costruttore)
  → l'anteprima viene ripubblicata in `init()`.

## Sessione 3 — 12 ottobre 2026

### Implementato
- Mod rinominata in **Dragon Block: Tenkaichi** (mod id invariato `dbtenkaichi`), autori Sh4d0wJ0J0 e Claude,
  descrizione "creata interamente con Claude"; JAR `dragon-block-tenkaichi-0.1.0-26.3-<loader>.jar`; versione
  lasciata a 0.1.0 su richiesta.
- Nuova licenza: addon consentiti, ripubblicazione e guadagno sulla mod vietati.
- UI completamente personalizzata (sprite GUI a 9-slice, `TenkaichiUi`, `TenkaichiButton`), HUD ridisegnato con
  ritratto e icone delle tecniche, barre vanilla di vita/fame/armatura nascoste.
- 8 tipi di particelle registrati (`ModParticles`, provider su Fabric e NeoForge) al posto di quelle vanilla.
- Aura ridisegnata con fiamme sfumate, rimosso il guscio a cubi.
- Kamehameha di colore fisso; nuove texture per Senzu e uova; icone delle tecniche.
- Skin: spalle, parte superiore delle gambe e base del busto coperte dagli abiti; maniche corte nel gi.
- Prima persona: braccia in guardia e colpi visibili in posizione di combattimento; mani con skin custom.
- `/tenkaichi setstat <giocatore> all <valore>`.
- Bilanciamento: Predone con danni ×0,6, statistiche ridotte e combo più lente; stordimento dei colpi NPC sui
  giocatori ridotto del 45% e combo breaker dopo 3 colpi.
- Allenamento: il Maestro viene cercato entro 40 blocchi durante una sessione (prima 8, causa della fine
  prematura); sparring 60 s, schivata 45 s; il Maestro della schivata mantiene una distanza fra 6 e 14 blocchi.

### Testato
- Build pulita, 19/19 JUnit; smoke server Fabric e NeoForge: PASS.
- Test client su entrambi i loader: tutti i passaggi completati (24 screenshot), nessun errore di rendering o mixin.

### Problemi trovati e risolti
- Aura base che formava un "cappuccio" conico sopra la testa → raggio non più rastremato e dissolvenza anticipata.
- Bagliori del raggio troppo grandi → ridotti.
- Testo troncato nei pulsanti stretti → ridimensionamento automatico del testo.

### Aggiornamento — Razza di Freezer
- Aspetto ridisegnato sulla prima forma (stile Freezer/Frost/Cooler base): pelle rosa-lilla, corazza
  biologica bianca (cupola, guance, spalle, petto, stinchi), gemme nel colore dettagli (viola di default,
  blu disponibile), corna scure, occhi rossi con segni scuri, labbra viola, coda spessa.
- Nuovo outfit "Naturale" (default per questa razza); placche e gemme del corpo visibili solo con esso.
- Verificato con la galleria del test client su Fabric e NeoForge (`docs/screenshots/frieza_race_first_form.png`).
- Primo tentativo: corna ruotate verso l'interno della testa → corretta la rotazione.

### Aggiornamento — Bio-Androide
- Aspetto ridisegnato sullo stile di Cell (forma imperfetta): macchie scure su tutto il corpo, viso pallido
  con segni viola sulle guance e bocca a becco, cresta nera a corona, esoscheletro scuro e segmentato su
  petto/addome/spalle/ginocchia, ali-guscio sulla schiena, coda a pungiglione dedicata; outfit Naturale di default.
- Area di anteprima del creator allargata (le creste e le corna alte venivano tagliate) e nome della razza
  disegnato sopra il modello.
- Verificato con la galleria del test client su Fabric e NeoForge (`docs/screenshots/bio_android_cell_style.png`).

### Aggiornamento — Posa di carica della Kamehameha
- Nuova posa di carica: busto ruotato, gambe larghe, braccia tirate indietro con le mani unite sul fianco
  destro, leggero tremito; in prima persona le mani a coppa in basso a destra; particelle di carica
  spostate fra le mani.
- Il test client cattura la posa di tre quarti, di lato (anteprima del giocatore durante la carica) e in prima persona
  (`docs/screenshots/kamehameha_charge_pose.png`).
- Tentativi intermedi: braccia che pendevano, poi braccia sollevate (la rotazione laterale con il busto molto ruotato
  avviene negli assi del mondo) → angoli ricalcolati con torsione moderata; mani in prima persona fuori inquadratura → riposizionate.

### Aggiornamento — Cutscene, suoni, posa Kamehameha
- Posa della Kamehameha rifatta ruotando tutto il modello (non solo il busto) e controruotando la testa: le braccia
  restano davanti al busto e le mani si uniscono sul fianco destro (confronto con il riferimento del proprietario).
- Nuovo sistema di cutscene (`client/cinematic`): script per forma con camera libera, eventi e tracce. Super Saiyan:
  primo piano tremante → campo lungo con fulmini → figura intera con terreno che si spacca e rocce che levitano →
  capelli che lampeggiano → urlo con lampo bianco → di spalle nella colonna d'aura dorata → rivelazione.
  Il riferimento è stato studiato da fotogrammi del video fornito (non inclusi nel progetto).
- Nuova particella "roccia", fulmini a colonna, oscuramento del cielo, flash; durata cinematica massima portata a
  200 tick (migrazione della config comune allo schema 3).
- Generatore audio riscritto (`tools/gen_sounds.py`, richiede numpy, scipy, soundfile); due nuovi eventi
  (`transform.thunder`, `transform.rumble`).
- Problemi trovati: polvere con bordi quadrati e troppo densa, cielo poco scuro, troppe rocce, urlo troppo rumoroso
  nello spettro, picco del tuono oltre 0 dBFS → tutti corretti e ricontrollati.
- Verificato su Fabric e NeoForge: smoke server PASS, test client completo (36 screenshot), forma Super Saiyan applicata
  al termine della cutscene.

### Aggiornamento — 0.2.0
- Kaioken combinabile, acquisto delle tecniche, Maestri canonici con lezioni e prove nella dimensione `training_realm`,
  luoghi iconici con i Maestri, pose canoniche per ogni tecnica, Makankosappo a spirale, Karin come gatto bianco,
  emblema del gi rimosso, aura riprogettata, SS2/SS3 e sei nuove tecniche.
- Test: smoke test del server PASS su Fabric e NeoForge (mondi nuovi); test client completo PASS su entrambi i loader,
  con la sequenza delle tecniche (stili ricevuti dal client) e il ritorno dalla prova (0,078 blocchi su NeoForge).
- Problemi trovati e risolti: altezza del terreno letta prima di caricare il chunk, pulsanti duplicati nel menu del
  Maestro, sabbia dell’isola senza supporto, cima di Karin in ombra (luci invisibili), aura trasparente, braccia
  incrociate in prima persona, nomi dei Maestri non ripristinati al caricamento, statura degli NPC non applicata.
- Un avvio del client NeoForge si è chiuso con un crash nativo del driver (0xC0000005) prima del caricamento della mod;
  il secondo avvio è passato.
- Cutscene canoniche SS2 (sfondo nero, scia rossa, sfondo giallo-verde, lampo, esplosione) e SS3 (nuvole vanilla
  spostate da un mixin sul `CloudRenderer` più sbuffi di nuvole, lampi, crescita dei capelli, lampo finale), criniera SS3
  ricostruita a ciocche e compensata sull’inclinazione della testa. Verificate su Fabric e NeoForge.
- Acconciature canoniche (10) con capelli trasformati per acconciatura e migrazione dei personaggi esistenti (schema dati 2).
- Outfit come capi indossabili (9 oggetti tingibili) con livello di rendering dedicato; capelli di Goku rifatti sul riferimento; SSJ di Goku con punte verticali e SSJ2 con punte rigide; sfondo del creatore senza bande (`stretch_inner`).
- Sblocco delle trasformazioni con Potenziale e prova contro se stessi, nuove arene tematiche (Kame Island, terra sacra di Karin, Namecc, ring del Cell Game, canyon), arene separate per tipo e contenimento nel ring.
- BP base bilanciati per le trasformazioni, presentazione femminile (busto, corporatura, ciglia, labbra, acconciature), pulsanti Prova e requisiti nel menu delle forme.

### Aggiornamento — 0.3.0 (21 ottobre 2026)
- Nuova direzione artistica "Dragon Ball x vanilla": sprite pixel art con contorno e smusso (tools/gen_style.py), sfondo con cielo, stelle, Sfera del Drago a 4 stelle e nuvole a blocchi, titoli in stile logo, HUD con icone vanilla di cuore, fame e armatura.
- Menu personaggio (U) a schermo intero in stile Genshin: schede a sinistra, personaggio su piedistallo al centro, pannello scorrevole a destra.
- Particelle pixel art non ruotate, nucleo a cubo nelle sfere Ki e a prisma nei raggi.
- Suoni rigenerati (tools/gen_sounds.py) con finitura asciutta e retrò, quattro nuovi suoni da scouter per i menu, suoni vanilla sovrapposti lato server.
- Test: smoke test del server PASS su Fabric e NeoForge; test client PASS su entrambi i loader (il primo avvio NeoForge si è chiuso con il solito crash nativo 0xC0000005 prima del caricamento della mod, il secondo è passato). Screenshot rivisti a 427x240 e 1920x1080.
- Problemi trovati e risolti: HUD e chat visibili sotto il menu (sfondo ora opaco), barre di pratica sovrapposte ai costi, schede delle forme troppo basse, titoli troppo larghi su finestre piccole, polvere e fiamme troppo grandi e ruotate, aspetto dell'anteprima del creator che restava sul giocatore.
- Maestro Muten rifatto sul riferimento canonico fornito dall'utente (altezza 165 cm dal wiki) con modelli dedicati (guscio a gradoni, occhiali, barba, ciuffi) e outfit dipinto (camicia hawaiana, pantaloncini, infradito).
- Forme equipaggiabili/rimovibili (azione FORM_EQUIP, campo unequipped_forms in PlayerData): V e B saltano le forme non equipaggiate. Test: smoke server PASS su Fabric e NeoForge con i controlli della logica; test client PASS (base -> SSJ2 con SSJ rimossa, ritorno a base, rimozione della forma attuale).
