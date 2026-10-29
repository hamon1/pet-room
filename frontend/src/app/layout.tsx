import type { Metadata, Viewport } from "next";
import type { ReactNode } from "react";
import "./globals.css";

export const metadata: Metadata = {
  title: "pet-room",
  description: "반려동물 건강·의료·먹이를 한곳에서 챙기는 펫케어 앱",
  appleWebApp: { capable: true, title: "pet-room", statusBarStyle: "default" },
  icons: { apple: "/apple-touch-icon.png" },
};

export const viewport: Viewport = {
  themeColor: "#FFF8EC",
  viewportFit: "cover",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="ko">
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="" />
        {/* App Router의 루트 레이아웃은 모든 페이지에 적용되므로 이 경고(pages 라우터용)는 해당되지 않음 */}
        {/* eslint-disable-next-line @next/next/no-page-custom-font */}
        <link
          rel="stylesheet"
          href="https://fonts.googleapis.com/css2?family=Jua&family=Noto+Sans+KR:wght@400;500;700&display=swap"
        />
      </head>
      <body>
        <main className="phone">{children}</main>
      </body>
    </html>
  );
}
