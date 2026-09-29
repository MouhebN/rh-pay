export interface ChatGreetingFact {
  type: string;
  value: string;
}

export interface ChatGreetingResponse {
  message: string;
  facts: ChatGreetingFact[];
  draftOpen?: boolean;
}

export interface ChatProposedAction {
  type: string;
  leaveType: string | null;
  startDate: string | null;
  endDate: string | null;
  reason: string | null;
  leaveRequestId?: number | null;
  approveLeaveRequestIds?: number[] | null;
  rejectLeaveRequestIds?: number[] | null;
  proposalId?: string | null;
}

export interface ChatMessageResponse {
  reply: string;
  link: string | null;
  linkLabel: string | null;
  intent: string;
  sourceKind: string | null;
  sourceLabel: string | null;
  suggestions: string[];
  proposedAction: ChatProposedAction | null;
  draftOpen?: boolean;
}

export interface ChatLeaveActionRequest {
  type: string;
  startDate: string;
  endDate: string;
  reason: string;
  proposalId: string;
}

export interface ChatCancelLeaveActionRequest {
  leaveRequestId: number;
  proposalId: string;
}

export interface ChatLeaveReviewActionRequest {
  approveLeaveRequestIds: number[];
  rejectLeaveRequestIds: number[];
  reason: string | null;
  proposalId: string;
}

export interface ChatThreadMessage {
  role: 'user' | 'assistant';
  text: string;
  link?: string | null;
  linkLabel?: string | null;
  intent?: string | null;
  sourceKind?: string | null;
  sourceLabel?: string | null;
  proposedAction?: ChatProposedAction | null;
  actionResolved?: boolean;
  actionStale?: boolean;
}
