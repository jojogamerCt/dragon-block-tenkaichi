import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "dbtenkaichi", "lang")

STATS = {
    "strength": ("Strength", "Forza"),
    "defense": ("Defense", "Difesa"),
    "constitution": ("Constitution", "Costituzione"),
    "ki_control": ("Ki Control", "Controllo Ki"),
    "ki_capacity": ("Ki Capacity", "Capacità Ki"),
    "ki_output": ("Ki Output", "Potenza Ki"),
    "speed": ("Speed", "Velocità"),
    "agility": ("Agility", "Agilità"),
    "stamina": ("Stamina", "Resistenza"),
    "martial_arts": ("Martial Arts", "Arti Marziali"),
    "spirit": ("Spirit", "Spirito"),
    "potential": ("Potential", "Potenziale"),
}

RACES = {
    "saiyan": ("Saiyan", "Saiyan", "Warrior race: Zenkai after near-death fights, hungry for battle, has a tail.", "Razza guerriera: Zenkai dopo scontri al limite, fame di battaglia, possiede la coda."),
    "half_saiyan": ("Half-Saiyan", "Mezzo-Saiyan", "Hidden potential and reduced Zenkai.", "Potenziale nascosto e Zenkai ridotto."),
    "human": ("Human", "Umano", "Masters of technique: efficient Ki and fast training.", "Maestri della tecnica: Ki efficiente e allenamento rapido."),
    "namekian": ("Namekian", "Namecciano", "Regenerates with Ki and stretches limbs.", "Si rigenera con il Ki e allunga gli arti."),
    "frieza_race": ("Frieza Race", "Razza di Freezer", "Fast, resilient, survives in space.", "Veloce, resistente, sopravvive nello spazio."),
    "majin": ("Majin", "Majin", "Elastic body, absorption and regeneration.", "Corpo elastico, assorbimento e rigenerazione."),
    "android": ("Android", "Androide", "Infinite energy reserve, impossible to sense.", "Riserva di energia infinita, impossibile da percepire."),
    "bio_android": ("Bio-Android", "Bio-Androide", "Absorbs and evolves.", "Assorbe ed evolve."),
    "kaioshin": ("Kaioshin", "Kaioshin", "Divine Ki and great spirit.", "Ki divino e grande spirito."),
    "demon": ("Demon Realm Race", "Razza del Regno Demoniaco", "Destructive Ki and demonic rage.", "Ki distruttivo e furia demoniaca."),
}

PASSIVES = {
    "zenkai": ("Zenkai", "Zenkai"),
    "battle_hunger": ("Battle hunger", "Fame di battaglia"),
    "tail": ("Tail", "Coda"),
    "technique_mastery": ("Technique mastery", "Maestria tecnica"),
    "ki_efficiency": ("Ki efficiency", "Efficienza Ki"),
    "regeneration": ("Regeneration", "Rigenerazione"),
    "stretch_limbs": ("Stretch limbs", "Arti estensibili"),
    "energy_reserve": ("Energy reserve", "Riserva energetica"),
    "unsensable": ("Unsensable", "Non percepibile"),
    "elastic_body": ("Elastic body", "Corpo elastico"),
    "absorption": ("Absorption", "Assorbimento"),
    "divine_ki": ("Divine Ki", "Ki divino"),
    "demonic_rage": ("Demonic rage", "Furia demoniaca"),
    "space_survival": ("Space survival", "Sopravvivenza spaziale"),
    "hidden_potential": ("Hidden potential", "Potenziale nascosto"),
}

HAIR = {
    "goku": ("Goku", "Goku"),
    "vegeta": ("Vegeta", "Vegeta"),
    "gohan": ("Gohan (Cell Saga)", "Gohan (saga di Cell)"),
    "trunks": ("Future Trunks", "Trunks del futuro"),
    "yamcha": ("Yamcha", "Yamcha"),
    "raditz": ("Raditz", "Radish"),
    "bald": ("Bald (Krillin, Tien)", "Calvo (Crilin, Tensing)"),
    "android_18": ("Android 18", "C-18"),
    "videl": ("Videl", "Videl"),
    "chi_chi": ("Chi-Chi", "Chi-Chi"),
    "super_spikes": ("Super Saiyan", "Super Saiyan"),
    "ascended_spikes": ("Super Saiyan 2", "Super Saiyan 2"),
    "legendary_mane": ("Super Saiyan 3", "Super Saiyan 3"),
    "ssj_vegeta": ("Super Saiyan (Vegeta)", "Super Saiyan (Vegeta)"),
    "ssj2_spikes": ("Super Saiyan 2 spikes", "Punte Super Saiyan 2"),
}

KEYS = {
    "charge": ("Charge Ki (hold)", "Carica Ki (tieni premuto)"),
    "flight": ("Toggle flight", "Attiva/disattiva volo"),
    "combat_stance": ("Combat stance", "Posizione di combattimento"),
    "lock_on": ("Lock on target", "Aggancia bersaglio"),
    "next_target": ("Next target", "Bersaglio successivo"),
    "dodge": ("Dodge / Vanish", "Schivata / Vanish"),
    "dash": ("Dash", "Scatto"),
    "transform": ("Transform", "Trasformazione"),
    "revert": ("Power down", "Annulla trasformazione"),
    "skill_1": ("Technique slot 1", "Tecnica slot 1"),
    "skill_2": ("Technique slot 2", "Tecnica slot 2"),
    "skill_3": ("Technique slot 3", "Tecnica slot 3"),
    "skill_4": ("Technique slot 4", "Tecnica slot 4"),
    "kaioken": ("Kaioken (cycle level)", "Kaioken (cambia livello)"),
    "character_menu": ("Character menu", "Menu personaggio"),
}

SOUNDS = {
    "combat.punch_light": ("Light punch lands", "Pugno leggero"),
    "combat.punch_heavy": ("Heavy blow lands", "Colpo pesante"),
    "combat.kick": ("Kick lands", "Calcio"),
    "combat.swing": ("Attack whooshes", "Attacco a vuoto"),
    "combat.guard": ("Blow blocked", "Colpo parato"),
    "combat.perfect_guard": ("Perfect guard", "Parata perfetta"),
    "combat.guard_break": ("Guard broken", "Guardia spezzata"),
    "combat.clash": ("Fists clash", "Scontro di pugni"),
    "combat.dodge": ("Dodge", "Schivata"),
    "combat.dash": ("Dash", "Scatto"),
    "combat.vanish": ("Afterimage vanish", "Scomparsa istantanea"),
    "ki.charge_start": ("Ki flares up", "Il Ki divampa"),
    "ki.charge_loop": ("Ki charging", "Carica del Ki"),
    "ki.blast": ("Ki blast fired", "Sfera Ki lanciata"),
    "ki.impact": ("Ki impact", "Impatto Ki"),
    "ki.beam_charge": ("Beam charging", "Raggio in carica"),
    "ki.beam_fire": ("Beam fired", "Raggio sparato"),
    "ki.beam_loop": ("Beam roars", "Ruggito del raggio"),
    "ki.explosion": ("Ki explosion", "Esplosione Ki"),
    "ki.clash_loop": ("Beams clash", "Scontro di raggi"),
    "flight.takeoff": ("Takes off", "Decollo"),
    "flight.loop": ("Flying", "Volo"),
    "flight.sonic": ("Sonic boom", "Bang sonico"),
    "flight.land": ("Heavy landing", "Atterraggio pesante"),
    "aura.burst": ("Aura bursts", "Esplosione d'aura"),
    "aura.loop": ("Aura hums", "Ronzio dell'aura"),
    "transform.power_up": ("Power rises", "La potenza cresce"),
    "transform.scream": ("Transformation roar", "Urlo di trasformazione"),
    "transform.revert": ("Power fades", "La potenza svanisce"),
    "transform.thunder": ("Thunder strikes", "Un fulmine si abbatte"),
    "transform.rumble": ("The ground rumbles", "Il terreno trema"),
    "ui.click": ("Click", "Clic"),
    "ui.confirm": ("Confirm", "Conferma"),
    "ui.open": ("Scouter boots up", "Lo scouter si accende"),
    "ui.tab": ("Scouter blip", "Bip dello scouter"),
    "ui.upgrade": ("Power increased", "Potenza aumentata"),
    "ui.deny": ("Not enough power", "Potenza insufficiente"),
    "training.beat": ("Training beat", "Battito dell'allenamento"),
    "training.success": ("Training success", "Allenamento riuscito"),
    "voice.grunt": ("Fighter grunts", "Grido del combattente"),
}


def build():
    en = {}
    it = {}

    def add(key, e, i):
        en[key] = e
        it[key] = i

    add("itemGroup.dbtenkaichi.main", "Dragon Block: Tenkaichi", "Dragon Block: Tenkaichi")
    add("item.dbtenkaichi.senzu_bean", "Senzu Bean", "Fagiolo Senzu")
    add("item.dbtenkaichi.gi_top", "Martial Arts Gi Top", "Casacca del gi")
    add("item.dbtenkaichi.gi_pants", "Martial Arts Gi Pants", "Pantaloni del gi")
    add("item.dbtenkaichi.gi_boots", "Martial Arts Boots", "Stivali del gi")
    add("item.dbtenkaichi.battle_armor_top", "Battle Armor Chestplate", "Corazza da battaglia")
    add("item.dbtenkaichi.battle_armor_pants", "Battle Armor Suit", "Tuta da battaglia")
    add("item.dbtenkaichi.battle_armor_boots", "Battle Armor Boots", "Stivali da battaglia")
    add("item.dbtenkaichi.casual_top", "Casual Jacket", "Giacca casual")
    add("item.dbtenkaichi.casual_pants", "Casual Pants", "Pantaloni casual")
    add("item.dbtenkaichi.casual_boots", "Casual Shoes", "Scarpe casual")
    add("item.dbtenkaichi.master_spawn_egg", "Martial Arts Master Spawn Egg", "Uovo generatore Maestro di arti marziali")
    add("item.dbtenkaichi.saiyan_raider_spawn_egg", "Saiyan Raider Spawn Egg", "Uovo generatore Predone Saiyan")
    add("entity.dbtenkaichi.master", "Martial Arts Master", "Maestro di arti marziali")
    add("entity.dbtenkaichi.saiyan_raider", "Saiyan Raider", "Predone Saiyan")
    add("entity.dbtenkaichi.ki_beam", "Ki Beam", "Raggio Ki")
    add("entity.dbtenkaichi.ki_blast", "Ki Blast", "Sfera Ki")
    add("key.category.dbtenkaichi.tenkaichi", "Dragon Block: Tenkaichi", "Dragon Block: Tenkaichi")
    for k, (e, i) in KEYS.items():
        add("key.dbtenkaichi." + k, e, i)
    for k, (e, i) in STATS.items():
        add("stat.dbtenkaichi." + k, e, i)
        add("requirement.dbtenkaichi.stat." + k, "%s requirement" % e, "requisito di %s" % i.lower())
    for k, (e, i, de, di) in RACES.items():
        add("race.dbtenkaichi." + k, e, i)
        add("race.dbtenkaichi." + k + ".desc", de, di)
    for k, (e, i) in PASSIVES.items():
        add("passive.dbtenkaichi." + k, e, i)
    for k, (e, i) in HAIR.items():
        add("hair.dbtenkaichi." + k, e, i)
    for k, (e, i) in SOUNDS.items():
        add("subtitles.dbtenkaichi." + k, e, i)
    add("form.dbtenkaichi.super_saiyan", "Super Saiyan", "Super Saiyan")
    add("form.dbtenkaichi.raider_rage", "Raider's Rage", "Furia del Predone")
    add("technique.dbtenkaichi.ki_blast", "Ki Blast", "Sfera Ki")
    add("technique.dbtenkaichi.kamehameha", "Kamehameha", "Kamehameha")
    add("technique.dbtenkaichi.raider_cannon", "Raider Cannon", "Cannone del Predone")
    add("technique.dbtenkaichi.galick_gun", "Galick Gun", "Galick Gun")
    add("technique.dbtenkaichi.masenko", "Masenko", "Masenko")
    add("technique.dbtenkaichi.special_beam_cannon", "Special Beam Cannon", "Makankosappo")
    add("technique.dbtenkaichi.final_flash", "Final Flash", "Final Flash")
    add("technique.dbtenkaichi.big_bang_attack", "Big Bang Attack", "Big Bang Attack")
    add("technique.dbtenkaichi.death_beam", "Death Beam", "Raggio della Morte")
    add("form.dbtenkaichi.super_saiyan_2", "Super Saiyan 2", "Super Saiyan 2")
    add("form.dbtenkaichi.super_saiyan_3", "Super Saiyan 3", "Super Saiyan 3")
    add("technique.dbtenkaichi.kaioken", "Kaioken", "Kaioken")
    add("message.dbtenkaichi.kaioken_unknown", "You have not learned the Kaioken.", "Non hai imparato il Kaioken.")
    add("message.dbtenkaichi.kaioken_incompatible", "The Kaioken cannot be used in this form.", "Il Kaioken non si può usare in questa forma.")
    add("message.dbtenkaichi.kaioken_on", "Kaioken x%s!", "Kaioken x%s!")
    add("message.dbtenkaichi.kaioken_off", "Kaioken released", "Kaioken disattivato")
    add("message.dbtenkaichi.kaioken_exhausted", "Your Ki ran out and the Kaioken faded.", "Il Ki si è esaurito e il Kaioken è svanito.")
    add("message.dbtenkaichi.kaioken_strain", "Your body cannot take the strain: the Kaioken breaks off!", "Il tuo corpo non regge lo sforzo: il Kaioken si interrompe!")
    add("hud.dbtenkaichi.kaioken", "Kaioken x%s", "Kaioken x%s")
    for dmg, (e, i) in {
        "martial_arts": ("%1$s was beaten down", "%1$s è stato messo al tappeto"),
        "ki_blast": ("%1$s was struck by a Ki blast", "%1$s è stato colpito da una sfera Ki"),
        "ki_beam": ("%1$s was erased by an energy beam", "%1$s è stato cancellato da un raggio di energia"),
        "ki_explosion": ("%1$s was caught in a Ki explosion", "%1$s è stato travolto da un'esplosione di Ki"),
    }.items():
        add("death.attack.dbtenkaichi." + dmg, e, i)
        add("death.attack.dbtenkaichi." + dmg + ".player", e.replace("%1$s was", "%1$s was") + " by %2$s", i + " da %2$s")
    add("requirement.dbtenkaichi.battle_power", "base Battle Power", "Battle Power di base")
    add("requirement.dbtenkaichi.race", "the right race", "la razza adatta")
    add("requirement.dbtenkaichi.mastery", "mastery of the previous form", "padronanza della forma precedente")
    add("message.dbtenkaichi.form_hint.rage", "You sense %s sleeping inside you... it will awaken only in a desperate fight against a stronger foe.",
        "Senti %s dormire dentro di te... si risveglierà solo in uno scontro disperato contro un nemico più forte.")
    add("message.dbtenkaichi.form_hint.training", "%s must be unlocked through training.", "%s si sblocca con l'allenamento.")
    add("message.dbtenkaichi.form_locked", "%s is out of reach: you lack %s.", "%s è fuori portata: ti manca %s.")
    add("message.dbtenkaichi.no_next_form", "There is no further transformation available.", "Non ci sono altre trasformazioni disponibili.")
    add("message.dbtenkaichi.no_ki", "Not enough Ki!", "Ki insufficiente!")
    add("message.dbtenkaichi.form_exhausted", "Your Ki ran out and the transformation collapsed.", "Il Ki si è esaurito e la trasformazione è crollata.")
    add("message.dbtenkaichi.flight_exhausted", "You are too drained to keep flying.", "Sei troppo esausto per continuare a volare.")
    add("message.dbtenkaichi.rage_unlock", "Rage explodes inside you: %s awakened!", "La rabbia esplode dentro di te: %s si è risvegliato!")
    add("message.dbtenkaichi.zenkai", "Zenkai! Surviving that fight made you stronger (+%s stat levels).", "Zenkai! Sopravvivere a quello scontro ti ha reso più forte (+%s livelli).")
    add("message.dbtenkaichi.character_created", "Your journey as a %s begins.", "Il tuo viaggio come %s ha inizio.")
    add("message.dbtenkaichi.race_unavailable", "That race is not available on this server.", "Questa razza non è disponibile su questo server.")
    add("message.dbtenkaichi.empty_slot", "No technique in this slot. Assign one from the character menu.", "Nessuna tecnica in questo slot. Assegnala dal menu personaggio.")
    add("message.dbtenkaichi.stat_capped", "%s is capped at %s: raise your Potential.", "%s è al limite di %s: aumenta il Potenziale.")
    add("message.dbtenkaichi.practice_level", "Training paid off: %s is now %s.", "L'allenamento ha dato frutti: %s ora è %s.")
    add("message.dbtenkaichi.too_tired", "You are too tired to train. Rest first.", "Sei troppo stanco per allenarti. Riposa prima.")
    add("message.dbtenkaichi.sparring_start", "Show me what you've got!", "Fammi vedere di cosa sei capace!")
    add("message.dbtenkaichi.drill_start", "Dodge my Ki blasts!", "Schiva le mie sfere di Ki!")
    add("message.dbtenkaichi.learned", "You learned %s!", "Hai imparato %s!")
    add("message.dbtenkaichi.not_ready_kamehameha", "You are not ready: train Ki Control (6) and Ki Output (5) and complete two sessions.",
        "Non sei pronto: allena Controllo Ki (6) e Potenza Ki (5) e completa due sessioni.")
    add("message.dbtenkaichi.tp_gained", "+%s TP", "+%s TP")
    add("command.dbtenkaichi.done", "Done.", "Fatto.")
    add('entity.dbtenkaichi.master.roshi', 'Master Roshi', 'Maestro Muten')
    add('entity.dbtenkaichi.master.karin', 'Korin', 'Karin')
    add('entity.dbtenkaichi.master.kami', 'Kami', 'Il Supremo')
    add('landmark.dbtenkaichi.kame_house', 'Kame House', 'Kame House')
    add('landmark.dbtenkaichi.korin_tower', 'Korin Tower', 'Torre di Karin')
    add('landmark.dbtenkaichi.kami_lookout', "Kami's Lookout", 'Palazzo del Supremo')
    add('command.dbtenkaichi.landmark', '%s: %s, %s, %s', '%s: %s, %s, %s')
    add('command.dbtenkaichi.no_landmarks', 'No landmarks have been generated yet.', 'Nessun luogo iconico generato.')
    add('command.dbtenkaichi.unknown_lesson', 'Unknown trial.', 'Prova sconosciuta.')
    add('lesson.dbtenkaichi.kamehameha', 'Kamehameha', 'Kamehameha')
    add('lesson.dbtenkaichi.senzu', 'Senzu Bean', 'Fagiolo Senzu')
    add('lesson.dbtenkaichi.sacred_water', 'Sacred Water', 'Acqua Sacra')
    add('lesson.dbtenkaichi.time_chamber', 'Hyperbolic Time Chamber', 'Stanza dello Spirito e del Tempo')
    add('arena.dbtenkaichi.mountain', 'Rocky Wasteland', 'Lande rocciose')
    add('arena.dbtenkaichi.kame_island', 'Kame Island', 'Isola della Kame House')
    add('arena.dbtenkaichi.korin_forest', 'Sacred Land of Korin', 'Terra sacra di Karin')
    add('arena.dbtenkaichi.namek', 'Planet Namek', 'Pianeta Namecc')
    add('arena.dbtenkaichi.cell_ring', 'Cell Games Ring', 'Ring del Cell Game')
    add('arena.dbtenkaichi.canyon', 'Rocky Canyon', 'Canyon roccioso')
    add('message.dbtenkaichi.trial_vs_self', 'Face yourself as %s', 'Affronta te stesso in %s')
    add('screen.dbtenkaichi.character.trial', 'Trial', 'Prova')
    add('screen.dbtenkaichi.character.req_bp', 'Base BP %s/%s', 'BP base %s/%s')
    add('screen.dbtenkaichi.character.req_parent', 'After %s', 'Dopo %s')
    add('screen.dbtenkaichi.character.req_trial', 'Defeat yourself', 'Sconfiggi te stesso')
    add('message.dbtenkaichi.form_trial_parent', 'Unlock %s first.', 'Prima sblocca %s.')
    add('message.dbtenkaichi.form_trial_ready', 'Your Potential is ready to break its limits. Press transform again to face yourself as %s.', 'Il tuo Potenziale è pronto a superare i limiti. Premi di nuovo trasformazione per affrontare te stesso in %s.')
    add('message.dbtenkaichi.form_trial_cleared', 'You defeated yourself: %s unlocked!', 'Hai sconfitto te stesso: %s sbloccato!')
    add('entity.dbtenkaichi.mirror', '%s (%s)', '%s (%s)')
    add('arena.dbtenkaichi.time_chamber', 'Hyperbolic Time Chamber', 'Stanza dello Spirito e del Tempo')
    add('message.dbtenkaichi.trial_vs', 'Trial against %s', 'Prova contro %s')
    add('message.dbtenkaichi.trial_fight', 'FIGHT!', 'COMBATTI!')
    add('message.dbtenkaichi.trial_cleared', 'Trial cleared!', 'Prova superata!')
    add('message.dbtenkaichi.trial_failed', 'Trial failed', 'Prova fallita')
    add('message.dbtenkaichi.trial_time', 'Time left: %s:%s', 'Tempo rimasto: %s:%s')
    add('message.dbtenkaichi.trial_busy', 'You cannot start a trial right now.', 'Non puoi iniziare una prova adesso.')
    add('message.dbtenkaichi.trial_unavailable', 'The training realm is not available on this server.', 'La dimensione di allenamento non è disponibile su questo server.')
    add('message.dbtenkaichi.lesson_locked', 'You need a base Battle Power of %s.', 'Ti serve un Battle Power base di %s.')
    add('message.dbtenkaichi.not_enough_tp', 'You need %s Training Points.', 'Ti servono %s Punti Allenamento.')
    add('message.dbtenkaichi.sacred_water', 'You drank the Sacred Water: Speed, Agility and Martial Arts +2.', "Hai bevuto l'Acqua Sacra: Velocità, Agilità e Arti marziali +2.")
    add('message.dbtenkaichi.time_chamber', 'A year of training in a single day: +250 TP.', 'Un anno di allenamento in un solo giorno: +250 TP.')
    add('training.dbtenkaichi.buy', 'Buy · %s TP', 'Compra · %s TP')
    add('training.dbtenkaichi.trial', 'Trial', 'Prova')
    add('training.dbtenkaichi.learned_short', 'Learned', 'Appreso')
    add('screen.dbtenkaichi.training.lessons', 'Lessons', 'Lezioni')
    add('screen.dbtenkaichi.training.requires_bp', 'Requires base BP %s', 'Richiede BP base %s')
    add('screen.dbtenkaichi.training.cooldown', 'Come back tomorrow', 'Torna domani')
    add('screen.dbtenkaichi.training.available', 'Available', 'Disponibile')
    add('screen.dbtenkaichi.character.buy_technique', '%s · %s TP · BP %s', '%s · %s TP · BP %s')
    add("command.dbtenkaichi.reloaded", "Dragon Block: Tenkaichi data reloaded.", "Dati di Dragon Block: Tenkaichi ricaricati.")
    add("command.dbtenkaichi.no_character", "That player has no character yet.", "Quel giocatore non ha ancora un personaggio.")
    add("command.dbtenkaichi.unknown_form", "Unknown transformation: %s", "Trasformazione sconosciuta: %s")
    add("command.dbtenkaichi.unknown_stat", "Unknown statistic.", "Statistica sconosciuta.")
    add("command.dbtenkaichi.unknown_technique", "Unknown technique: %s", "Tecnica sconosciuta: %s")
    add("command.dbtenkaichi.stats_header", "%s - %s - BP %s - TP %s", "%s - %s - BP %s - TP %s")
    add("hud.dbtenkaichi.ki", "Ki %s/%s", "Ki %s/%s")
    add("hud.dbtenkaichi.tp", "TP %s", "TP %s")
    add("hud.dbtenkaichi.mastery", "Mastery", "Padronanza")
    add("hud.dbtenkaichi.fatigue", "Fatigue %s%%", "Fatica %s%%")
    add("hud.dbtenkaichi.flight", "FLIGHT", "VOLO")
    add("hud.dbtenkaichi.flight_fast", "SUPERSONIC", "SUPERSONICO")
    add("hud.dbtenkaichi.stance", "COMBAT STANCE", "IN GUARDIA")
    add("hud.dbtenkaichi.exhausted", "EXHAUSTED", "ESAUSTO")
    add("hud.dbtenkaichi.clash_hint", "Hold your technique key and press %s on the green beat!", "Tieni premuto il tasto della tecnica e premi %s sul battito verde!")
    add("screen.dbtenkaichi.creator", "Create your fighter", "Crea il tuo combattente")
    add("screen.dbtenkaichi.creator.tab.body", "Body", "Corpo")
    add("screen.dbtenkaichi.creator.tab.face", "Face & hair", "Viso e capelli")
    add("screen.dbtenkaichi.creator.tab.ki", "Ki & outfit", "Ki e abiti")
    rows = {
        "race": ("Race", "Razza"), "presentation": ("Look", "Aspetto"), "build": ("Build", "Corporatura"), "skin": ("Skin", "Pelle"),
        "voice": ("Voice", "Voce"), "own_skin": ("Use own skin", "Usa la tua skin"), "eyes": ("Eyes", "Occhi"), "eye_color": ("Eye color", "Colore occhi"),
        "brows": ("Eyebrows", "Sopracciglia"), "expression": ("Expression", "Espressione"), "hair": ("Hair", "Capelli"), "hair_color": ("Hair color", "Colore capelli"),
        "tail": ("Tail", "Coda"), "aura": ("Aura", "Aura"), "outfit": ("Outfit", "Abito"), "outfit_primary": ("Main color", "Colore principale"),
        "outfit_secondary": ("Accent color", "Colore accento"),
        "feature_color": ("Feature color", "Colore dettagli"),
    }
    for k, (e, i) in rows.items():
        add("screen.dbtenkaichi.creator." + k, e, i)
    add("screen.dbtenkaichi.creator.presentation.0", "Masculine", "Maschile")
    add("screen.dbtenkaichi.creator.presentation.1", "Feminine", "Femminile")
    for n, (e, i) in enumerate([("Slim", "Snella"), ("Athletic", "Atletica"), ("Muscular", "Muscolosa")]):
        add("screen.dbtenkaichi.creator.build.%d" % n, e, i)
    for n, (e, i) in enumerate([("Calm", "Calma"), ("Focused", "Concentrata"), ("Fierce", "Feroce"), ("Cheerful", "Allegra")]):
        add("screen.dbtenkaichi.creator.expression.%d" % n, e, i)
    for n, (e, i) in enumerate([("Martial arts gi", "Gi da arti marziali"), ("Battle armor", "Armatura da battaglia"), ("Casual", "Casual"), ("Natural", "Naturale")]):
        add("screen.dbtenkaichi.creator.outfit.%d" % n, e, i)
    add("screen.dbtenkaichi.creator.style", "Style %s", "Stile %s")
    add("screen.dbtenkaichi.creator.voice_value", "Voice %s", "Voce %s")
    add("screen.dbtenkaichi.creator.height", "Height: %s cm", "Altezza: %s cm")
    add("screen.dbtenkaichi.creator.random", "Randomize", "Casuale")
    add("screen.dbtenkaichi.creator.confirm", "Begin the journey", "Inizia il viaggio")
    add("screen.dbtenkaichi.character", "Character", "Personaggio")
    for k, (e, i) in {"stats": ("Stats", "Statistiche"), "skills": ("Techniques", "Tecniche"), "forms": ("Forms", "Forme"), "settings": ("Settings", "Impostazioni")}.items():
        add("screen.dbtenkaichi.character.tab." + k, e, i)
    add("screen.dbtenkaichi.character.cost", "Cost %s TP", "Costo %s TP")
    add("screen.dbtenkaichi.character.cap", "Current stat cap: %s (raise Potential to increase it)", "Limite attuale: %s (aumenta il Potenziale per alzarlo)")
    add("screen.dbtenkaichi.character.passives", "Racial traits: %s", "Tratti razziali: %s")
    add("screen.dbtenkaichi.character.slot", "Slot %s", "Slot %s")
    add("screen.dbtenkaichi.character.skills_hint", "Select a technique, then click a slot to assign it.", "Seleziona una tecnica, poi clicca uno slot per assegnarla.")
    add("screen.dbtenkaichi.character.no_forms", "No transformations for your race yet.", "Ancora nessuna trasformazione per la tua razza.")
    add("screen.dbtenkaichi.character.unlocked", "Unlocked", "Sbloccata")
    add("screen.dbtenkaichi.character.requires", "Requires base BP %s and a special trigger", "Richiede BP base %s e un evento scatenante")
    add("screen.dbtenkaichi.settings.quality", "Effects quality: %s", "Qualità effetti: %s")
    add("screen.dbtenkaichi.settings.cinematics", "Transformation cinematics: %s", "Cinematiche trasformazione: %s")
    add("screen.dbtenkaichi.settings.shake", "Camera shake: %s%%", "Scossa camera: %s%%")
    add("screen.dbtenkaichi.settings.hitstop", "Hit stop: %s", "Hit stop: %s")
    add("screen.dbtenkaichi.settings.bp", "Show Battle Power: %s", "Mostra Battle Power: %s")
    for k, (e, i) in {"low": ("Low", "Bassa"), "medium": ("Medium", "Media"), "high": ("High", "Alta"), "ultra": ("Ultra", "Ultra")}.items():
        add("quality.dbtenkaichi." + k, e, i)
    for k, (e, i) in {"full": ("Full", "Complete"), "short": ("Short", "Brevi"), "off": ("Off", "Disattivate")}.items():
        add("cinematics.dbtenkaichi." + k, e, i)
    add("screen.dbtenkaichi.training", "Training", "Allenamento")
    add("screen.dbtenkaichi.training.master", "\"Hm. Let's see what you are made of.\"", "\"Hm. Vediamo di che pasta sei fatto.\"")
    add("screen.dbtenkaichi.training.status", "Sessions completed: %s - Fatigue: %s%%", "Sessioni completate: %s - Fatica: %s%%")
    add("screen.dbtenkaichi.training.score", "Score: %s", "Punteggio: %s")
    add("screen.dbtenkaichi.training.reward", "Training Points earned: %s", "Punti allenamento ottenuti: %s")
    add("screen.dbtenkaichi.training.aborted", "Session interrupted.", "Sessione interrotta.")
    add("screen.dbtenkaichi.training.meditation_hint", "Press SPACE when the ring meets the golden circle.", "Premi SPAZIO quando l'anello tocca il cerchio dorato.")
    add("screen.dbtenkaichi.training.presses", "Breaths: %s", "Respiri: %s")
    add("screen.dbtenkaichi.training.ki_hint", "Hold %s to charge, release inside the green band.", "Tieni premuto %s per caricare, rilascia nella fascia verde.")
    for k, (e, i) in {"meditation": ("Meditation", "Meditazione"), "ki_control": ("Ki Control", "Controllo del Ki"), "sparring": ("Sparring", "Combattimento d'allenamento"),
                      "dodge_drill": ("Dodge drill", "Esercizio di schivata")}.items():
        add("training.dbtenkaichi." + k, e, i)
    add("training.dbtenkaichi.learn", "Learn %s", "Impara %s")
    add("training.dbtenkaichi.learned", "%s already learned", "%s già appresa")
    add("training.dbtenkaichi.stop", "Stop", "Interrompi")
    add("command.dbtenkaichi.landmarks_rebuilt", "Rebuilt %s landmarks and refreshed their safe zones.", "Ricostruiti %s luoghi iconici e aggiornate le loro zone sicure.")
    for k, (e, i) in {"attributes": ("Attributes", "Attributi"), "traits": ("Racial traits", "Tratti razziali"), "equipped": ("Equipped techniques", "Tecniche equipaggiate"),
                      "known": ("Known techniques", "Tecniche conosciute"), "learn": ("Learn new techniques", "Impara nuove tecniche"), "forms": ("Transformations", "Trasformazioni"),
                      "settings": ("Game settings", "Impostazioni di gioco"), "profile": ("Profile", "Profilo")}.items():
        add("screen.dbtenkaichi.character.section." + k, e, i)
    add("screen.dbtenkaichi.character.level", "Lv. %s", "Liv. %s")
    add("screen.dbtenkaichi.character.tier", "Tier %s", "Livello %s")
    add("screen.dbtenkaichi.character.multiplier", "x%s power", "potenza x%s")
    add("screen.dbtenkaichi.character.mastery", "Mastery %s%%", "Padronanza %s%%")
    add("screen.dbtenkaichi.character.selected", "Selected: %s · Ki %s", "Selezionata: %s · Ki %s")
    add("screen.dbtenkaichi.character.none_selected", "Pick a technique below, then click a slot.", "Scegli una tecnica qui sotto, poi clicca uno slot.")
    add("screen.dbtenkaichi.character.all_learned", "You know every technique available to you.", "Conosci tutte le tecniche disponibili.")
    add("screen.dbtenkaichi.character.drag_hint", "Drag to rotate · Scroll to zoom", "Trascina per ruotare · Rotella per lo zoom")
    add("screen.dbtenkaichi.character.close", "Close", "Chiudi")
    add("screen.dbtenkaichi.character.height", "Height %s cm", "Altezza %s cm")
    return en, it


def main():
    en, it = build()
    os.makedirs(OUT, exist_ok=True)
    for name, data in (("en_us", en), ("it_it", it)):
        with open(os.path.join(OUT, name + ".json"), "w", encoding="utf-8") as f:
            json.dump(dict(sorted(data.items())), f, ensure_ascii=False, indent=2)
            f.write("\n")
    print("wrote %d keys" % len(en))
    if len(sys.argv) > 1:
        missing = []
        with open(sys.argv[1], encoding="utf-8") as f:
            for line in f:
                key = line.strip()
                if key and not key.endswith(".") and key not in en:
                    missing.append(key)
        print("missing:", missing)


if __name__ == "__main__":
    main()
