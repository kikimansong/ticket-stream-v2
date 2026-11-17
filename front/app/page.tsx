'use client';

import React, { useEffect, useState, useRef } from 'react';
import ConcertCarousel from '../components/ConcertCarousel';
import StadiumSeatMap from '../components/StadiumSeatMap';
import QueueModal from '../components/QueueModal';
import {
  Concert,
  Seat,
  QueueTokenResponse,
  SeatReserveResponse,
  PaymentResponse,
  ApiResponse,
  TestLog,
} from '../types';

// MSA 서비스별 베이스 URL
const QUEUE_URL = 'http://localhost:8081/api/v1';
const CONCERT_URL = 'http://localhost:8082/api/v1';
const SEAT_URL = 'http://localhost:8083/api/v1';
const ORDER_URL = 'http://localhost:8084/api/v1';
const PAYMENT_URL = 'http://localhost:8085/api/v1';

export default function Home() {
  const [concerts, setConcerts] = useState<Concert[]>([]);
  const [selectedConcertId, setSelectedConcertId] = useState<number | null>(1);
  const [seats, setSeats] = useState<Seat[]>([]);
  const [selectedSeat, setSelectedSeat] = useState<Seat | null>(null);

  const [isLoadingSeats, setIsLoadingSeats] = useState<boolean>(false);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  // 대기열 상태
  const [queueStatus, setQueueStatus] = useState<QueueTokenResponse | null>(null);
  const [queueToken, setQueueToken] = useState<string | null>(null);

  // 선점된 좌석 및 주문 정보 (결제 대기)
  const [reservedSeatInfo, setReservedSeatInfo] = useState<SeatReserveResponse | null>(null);

  // 실시간 로그
  const [logs, setLogs] = useState<TestLog[]>([]);
  const logsEndRef = useRef<HTMLDivElement>(null);

  const addLog = (type: 'INFO' | 'SUCCESS' | 'ERROR' | 'QUEUE', message: string, service?: string, latencyMs?: number) => {
    const newLog: TestLog = {
      id: Math.random().toString(36).substring(2, 9),
      time: new Date().toLocaleTimeString(),
      type,
      message,
      service,
      latencyMs,
    };
    setLogs((prev) => [...prev.slice(-49), newLog]);
  };

  useEffect(() => {
    logsEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [logs]);

  // 1. 공연 목록 로드 (concert-service: 8082)
  useEffect(() => {
    fetchConcerts();
  }, []);

  const fetchConcerts = async () => {
    try {
      const res = await fetch(`${CONCERT_URL}/concerts`);
      if (res.ok) {
        const json: ApiResponse<Concert[]> = await res.json();
        setConcerts(json.data);
        if (json.data.length > 0 && !selectedConcertId) {
          setSelectedConcertId(json.data[0].id);
        }
      }
    } catch {
      // 기본 콘서트 목업
      setConcerts([
        {
          id: 1,
          title: '2026 WORLD TOUR FINALE IN SEOUL',
          venue: '올림픽공원 KSPO DOME',
          concertDate: '2026-12-25T19:00:00',
          totalSeats: 500,
        },
      ]);
    }
  };

  // 2. 좌석 목록 로드 (seat-service: 8083)
  const fetchSeats = async (concertId: number) => {
    setIsLoadingSeats(true);
    try {
      const res = await fetch(`${SEAT_URL}/seats?concertId=${concertId}`);
      if (res.ok) {
        const json: ApiResponse<Seat[]> = await res.json();
        setSeats(json.data);
      }
    } catch (e: any) {
      addLog('ERROR', `좌석 조회 실패 (seat-service 8083 구동 확인 필요): ${e.message}`, 'seat-service');
    } finally {
      setIsLoadingSeats(false);
    }
  };

  useEffect(() => {
    if (selectedConcertId) {
      fetchSeats(selectedConcertId);
    }
  }, [selectedConcertId]);

  // 3. 대기열 진입 요청 (waiting-service: 8081)
  const handleEnterQueue = async () => {
    if (!selectedConcertId) return;
    const userId = 1001;

    addLog('QUEUE', `대기열 진입 요청 (POST :8081/queue/tokens)`, 'waiting-service');
    try {
      const res = await fetch(`${QUEUE_URL}/queue/tokens`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId, concertId: selectedConcertId }),
      });
      const json: ApiResponse<QueueTokenResponse> = await res.json();
      if (res.ok) {
        setQueueStatus(json.data);
        setQueueToken(json.data.token);
        addLog('QUEUE', `대기열 등록 완료 (내 순번: ${json.data.rank}번 / 총 ${json.data.totalWaiting}명)`, 'waiting-service');
      } else {
        addLog('ERROR', `대기열 진입 실패: ${json.message}`, 'waiting-service');
      }
    } catch (e: any) {
      addLog('ERROR', `waiting-service (Port 8081) 연결 실패: ${e.message}`, 'waiting-service');
    }
  };

  // 4. 대기열 순번 폴링
  useEffect(() => {
    if (!queueStatus || queueStatus.allowed || !queueToken || !selectedConcertId) return;

    const interval = setInterval(async () => {
      try {
        const res = await fetch(`${QUEUE_URL}/queue/status?concertId=${selectedConcertId}`, {
          headers: {
            'Content-Type': 'application/json',
            'X-Queue-Token': queueToken,
          },
        });
        if (res.ok) {
          const json: ApiResponse<QueueTokenResponse> = await res.json();
          setQueueStatus(json.data);
          if (json.data.allowed) {
            addLog('SUCCESS', `🎉 대기열 통과! 입장 허가 승인되었습니다.`, 'waiting-service');
            setTimeout(() => {
              setQueueStatus(null); // 모달 닫기
            }, 1200);
          }
        }
      } catch {}
    }, 1000);

    return () => clearInterval(interval);
  }, [queueStatus, queueToken, selectedConcertId]);

  // 5. 좌석 임시 선점 (seat-service: 8083 - Redisson 분산 락)
  const handleReserveSeat = async () => {
    if (!selectedSeat || !selectedConcertId) return;

    setIsSubmitting(true);
    const startTime = performance.now();
    const userId = 1001;

    addLog('INFO', `[Redisson 분산 락] 좌석 선점 시도: seatId=${selectedSeat.id} (${selectedSeat.seatNumber})`, 'seat-service');

    try {
      const res = await fetch(`${SEAT_URL}/seats/reserve`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          userId,
          concertId: selectedConcertId,
          seatId: selectedSeat.id,
        }),
      });

      const latency = Math.round(performance.now() - startTime);
      const json: ApiResponse<SeatReserveResponse> = await res.json();

      if (res.ok) {
        addLog('SUCCESS', `✅ 좌석 임시 선점 성공! (Kafka 'seat.reserved' 이벤트 자동 발행)`, 'seat-service', latency);
        setReservedSeatInfo(json.data);
        fetchSeats(selectedConcertId);
      } else {
        addLog('ERROR', `❌ 선점 실패 (${res.status}): ${json.message}`, 'seat-service', latency);
        fetchSeats(selectedConcertId);
      }
    } catch (e: any) {
      addLog('ERROR', `통신 오류: ${e.message}`, 'seat-service');
    } finally {
      setIsSubmitting(false);
    }
  };

  // 6. 결제 승인 요청 (payment-service: 8085)
  const handleProcessPayment = async () => {
    if (!reservedSeatInfo) return;

    setIsSubmitting(true);
    const startTime = performance.now();

    addLog('INFO', `결제 승인 요청: seatId=${reservedSeatInfo.seatId}, amount=₩${reservedSeatInfo.price.toLocaleString()}`, 'payment-service');

    try {
      const res = await fetch(`${PAYMENT_URL}/payments`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          orderId: 1, // 가주문 ID
          seatId: reservedSeatInfo.seatId,
          amount: reservedSeatInfo.price,
          paymentMethod: 'CARD',
        }),
      });

      const latency = Math.round(performance.now() - startTime);
      const json: ApiResponse<PaymentResponse> = await res.json();

      if (res.ok) {
        addLog('SUCCESS', `💳 결제 완료! PG 승인번호: ${json.data.pgTid} (Kafka 'payment.completed' 전파)`, 'payment-service', latency);
        setReservedSeatInfo(null);
        setSelectedSeat(null);
        if (selectedConcertId) fetchSeats(selectedConcertId);
      } else {
        addLog('ERROR', `결제 실패: ${json.message}`, 'payment-service', latency);
      }
    } catch (e: any) {
      addLog('ERROR', `payment-service 통신 오류: ${e.message}`, 'payment-service');
    } finally {
      setIsSubmitting(false);
    }
  };

  // 7. 좌석 리셋
  const handleResetSeats = async () => {
    if (!selectedConcertId) return;
    try {
      const res = await fetch(`${SEAT_URL}/seats/reset/${selectedConcertId}`, { method: 'POST' });
      if (res.ok) {
        addLog('SUCCESS', `공연 ID ${selectedConcertId} 좌석 상태가 초기화되었습니다.`, 'seat-service');
        setSelectedSeat(null);
        setReservedSeatInfo(null);
        fetchSeats(selectedConcertId);
      }
    } catch (e: any) {
      addLog('ERROR', `리셋 실패: ${e.message}`, 'seat-service');
    }
  };

  return (
    <main className="min-h-screen bg-slate-950 text-slate-100 p-4 md:p-6 font-sans">
      {/* 대기열 모달 */}
      <QueueModal queueStatus={queueStatus} onCancel={() => setQueueStatus(null)} />

      {/* 상단 헤더 */}
      <header className="flex flex-wrap items-center justify-between border-b border-slate-800 pb-4 mb-4 gap-2">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-xl md:text-2xl font-black text-white tracking-tight">
              Ticket Stream v2 🎫
            </h1>
            <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-indigo-950 text-indigo-400 border border-indigo-800">
              MSA + Kafka + Redis
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            대기열 큐(Redis ZSet) $\rightarrow$ 좌석 선점(Redisson 분산 락) $\rightarrow$ 결제 및 Saga 이벤트 스트리밍
          </p>
        </div>

        {/* 서비스 상태 뱃지 */}
        <div className="flex flex-wrap gap-1.5 text-[10px] font-mono">
          <span className="px-2 py-0.5 rounded bg-indigo-950 text-indigo-300 border border-indigo-800">queue: 8081</span>
          <span className="px-2 py-0.5 rounded bg-slate-900 text-slate-300 border border-slate-700">concert: 8082</span>
          <span className="px-2 py-0.5 rounded bg-cyan-950 text-cyan-300 border border-cyan-800">seat: 8083</span>
          <span className="px-2 py-0.5 rounded bg-amber-950 text-amber-300 border border-amber-800">order: 8084</span>
          <span className="px-2 py-0.5 rounded bg-emerald-950 text-emerald-300 border border-emerald-800">payment: 8085</span>
        </div>
      </header>

      {/* 메인 2단 레이아웃 */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
        {/* 좌측: 공연 캐러셀 & 좌석 맵 (8컬럼) */}
        <div className="lg:col-span-8 flex flex-col gap-4">
          <ConcertCarousel
            concerts={concerts}
            selectedConcertId={selectedConcertId}
            onSelectConcert={(id) => setSelectedConcertId(id)}
          />

          <StadiumSeatMap
            seats={seats}
            selectedSeatId={selectedSeat?.id ?? null}
            selectedConcertId={selectedConcertId}
            onSelectSeat={(seat) => setSelectedSeat(seat)}
            onResetSeats={handleResetSeats}
            isLoading={isLoadingSeats}
          />
        </div>

        {/* 우측: 예매 제어 콘솔 & 실시간 이벤트 로그 (4컬럼) */}
        <div className="lg:col-span-4 flex flex-col gap-4">
          {/* 1. 예매 및 결제 액션 카드 */}
          <div className="bg-slate-900 border border-slate-800 rounded-lg p-4">
            <h2 className="text-sm font-semibold text-slate-200 mb-3 flex items-center justify-between">
              <span>⚡ 티켓 예매 콘솔</span>
              {queueToken ? (
                <span className="text-[10px] font-mono text-emerald-400 bg-emerald-950 px-2 py-0.5 rounded border border-emerald-800">
                  대기열 승인됨
                </span>
              ) : (
                <button
                  onClick={handleEnterQueue}
                  className="text-[10px] font-mono text-indigo-300 bg-indigo-950 hover:bg-indigo-900 px-2 py-0.5 rounded border border-indigo-700 transition"
                >
                  대기열 표 뽑기 (테스트)
                </button>
              )}
            </h2>

            {/* 선택 좌석 정보 */}
            <div className="bg-slate-950 border border-slate-800/80 rounded p-3 text-xs mb-3 space-y-1.5 font-mono">
              <div className="flex justify-between">
                <span className="text-slate-400">선택 좌석:</span>
                <span className="text-white font-bold">{selectedSeat ? selectedSeat.seatNumber : '좌석을 클릭하세요'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">티켓 금액:</span>
                <span className="text-emerald-400 font-semibold">{selectedSeat ? `₩${selectedSeat.price.toLocaleString()}` : '-'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">선점 전략:</span>
                <span className="text-cyan-400">Redisson 분산 락 (Fast-Fail)</span>
              </div>
            </div>

            {/* 액션 버튼 */}
            {!reservedSeatInfo ? (
              <button
                disabled={!selectedSeat || isSubmitting}
                onClick={handleReserveSeat}
                className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 disabled:bg-slate-800 disabled:text-slate-500 text-white font-semibold rounded text-sm transition shadow-lg shadow-indigo-950"
              >
                {isSubmitting ? '좌석 선점 처리 중...' : '좌석 임시 선점하기 (POST /seats/reserve)'}
              </button>
            ) : (
              <div className="space-y-2">
                <div className="p-2 bg-emerald-950/60 border border-emerald-700/60 rounded text-xs text-emerald-300 text-center font-mono">
                  ⏱️ 좌석 선점 완료 (5분 내 결제 필요)
                </div>
                <button
                  disabled={isSubmitting}
                  onClick={handleProcessPayment}
                  className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white font-bold rounded text-sm transition shadow-lg shadow-emerald-950 animate-pulse"
                >
                  {isSubmitting ? '결제 승인 중...' : '신용카드 즉시 결제하기 (POST /payments)'}
                </button>
              </div>
            )}
          </div>

          {/* 2. 실시간 MSA & Kafka 이벤트 로그 */}
          <div className="bg-slate-900 border border-slate-800 rounded-lg p-4 flex-1 flex flex-col min-h-[300px]">
            <div className="flex justify-between items-center pb-2 border-b border-slate-800 mb-2">
              <h3 className="text-xs font-semibold text-slate-300 font-mono">📡 MSA & Kafka 스트리밍 로그</h3>
              <button
                onClick={() => setLogs([])}
                className="text-[10px] text-slate-400 hover:text-slate-200 transition"
              >
                지우기
              </button>
            </div>

            <div className="flex-1 overflow-auto max-h-[350px] space-y-1.5 text-[11px] font-mono pr-1">
              {logs.length === 0 ? (
                <div className="text-center text-slate-400 py-8">
                  버튼을 클릭하거나 k6 부하 테스트를 실행하면 실시간 호출 로그가 여기에 표시됩니다.
                </div>
              ) : (
                logs.map((log) => {
                  let colorClass = 'text-slate-300';
                  if (log.type === 'SUCCESS') colorClass = 'text-emerald-400';
                  else if (log.type === 'ERROR') colorClass = 'text-rose-400';
                  else if (log.type === 'QUEUE') colorClass = 'text-amber-300';

                  return (
                    <div key={log.id} className="leading-relaxed border-b border-slate-800/40 pb-1">
                      <span className="text-slate-400">[{log.time}]</span>{' '}
                      {log.service && (
                        <span className="text-[10px] px-1 py-0.2 rounded bg-slate-800 text-indigo-300 border border-slate-700 mr-1">
                          {log.service}
                        </span>
                      )}
                      <span className={colorClass}>{log.message}</span>
                      {log.latencyMs !== undefined && (
                        <span className="text-slate-400 ml-1">({log.latencyMs}ms)</span>
                      )}
                    </div>
                  );
                })
              )}
              <div ref={logsEndRef} />
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}
