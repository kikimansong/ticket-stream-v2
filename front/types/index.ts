export interface Concert {
  id: number;
  title: string;
  venue: string;
  concertDate: string;
  totalSeats: number;
}

export type SeatStatus = 'AVAILABLE' | 'RESERVED' | 'SOLD';

export interface Seat {
  id: number;
  concertId: number;
  seatNumber: string;
  status: SeatStatus;
  price: number;
  version: number;
}

export interface QueueTokenResponse {
  token: string;
  userId: number;
  concertId: number;
  rank: number;
  totalWaiting: number;
  allowed: boolean;
  message: string;
}

export interface SeatReserveResponse {
  seatId: number;
  concertId: number;
  userId: number;
  seatNumber: string;
  status: SeatStatus;
  price: number;
  reservedAt: string;
  message: string;
}

export interface PaymentResponse {
  paymentId: number;
  orderId: number;
  seatId: number;
  amount: number;
  pgTid: string;
  status: string;
  approvedAt: string;
  message: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface TestLog {
  id: string;
  time: string;
  type: 'INFO' | 'SUCCESS' | 'ERROR' | 'QUEUE';
  message: string;
  service?: string;
  latencyMs?: number;
}
