'use client';

import React from 'react';
import { Seat } from '../types';

interface StadiumSeatMapProps {
  seats: Seat[];
  selectedSeatId: number | null;
  selectedConcertId: number | null;
  onSelectSeat: (seat: Seat) => void;
  onResetSeats?: () => void;
  isLoading: boolean;
  isQueuePassed?: boolean;
  onEnterQueue?: () => void;
}

export default function StadiumSeatMap({
  seats,
  selectedSeatId,
  selectedConcertId,
  onSelectSeat,
  onResetSeats,
  isLoading,
  isQueuePassed = true,
  onEnterQueue,
}: StadiumSeatMapProps) {
  const availableCount = seats.filter((s) => s.status === 'AVAILABLE').length;
  const reservedCount = seats.filter((s) => s.status === 'RESERVED').length;
  const soldCount = seats.filter((s) => s.status === 'SOLD').length;

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-lg p-4 flex flex-col h-full">
      {/* 헤더 및 API 명세, 통계 */}
      <div className="flex flex-wrap items-center justify-between gap-2 pb-3 border-b border-slate-800 mb-3">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-base font-semibold text-slate-200">
              🏟️ 공연장 좌석 배치도 (Kafka 스트리밍)
            </h2>
            <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-cyan-950 text-cyan-400 border border-cyan-800">
              Backend [Port 8080]
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">
            {!isQueuePassed
              ? '🔒 대기열을 통과해야 실시간 좌석을 확인하고 예매할 수 있습니다.'
              : isLoading
              ? '실시간 좌석 상태를 동기화하는 중...'
              : `총 ${seats.length}석 중 예매 가능 ${availableCount}석`}
          </p>
        </div>

        {/* 상태 레전드 & 카운트 & 리셋 버튼 */}
        <div className="flex flex-wrap items-center gap-3 text-xs font-mono">
          <div className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-sm bg-emerald-500"></span>
            <span className="text-slate-300">선점가능 ({isQueuePassed ? availableCount : '-'})</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-sm bg-amber-500"></span>
            <span className="text-slate-300">선점중/5분홀드 ({isQueuePassed ? reservedCount : '-'})</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-sm bg-rose-600"></span>
            <span className="text-slate-300">판매완료 ({isQueuePassed ? soldCount : '-'})</span>
          </div>

          {onResetSeats && isQueuePassed && (
            <button
              onClick={onResetSeats}
              title="동시성 테스트를 위해 모든 좌석을 AVAILABLE로 리셋합니다."
              className="px-2.5 py-1 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded text-xs border border-slate-700 transition flex items-center gap-1 ml-1"
            >
              <span>🧹 좌석 초기화</span>
            </button>
          )}
        </div>
      </div>

      {/* 무대 영역 시각화 */}
      <div className="w-full bg-slate-800/80 border border-slate-700 text-center py-1.5 rounded text-xs font-bold text-slate-300 tracking-widest mb-3">
        [ STAGE / STADIUM CENTER ]
      </div>

      {/* 미통과 상태: 대기열 진입 유도 카드 */}
      {!isQueuePassed ? (
        <div className="flex-1 min-h-[320px] border border-slate-800/80 rounded-lg p-8 bg-slate-950/70 flex flex-col items-center justify-center text-center">
          <div className="w-16 h-16 rounded-full bg-slate-900 border border-slate-700 flex items-center justify-center text-3xl mb-4 shadow-inner">
            🔒
          </div>
          <h3 className="text-base font-bold text-slate-100 mb-1">
            대기열 통과 후 좌석 선택이 가능합니다
          </h3>
          <p className="text-xs text-slate-400 mb-6 max-w-sm leading-relaxed">
            트래픽 폭주 방지 및 공정한 예매를 위해 순서대로 대기열을 통과한 사용자에게만 실시간 좌석 배치도가 공개됩니다.
          </p>
          {onEnterQueue && (
            <button
              onClick={onEnterQueue}
              className="px-6 py-2.5 bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 text-white font-bold rounded-lg text-sm transition shadow-lg shadow-indigo-950 flex items-center gap-2 transform active:scale-95 cursor-pointer"
            >
              <span>🎟️</span>
              <span>티켓 예매하기 (대기열 표 뽑기)</span>
            </button>
          )}
        </div>
      ) : isLoading || seats.length === 0 ? (
        <div className="flex-1 border border-slate-800/80 rounded p-2 bg-slate-950/50 flex flex-col justify-center">
          <div className="text-center py-2 text-xs text-slate-400 font-mono animate-pulse mb-2">
            📡 백엔드 (Port 8080) 좌석 데이터를 조회하고 있습니다...
          </div>
          <div className="grid grid-cols-10 sm:grid-cols-20 md:grid-cols-25 gap-1.5 overflow-hidden opacity-60">
            {Array.from({ length: 100 }).map((_, i) => (
              <div
                key={i}
                className="h-7 rounded border border-slate-800 bg-slate-800/50 animate-pulse"
              />
            ))}
          </div>
        </div>
      ) : (
        <div className="flex-1 overflow-auto max-h-[460px] border border-slate-800/80 rounded p-2 bg-slate-950/50">
          <div className="grid grid-cols-10 sm:grid-cols-20 md:grid-cols-25 gap-1.5 min-w-[500px]">
            {seats.map((seat) => {
              const isSelected = selectedSeatId === seat.id;
              let bgClass = 'bg-emerald-600/80 hover:bg-emerald-500 text-white border-emerald-500';
              let cursorClass = 'cursor-pointer';

              if (seat.status === 'RESERVED') {
                bgClass = 'bg-amber-600/80 text-amber-100 border-amber-500';
                cursorClass = 'cursor-not-allowed opacity-80';
              } else if (seat.status === 'SOLD') {
                bgClass = 'bg-rose-950 text-rose-400 border-rose-800';
                cursorClass = 'cursor-not-allowed opacity-60';
              }

              if (isSelected) {
                bgClass = 'bg-indigo-600 text-white ring-2 ring-indigo-400 border-white scale-105 z-10';
              }

              return (
                <button
                  key={seat.id}
                  disabled={seat.status !== 'AVAILABLE'}
                  onClick={() => onSelectSeat(seat)}
                  className={`h-7 text-[10px] font-mono font-medium rounded border transition flex flex-col items-center justify-center p-0.5 ${bgClass} ${cursorClass}`}
                  title={`${seat.seatNumber} (${seat.status}) - ₩${seat.price.toLocaleString()}`}
                >
                  <span className="truncate w-full text-center leading-none">
                    {seat.seatNumber}
                  </span>
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
