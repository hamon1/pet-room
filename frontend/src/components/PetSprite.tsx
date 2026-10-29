import type { PetCategory, PetSubCategory } from "@/lib/pets";

const PATTERNS = {
  cat: [".X....X..", ".XX..XX..", ".XXXXXX..", "XXXXXXXX.", "XEXXXXEX.", "PXXXXXXP.", "XXXMMXXX.", ".XXXXXX..", "..X..X..."],
  dog: ["LLXXXXLL.", "LXXXXXXL.", "LXEXXEXL.", "LXXXXXXL.", ".PXNNXP..", ".XXMMXX..", "..XXXX...", "..XXXX...", "..X..X..."],
  bird: ["..XXXX...", ".XXXXXX..", "XXEXXXXX.", "BXXXXXXX.", ".XXXXWWX.", ".XXXWWWXX", "..XXXXXX.", "...F.F...", "..FF.FF.."],
  fish: [".........", "...XX....", ".XXXXXX.T", "XEXXXXXTT", "XXXXXXXTT", ".XXXXXX.T", "...XX....", ".........", "........."],
};

const BODY: Record<string, string> = { DOG: "#F2C48D", CAT: "#FFC9A3", FINCH: "#FFE476", PARROT: "#9BE07E", FISH: "#9ECCF7" };

export default function PetSprite({
  category,
  subCategory,
  size = 56,
}: {
  category: PetCategory;
  subCategory: PetSubCategory | null;
  size?: number;
}) {
  const kind = category === "FISH" ? "fish" : category === "BIRD" ? "bird" : subCategory === "CAT" ? "cat" : "dog";
  const body = BODY[category === "FISH" ? "FISH" : (subCategory ?? "DOG")];
  const colors: Record<string, string> = {
    X: body, L: "#C98F5E", W: "#7FC266", T: "#7FB3E0", E: "#3B2F2F", M: "#3B2F2F", N: "#3B2F2F", P: "#FF9AA2", B: "#F5A04A", F: "#F5A04A",
  };
  const rows = PATTERNS[kind];
  return (
    <svg width={size} height={size} viewBox="0 0 9 9" shapeRendering="crispEdges" aria-hidden="true">
      {rows.flatMap((row, y) =>
        [...row].map((ch, x) =>
          ch === "." ? null : <rect key={`${x}-${y}`} x={x} y={y} width={1} height={1} fill={colors[ch]} />,
        ),
      )}
    </svg>
  );
}
