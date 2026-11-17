'use client';

import React from 'react';
import { Concert } from '../types';

interface ConcertCarouselProps {
  concerts: Concert[];
  selectedConcertId: number | null;
  onSelectConcert: (id: number) => void;
  isLoading?: boolean;
}

export default function ConcertCarousel({
  concerts,
  selectedConcertId,
  onSelectConcert,
  isLoading = false,
}: ConcertCarouselProps) {
  return (
    <div className="bg-slate-900 border border-slate-800 rounded-lg p-4">
      <div className="flex flex-wrap items-center justify-between gap-2 mb-3">
        <div className="flex items-center gap-2">
          <h2 className="text-base font-semibold text-slate-200">
            🎵 콘서트 목록
          </h2>
          <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-blue-950 text-blue-400 border border-blue-800">
            GET /api/v1/concerts
          </span>
        </div>
        <span className="text-xs text-slate-400">
          {isLoading ? 'DB에서 공연 정보를 불러오는 중...' : `총 ${concerts.length}개 공연 등록됨`}
        </span>
      </div>

      {isLoading ? (
        /* 스켈레톤 UI (연결 및 로딩 대기) */
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          {[1, 2, 3].map((idx) => (
            <div
              key={idx}
              className="p-3 rounded-md border border-slate-800 bg-slate-800/40 animate-pulse space-y-2.5"
            >
              <div className="flex justify-between items-center">
                <div className="h-4 w-14 bg-slate-700/70 rounded"></div>
                <div className="h-3 w-16 bg-slate-700/60 rounded"></div>
              </div>
              <div className="h-4 w-3/4 bg-slate-700/80 rounded"></div>
              <div className="h-3 w-1/2 bg-slate-700/50 rounded"></div>
            </div>
          ))}
        </div>
      ) : concerts.length === 0 ? (
        <div className="text-center py-6 text-xs text-slate-500 border border-dashed border-slate-800 rounded">
          등록된 콘서트 데이터가 없습니다. (백엔드 DB 확인 필요)
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          {concerts.map((concert) => {
            const isSelected = concert.id === selectedConcertId;
            return (
              <button
                key={concert.id}
                onClick={() => onSelectConcert(concert.id)}
                className={`text-left p-3 rounded-md border transition-all ${
                  isSelected
                    ? 'bg-blue-950/70 border-blue-500 text-white shadow-md ring-1 ring-blue-500/50'
                    : 'bg-slate-800/60 border-slate-700 text-slate-300 hover:border-slate-500 hover:bg-slate-800'
                }`}
              >
                <div className="flex items-center justify-between mb-1.5">
                  <span className="text-[11px] font-mono font-semibold px-2 py-0.5 rounded bg-slate-700/80 text-blue-300">
                    ID: {concert.id}
                  </span>
                  <span className="text-xs text-slate-400 font-mono">
                    총 {concert.totalSeats}석
                  </span>
                </div>
                <h3 className="font-semibold text-sm text-slate-100 truncate">
                  {concert.title}
                </h3>
                <div className="flex items-center justify-between text-xs text-slate-400 mt-1">
                  <span>📍 {concert.venue}</span>
                  <span className="font-mono text-[11px] text-slate-500">
                    {concert.concertDate?.replace('T', ' ').slice(0, 16)}
                  </span>
                </div>
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
