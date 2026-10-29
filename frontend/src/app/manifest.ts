import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "pet-room 우리 집",
    short_name: "pet-room",
    description: "반려동물 건강·의료·먹이를 한곳에서 챙기는 펫케어 앱",
    start_url: "/",
    display: "standalone",
    background_color: "#FFF8EC",
    theme_color: "#FFF8EC",
    lang: "ko",
    icons: [
      { src: "/icon-192.png", sizes: "192x192", type: "image/png" },
      { src: "/icon-512.png", sizes: "512x512", type: "image/png" },
    ],
  };
}
