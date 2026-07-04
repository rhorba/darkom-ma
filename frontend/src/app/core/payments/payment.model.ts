export type PaymentStatus = 'PENDING' | 'PAID' | 'OVERDUE' | 'FAILED';

export interface Payment {
  id: string;
  leaseId: string;
  amount: number;
  dueDate: string;
  paidAt: string | null;
  status: PaymentStatus;
  cmiTransactionId: string | null;
}

export interface PaymentInitiationResponse {
  paymentId: string;
  cmiTransactionId: string;
  redirectUrl: string;
}
