export interface AttendanceSummary {
  presentTodayCount: number;
  lateTodayCount: number;
  absentTodayCount: number;
  onLeaveTodayCount: number;
  incompleteTodayCount: number;
  totalOvertimeMinutesThisMonth: number;
  totalWorkedHoursThisMonth: number;
}
