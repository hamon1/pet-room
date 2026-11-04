import Link from "next/link";

export default function BackLink({ href = "/", label = "방" }: { href?: string; label?: string }) {
  return (
    <Link className="link" href={href}>
      ← {label}
    </Link>
  );
}
