import './globals.css';
import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Ticket Stream v1 - 단일 모놀리식 티켓팅 테스트',
  description: 'v1 동기식 DB 락 (비관적/낙관적 락) 동시성 제어 테스트 베드',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ko">
      <body className="bg-slate-950 text-slate-100 min-h-screen antialiased">
        {children}
      </body>
    </html>
  );
}
