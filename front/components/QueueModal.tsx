'use client';

import React from 'react';
import { QueueTokenResponse } from '../types';

interface QueueModalProps {
  queueStatus: QueueTokenResponse | null;
  onCancel: () => void;
}

export default function QueueModal({ queueStatus, onCancel }: QueueModalProps) {
  if (!queueStatus) return null;

  const isAllowed = queueStatus.allowed;
  const progressPercent = queueStatus.totalWaiting > 0
    ? Math.max(5, Math.min(100, Math.round(((queueStatus.totalWaiting - queueStatus.rank) / queueStatus.totalWaiting) * 100)))
    : 100;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
      <div className="bg-slate-900 border border-slate-700 rounded-xl max-w-md w-full p-6 shadow-2xl flex flex-col items-center text-center">
        {/* 헤더 배지 */}
        <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-mono font-semibold bg-indigo-950 text-indigo-400 border border-indigo-800 mb-4">
          <span className="w-2 h-2 rounded-full bg-indigo-500 animate-ping"></span>
          Redis Queue [Port 8080]
        </div>

        {isAllowed ? (
          <>
            <div className="w-16 h-16 rounded-full bg-emerald-950 border border-emerald-600 flex items-center justify-center text-3xl mb-3">
              🎉
            </div>
            <h3 className="text-xl font-bold text-emerald-400 mb-1">
              입장 순서가 되었습니다!
            </h3>
            <p className="text-sm text-slate-300 mb-4">
              좌석 선택 및 예매 페이지로 즉시 이동합니다.
            </p>
            <div className="w-full bg-emerald-900/30 border border-emerald-800/60 rounded p-2 text-xs text-emerald-300 font-mono">
              입장 토큰 승인 완료 (유효시간: 15분)
            </div>
          </>
        ) : (
          <>
            <div className="w-16 h-16 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center text-2xl mb-3 animate-pulse">
              ⏳
            </div>
            <h3 className="text-lg font-bold text-slate-100 mb-1">
              접속 대기 중입니다
            </h3>
            <p className="text-xs text-slate-400 mb-4">
              안전한 예매를 위해 순서대로 입장 처리를 진행하고 있습니다.
            </p>

            {/* 대기 순번 카드 */}
            <div className="w-full bg-slate-950 border border-slate-800 rounded-lg p-4 mb-4">
              <div className="flex justify-between items-center mb-1 text-xs text-slate-400">
                <span>내 앞 대기자</span>
                <span className="font-mono text-indigo-400 font-semibold">{queueStatus.rank - 1} 명</span>
              </div>
              <div className="text-3xl font-extrabold text-amber-400 font-mono my-2">
                {queueStatus.rank} <span className="text-sm font-normal text-slate-400">번째</span>
              </div>
              <div className="w-full bg-slate-800 rounded-full h-2 overflow-hidden mt-3">
                <div
                  className="bg-indigo-500 h-2 rounded-full transition-all duration-500"
                  style={{ width: `${progressPercent}%` }}
                ></div>
              </div>
              <div className="flex justify-between text-[11px] text-slate-400 font-mono mt-1.5">
                <span>총 대기: {queueStatus.totalWaiting}명</span>
                <span>진행률: {progressPercent}%</span>
              </div>
            </div>

            <p className="text-[11px] text-slate-400 mb-4">
              💡 새로고침하거나 브라우저를 닫으시면 대기 순번이 초기화됩니다.
            </p>

            <button
              onClick={onCancel}
              className="text-xs text-slate-400 hover:text-slate-200 underline underline-offset-2 transition"
            >
              대기열 나가기
            </button>
          </>
        )}
      </div>
    </div>
  );
}
