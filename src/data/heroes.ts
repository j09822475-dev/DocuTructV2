/**
 * Static Dota 2 hero roster + cosmetic item slots.
 * The slot list matches the common Workshop item slots; not every hero has
 * every slot, so the per-hero `slots` narrows the valid choices where known.
 */

export const ITEM_SLOTS = [
  "weapon",
  "off_hand",
  "head",
  "shoulder",
  "arms",
  "back",
  "belt",
  "legs",
  "tail",
  "armor",
  "mount",
  "misc",
] as const;

export type ItemSlot = (typeof ITEM_SLOTS)[number];

export interface Hero {
  name: string;
  attribute: "strength" | "agility" | "intelligence" | "universal";
  /** Visual identity notes fed into the concept prompt. */
  identity: string;
  slots: ItemSlot[];
}

export const HEROES: Hero[] = [
  { name: "Pudge", attribute: "strength", identity: "rotund undead butcher; sickly green flesh, stitched scars, rusty hook and cleaver", slots: ["weapon", "off_hand", "head", "shoulder", "arms", "back", "belt"] },
  { name: "Juggernaut", attribute: "agility", identity: "masked swordsman; white mask, orange/red robes, curved blade, honor-bound warrior", slots: ["weapon", "head", "shoulder", "arms", "back", "belt", "legs"] },
  { name: "Crystal Maiden", attribute: "intelligence", identity: "ice sorceress; blue and white palette, fur-lined cape, frost crystals, staff", slots: ["weapon", "head", "shoulder", "arms", "back", "belt"] },
  { name: "Invoker", attribute: "universal", identity: "arrogant arcane prodigy; violet and gold, floating spheres of quas/wex/exort, ornate cape", slots: ["weapon", "head", "shoulder", "arms", "back", "belt"] },
  { name: "Phantom Assassin", attribute: "agility", identity: "veiled assassin; dark blue shrouds, twin blades, ghostly mist", slots: ["weapon", "off_hand", "head", "shoulder", "arms", "back", "belt"] },
  { name: "Axe", attribute: "strength", identity: "red-skinned berserker; massive axe, spiked pauldrons, mohawk", slots: ["weapon", "head", "shoulder", "arms", "back", "belt"] },
  { name: "Lina", attribute: "intelligence", identity: "fire sorceress; flame-red hair, orange and crimson garb, ember particles", slots: ["weapon", "head", "shoulder", "arms", "back", "belt", "legs"] },
  { name: "Shadow Fiend", attribute: "agility", identity: "demonic soul collector; charcoal body, glowing ember chest, shadowraze flames", slots: ["head", "shoulder", "arms", "back", "armor"] },
  { name: "Drow Ranger", attribute: "agility", identity: "pale frost archer; icy blue cloak, longbow, silver hair", slots: ["weapon", "off_hand", "head", "shoulder", "back", "belt", "legs"] },
  { name: "Earthshaker", attribute: "strength", identity: "stone shaman; granite skin, totem weapon, tribal adornments", slots: ["weapon", "head", "shoulder", "arms", "back", "belt", "tail"] },
  { name: "Storm Spirit", attribute: "intelligence", identity: "jovial lightning elemental; blue electric energy, celestial robes", slots: ["weapon", "head", "shoulder", "arms", "back", "belt"] },
  { name: "Sven", attribute: "strength", identity: "rogue knight; heavy plate armor, giant two-handed sword, blue glow", slots: ["weapon", "head", "shoulder", "arms", "back", "belt"] },
  { name: "Windranger", attribute: "universal", identity: "agile forest archer; red hair, green hooded garb, powerful bow", slots: ["weapon", "head", "shoulder", "arms", "back", "belt", "legs"] },
  { name: "Faceless Void", attribute: "agility", identity: "time-bending voidwalker; purple carapace, mace of ages, chrono energy", slots: ["weapon", "head", "shoulder", "arms", "back", "belt"] },
  { name: "Anti-Mage", attribute: "agility", identity: "monk blade-dancer; twin glaives, purple and teal, mana-burning runes", slots: ["weapon", "head", "shoulder", "arms", "back", "belt", "legs"] },
  { name: "Queen of Pain", attribute: "intelligence", identity: "succubus; black leather, bat wings, sonic scream, crimson accents", slots: ["weapon", "head", "shoulder", "arms", "back", "belt", "legs"] },
  { name: "Tidehunter", attribute: "strength", identity: "hulking sea beast; green scales, anchor weapon, barnacles and coral", slots: ["weapon", "head", "shoulder", "arms", "back", "belt"] },
  { name: "Rubick", attribute: "intelligence", identity: "grand magus; green and gold, pointed collar, spell-stealing staff", slots: ["weapon", "head", "shoulder", "arms", "back", "belt"] },
  { name: "Legion Commander", attribute: "strength", identity: "duelist general; red and bronze armor, banner, ponytail", slots: ["weapon", "off_hand", "head", "shoulder", "arms", "back", "belt", "legs"] },
  { name: "Templar Assassin", attribute: "agility", identity: "psionic assassin; violet blades, refraction shields, hidden temple garb", slots: ["weapon", "head", "shoulder", "arms", "back", "belt", "legs"] },
  { name: "Ursa", attribute: "agility", identity: "fierce bear warrior; brown fur, claw weapons, tribal jewelry", slots: ["weapon", "head", "shoulder", "arms", "back", "belt", "tail"] },
  { name: "Zeus", attribute: "intelligence", identity: "lord of thunder; white beard, lightning bolts, olympian regalia", slots: ["weapon", "head", "shoulder", "arms", "back", "belt"] },
  { name: "Slark", attribute: "agility", identity: "escaped fish-man rogue; dark teal scales, twin daggers, shadowy pounce", slots: ["weapon", "head", "shoulder", "arms", "back", "belt", "tail"] },
  { name: "Ogre Magi", attribute: "strength", identity: "two-headed dim ogre; blue skin, crude club, fire magic", slots: ["weapon", "head", "shoulder", "arms", "back", "belt"] },
  { name: "Dazzle", attribute: "universal", identity: "shadow priest; pink and purple ritual garb, feathered totems", slots: ["weapon", "head", "shoulder", "arms", "back", "belt"] },
  { name: "Wraith King", attribute: "strength", identity: "skeletal monarch; bone crown, ancient blade, ghostly green fire", slots: ["weapon", "head", "shoulder", "arms", "back", "belt", "armor"] },
  { name: "Terrorblade", attribute: "agility", identity: "demon marauder; metamorphosis, twin swords, teal and black demon armor", slots: ["weapon", "head", "shoulder", "arms", "back", "belt", "tail"] },
  { name: "Luna", attribute: "agility", identity: "moon rider; crescent glaive, silver armor, feline mount Nova", slots: ["weapon", "head", "shoulder", "arms", "back", "belt", "mount"] },
  { name: "Tiny", attribute: "strength", identity: "stone giant; craggy granite body, grows with tree weapon", slots: ["weapon", "head", "shoulder", "arms", "back", "misc"] },
  { name: "Lion", attribute: "intelligence", identity: "demon witch; hellfire hand, horns, finger of death, dark ritual robes", slots: ["weapon", "head", "shoulder", "arms", "back", "belt"] },
];

export function findHero(name: string): Hero | undefined {
  const needle = name.trim().toLowerCase();
  return HEROES.find((h) => h.name.toLowerCase() === needle);
}
