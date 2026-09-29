package com.rhpaie.attendance.service;

import com.rhpaie.attendance.dto.AttendanceAdjustRequest;
import com.rhpaie.attendance.dto.AttendanceFilterRequest;
import com.rhpaie.attendance.dto.AttendanceResponse;
import com.rhpaie.attendance.dto.AttendanceSummaryResponse;
import com.rhpaie.attendance.dto.TodayAttendanceResponse;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

    AttendanceResponse checkIn(String username);

    AttendanceResponse checkOut(String username);

    TodayAttendanceResponse today(String username);

    List<AttendanceResponse> myRecords(String username, AttendanceFilterRequest filter);

    List<AttendanceResponse> findAll(AttendanceFilterRequest filter);

    AttendanceResponse findById(Long id);

    List<AttendanceResponse> findByEmployee(Long employeeId);

    AttendanceResponse adjust(Long id, AttendanceAdjustRequest request, String username);

    AttendanceSummaryResponse summary();

    void generateDailyAbsences(LocalDate date);
}
