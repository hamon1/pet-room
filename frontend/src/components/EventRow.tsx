import Link from "next/link";
import { daysText, EVENT_CLASS, EVENT_LABEL, eventLink, type PetEvent } from "@/lib/events";
import { shortDate } from "@/lib/date";

export default function EventRow({ event }: { event: PetEvent }) {
  return (
    <Link href={eventLink(event)} className="list-item" style={{ color: "inherit", textDecoration: "none" }}>
      <span className={`chip ${EVENT_CLASS[event.type]}`}>{EVENT_LABEL[event.type]}</span>
      <span className="grow">{event.title}</span>
      <span className="small muted" style={{ whiteSpace: "nowrap" }}>
        {shortDate(event.date)} · {daysText(event.daysUntil)}
      </span>
    </Link>
  );
}
