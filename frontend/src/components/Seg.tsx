"use client";

export default function Seg<T extends string>({
  options,
  value,
  onChange,
  labels,
  ariaLabel,
}: {
  options: readonly T[];
  value: T | null;
  onChange: (value: T) => void;
  labels: Record<T, string>;
  ariaLabel: string;
}) {
  return (
    <div className="seg" role="group" aria-label={ariaLabel}>
      {options.map((o) => (
        <button key={o} type="button" aria-pressed={value === o} onClick={() => onChange(o)}>
          {labels[o]}
        </button>
      ))}
    </div>
  );
}
